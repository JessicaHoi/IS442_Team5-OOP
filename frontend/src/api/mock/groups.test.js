import { beforeEach, describe, expect, it } from 'vitest';
import { mockAdapter } from './adapter';
import { getDb, resetDb } from './db';

async function call(method, url, { data, token } = {}) {
	try {
		return await mockAdapter({ method, url, data: data ? JSON.stringify(data) : undefined, headers: token ? { Authorization: `Bearer ${token}` } : {} });
	} catch (error) {
		return error.response;
	}
}

async function login(user) {
	return (await call('post', '/auth/login', { data: { email: user.email, password: 'password' } })).data.token;
}

describe('group leadership and member access', () => {
	let db, group, leader, member, leaderToken, memberToken;
	beforeEach(async () => {
		resetDb();
		db = getDb();
		group = db.groups.find((candidate) => candidate.memberIds.length > 1);
		leader = db.users.find((user) => user.id === group.leaderId);
		member = db.users.find((user) => group.memberIds.includes(user.id) && user.id !== leader.id && user.status === 'ACTIVE');
		leaderToken = await login(leader);
		memberToken = await login(member);
	});

	it('transfers to an active member, preserves memberships and removes the former leader’s permissions', async () => {
		const members = [...group.memberIds];
		expect((await call('get', `/groups/${group.id}/leadership`, { token: leaderToken })).data.eligibleMembers.some((user) => user.id === member.id)).toBe(true);
		expect((await call('put', `/groups/${group.id}/leader`, { token: leaderToken, data: { studentId: member.id } })).status).toBe(200);
		expect(group.memberIds).toEqual(members);
		expect((await call('get', `/groups/${group.id}`, { token: leaderToken })).data.myRole).toBe('MEMBER');
		expect((await call('get', `/groups/${group.id}`, { token: memberToken })).data.myRole).toBe('LEADER');
		expect((await call('get', `/groups/${group.id}/join-requests`, { token: leaderToken })).status).toBe(200);
		expect((await call('put', `/groups/${group.id}/leader`, { token: leaderToken, data: { studentId: leader.id } })).status).toBe(403);
		expect((await call('post', `/groups/${group.id}/close`, { token: leaderToken })).status).toBe(403);
	});

	it('allows members to read requests while denying all management actions and outsider access', async () => {
		const outsider = db.users.find((user) => user.role === 'STUDENT' && user.status === 'ACTIVE' && !group.memberIds.includes(user.id));
		const outsiderToken = await login(outsider);
		expect((await call('get', `/groups/${group.id}/join-requests`, { token: memberToken })).status).toBe(200);
		expect((await call('get', `/groups/${group.id}/join-requests`, { token: outsiderToken })).status).toBe(403);
		expect((await call('get', `/groups/${group.id}/leadership`, { token: memberToken })).status).toBe(403);
		for (const [method, path] of [
			['put', ''], ['post', '/close'], ['put', '/leader'],
			['delete', `/members/${leader.id}`], ['post', '/join-requests/999/accept'], ['post', '/join-requests/999/reject'],
		]) {
			expect((await call(method, `/groups/${group.id}${path}`, { token: memberToken, data: { studentId: leader.id } })).status).toBe(403);
		}
	});

	it('rejects self, nonmembers, suspended members and empty successor selections', async () => {
		const outsider = db.users.find((user) => user.role === 'STUDENT' && !group.memberIds.includes(user.id));
		for (const studentId of [leader.id, outsider.id, undefined]) {
			expect((await call('put', `/groups/${group.id}/leader`, { token: leaderToken, data: { studentId } })).status).toBe(400);
		}
		member.status = 'SUSPENDED';
		expect((await call('put', `/groups/${group.id}/leader`, { token: leaderToken, data: { studentId: member.id } })).status).toBe(400);
		expect(group.leaderId).toBe(leader.id);
	});

	it('reports zero led groups after the final transfer even while the former leader remains a member', async () => {
		const adminToken = await login(db.users.find((user) => user.role === 'ADMIN'));
		for (const owned of db.groups.filter((candidate) => candidate.leaderId === leader.id)) {
			const successor = owned.memberIds.find((id) => id !== leader.id && db.users.find((user) => user.id === id)?.status === 'ACTIVE');
			expect(successor).toBeDefined();
			expect((await call('put', `/groups/${owned.id}/leader`, { token: leaderToken, data: { studentId: successor } })).status).toBe(200);
		}
		const account = (await call('get', '/admin/users', { token: adminToken })).data.find((user) => user.id === leader.id);
		expect(account.ledGroupCount).toBe(0);
		expect(account.groupCount).toBeGreaterThan(0);
	});

	it('allows closed-group transfers without reopening or changing membership', async () => {
		group.status = 'CLOSED';
		const count = group.memberIds.length;
		expect((await call('put', `/groups/${group.id}/leader`, { token: leaderToken, data: { studentId: member.id } })).status).toBe(200);
		expect(group.status).toBe('CLOSED');
		expect(group.memberIds).toHaveLength(count);
	});

	it('lets a member leave, but not the leader', async () => {
		expect((await call('post', `/groups/${group.id}/leave`, { token: leaderToken })).status).toBe(409);
		expect((await call('post', `/groups/${group.id}/leave`, { token: memberToken })).status).toBe(204);
		expect(group.memberIds).not.toContain(member.id);
		expect((await call('post', `/groups/${group.id}/leave`, { token: memberToken })).status).toBe(409);
	});
});
