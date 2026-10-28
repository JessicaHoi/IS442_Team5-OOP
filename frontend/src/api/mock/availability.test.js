import { beforeEach, describe, expect, it } from 'vitest';
import { mockAdapter } from './adapter';
import { getDb, resetDb } from './db';

async function call(method, url, { data, token, params } = {}) {
	try {
		return await mockAdapter({ method, url, params, data: data ? JSON.stringify(data) : undefined, headers: token ? { Authorization: `Bearer ${token}` } : {} });
	} catch (error) { return error.response; }
}
async function login(user) { return (await call('post', '/auth/login', { data: { email: user.email, password: 'password' } })).data.token; }

describe('group availability API', () => {
	let db, group, member, token, path;
	beforeEach(async () => {
		resetDb();
		db = getDb();
		group = db.groups.find((item) => item.memberIds.length > 1);
		member = db.users.find((user) => group.memberIds.includes(user.id) && user.id !== group.leaderId && user.status === 'ACTIVE');
		token = await login(member);
		path = `/groups/${group.id}/availability/me`;
	});

	it('persists an empty override separately from the profile and reset restores the profile', async () => {
		const original = structuredClone(db.students[member.id]);
		const fallback = (await call('get', path, { token })).data;
		expect(fallback.source).toBe('PROFILE');
		const saved = await call('put', path, { token, data: { revision: null, availability: [] } });
		expect(saved.status).toBe(200);
		expect(saved.data.source).toBe('GROUP');
		expect((await call('get', path, { token })).data.availability).toEqual([]);
		expect(db.students[member.id]).toEqual(original);
		const reset = await call('delete', path, { token, params: { revision: saved.data.revision } });
		expect(reset.status).toBe(200);
		expect(reset.data).toEqual(fallback);
	});

	it('rejects stale saves and resets after another session changes availability', async () => {
		const first = (await call('put', path, { token, data: { availability: [] } })).data;
		const second = (await call('put', path, { token, data: { revision: first.revision, availability: [] } })).data;
		expect(second.revision).not.toBe(first.revision);
		expect((await call('put', path, { token, data: { revision: first.revision, availability: [] } })).status).toBe(409);
		expect((await call('delete', path, { token, params: { revision: first.revision } })).status).toBe(409);
	});

	it('uses overrides for counts and member names without changing another group', async () => {
		const before = structuredClone(db.groups.filter((item) => item.id !== group.id));
		const availability = [{ day: 'TUE', start: '17:00', end: '18:00' }];
		expect((await call('put', path, { token, data: { availability } })).status).toBe(200);
		const result = (await call('get', `/groups/${group.id}/best-times`, { token })).data;
		expect(result.heatmap).toHaveLength(224);
		expect(result.memberCount).toBe(group.memberIds.length);
		expect(result.heatmap.find((cell) => cell.day === 'TUE' && cell.startTime === '17:30').availableMembers).toContainEqual({ id: member.id, name: member.name });
		expect(db.groups.filter((item) => item.id !== group.id)).toEqual(before);
	});

	it('allows members to view closed groups but rejects edits and outsider access', async () => {
		const outsider = db.users.find((user) => user.role === 'STUDENT' && user.status === 'ACTIVE' && !group.memberIds.includes(user.id));
		const outsiderToken = await login(outsider);
		expect((await call('get', path, { token: outsiderToken })).status).toBe(403);
		expect((await call('put', path, { token: outsiderToken, data: { availability: [] } })).status).toBe(403);
		group.status = 'CLOSED';
		expect((await call('get', `/groups/${group.id}/best-times`, { token })).status).toBe(200);
		expect((await call('put', path, { token, data: { availability: [] } })).status).toBe(409);
	});

	it('cleans up an override when a member is removed', async () => {
		await call('put', path, { token, data: { availability: [] } });
		const leaderToken = await login(db.users.find((user) => user.id === group.leaderId));
		expect((await call('delete', `/groups/${group.id}/members/${member.id}`, { token: leaderToken })).status).toBe(204);
		expect(group.availabilityOverrides[member.id]).toBeUndefined();
		expect((await call('get', path, { token })).status).toBe(403);
	});
});
