import { beforeEach, describe, expect, it } from 'vitest';
import { mockAdapter } from './adapter';
import { getDb, resetDb } from './db';

async function call(method, url, { token, data } = {}) {
	try {
		return await mockAdapter({ method, url, data: data ? JSON.stringify(data) : undefined, headers: token ? { Authorization: `Bearer ${token}` } : {} });
	} catch (error) {
		return error.response;
	}
}

async function login(email, password = 'password') {
	return (await call('post', '/auth/login', { data: { email, password } })).data.token;
}

function fields(user, changes = {}) {
	return { name: user.name, email: user.email, role: user.role, status: user.status, ...changes };
}

describe('administrator account lifecycle', () => {
	let token;
	beforeEach(async () => {
		resetDb();
		token = await login('admin@smu.edu.sg');
	});

	it('creates a normalized account and preserves an omitted password during editing', async () => {
		const created = await call('post', '/admin/users', { token, data: { name: ' New Student ', email: ' NEW@SMU.EDU.SG ', role: 'STUDENT', status: 'ACTIVE', password: 'initial-password' } });
		expect(created.status).toBe(201);
		expect(created.data.email).toBe('new@smu.edu.sg');
		expect(created.data).not.toHaveProperty('password');
		const studentToken = await login('new@smu.edu.sg', 'initial-password');
		const updated = await call('put', `/admin/users/${created.data.id}`, { token, data: fields(created.data, { name: 'Renamed Student' }) });
		expect(updated.status).toBe(200);
		expect((await call('get', '/auth/me', { token: studentToken })).data.name).toBe('Renamed Student');
		expect((await call('get', '/students/me', { token: studentToken })).data.preferences).toEqual([]);
	});

	it('rejects role conversion, self-suspension, duplicates and overlong Unicode passwords', async () => {
		const db = getDb();
		const admin = db.users.find((user) => user.role === 'ADMIN');
		const student = db.users.find((user) => user.role === 'STUDENT');
		expect((await call('put', `/admin/users/${student.id}`, { token, data: fields(student, { role: 'ADMIN' }) })).status).toBe(400);
		expect((await call('put', `/admin/users/${admin.id}`, { token, data: fields(admin, { status: 'SUSPENDED' }) })).status).toBe(400);
		expect((await call('post', '/admin/users', { token, data: fields(student, { email: admin.email.toUpperCase(), password: 'password' }) })).status).toBe(409);
		expect((await call('put', `/admin/users/${student.id}`, { token, data: fields(student, { password: '界'.repeat(25) }) })).status).toBe(400);
	});

	it('keeps old sessions revoked after suspension and reactivation', async () => {
		const student = getDb().users.find((user) => user.role === 'STUDENT');
		const old = await login(student.email);
		expect((await call('put', `/admin/users/${student.id}`, { token, data: fields(student, { status: 'SUSPENDED' }) })).status).toBe(200);
		expect((await call('put', `/admin/users/${student.id}`, { token, data: fields(student, { status: 'ACTIVE' }) })).status).toBe(200);
		expect((await call('get', '/auth/me', { token: old })).status).toBe(401);
		expect((await call('get', '/auth/me', { token: await login(student.email) })).status).toBe(200);
	});

	it('revokes every old session after a password reset while allowing a fresh login', async () => {
		const student = getDb().users.find((user) => user.role === 'STUDENT' && user.status === 'ACTIVE');
		const first = await login(student.email);
		const second = await login(student.email);
		expect(first).not.toBe(second);
		expect((await call('put', `/admin/users/${student.id}`, { token, data: fields(student, { password: 'replacement-password' }) })).status).toBe(200);
		expect((await call('get', '/auth/me', { token: first })).status).toBe(401);
		expect((await call('get', '/auth/me', { token: second })).status).toBe(401);
		expect((await call('get', '/auth/me', { token: await login(student.email, 'replacement-password') })).status).toBe(200);
		expect((await call('get', '/auth/me', { token })).status).toBe(200);
	});

	it('rejects expired sessions at the expiry time without affecting other sessions', async () => {
		const student = getDb().users.find((user) => user.role === 'STUDENT' && user.status === 'ACTIVE');
		const expired = await login(student.email);
		getDb().sessions[expired].expiresAt = Date.now();
		expect((await call('get', '/auth/me', { token: expired })).status).toBe(401);
		expect((await call('get', '/auth/me', { token })).status).toBe(200);
	});

	it('does not update the last-login time or create a session for invalid credentials', async () => {
		const student = getDb().users.find((user) => user.role === 'STUDENT' && user.status === 'ACTIVE');
		const before = student.lastLogin;
		const sessions = Object.keys(getDb().sessions);
		expect((await call('post', '/auth/login', { data: { email: student.email, password: 'wrong-password' } })).status).toBe(401);
		expect(student.lastLogin).toBe(before);
		expect(Object.keys(getDb().sessions)).toEqual(sessions);
	});

	it('deleting an ordinary student removes their sessions and preserves unrelated groups', async () => {
		const db = getDb();
		const student = db.users.find((user) => user.role === 'STUDENT' && user.status === 'ACTIVE' && !db.groups.some((group) => group.leaderId === user.id));
		const session = await login(student.email);
		const groupIds = db.groups.map((group) => group.id);
		expect((await call('delete', `/admin/users/${student.id}`, { token })).status).toBe(204);
		expect((await call('get', '/auth/me', { token: session })).status).toBe(401);
		expect(Object.values(db.sessions).some((entry) => entry.userId === student.id)).toBe(false);
		expect(db.groups.map((group) => group.id)).toEqual(groupIds);
	});

	it('requires an active existing member to take over every led group before deletion', async () => {
		const db = getDb();
		const group = db.groups.find((candidate) => candidate.memberIds.length > 1);
		const old = db.users.find((user) => user.id === group.leaderId);
		const ids = db.groups.map((candidate) => candidate.id);
		expect((await call('delete', `/admin/users/${old.id}`, { token })).status).toBe(409);
		const led = await call('get', `/admin/users/${old.id}/led-groups`, { token });
		for (const owned of led.data) {
			expect(owned.eligibleMembers.length).toBeGreaterThan(0);
			const successor = owned.eligibleMembers[0].id;
			expect((await call('put', `/admin/users/${old.id}/led-groups/${owned.id}/leader`, { token, data: { studentId: successor } })).status).toBe(200);
		}
		expect((await call('delete', `/admin/users/${old.id}`, { token })).status).toBe(204);
		expect(db.groups.map((candidate) => candidate.id)).toEqual(ids);
		expect(db.groups.every((candidate) => !candidate.memberIds.includes(old.id))).toBe(true);
	});

	it('rejects a replacement outside the group and deletion when no replacement is eligible', async () => {
		const db = getDb();
		const group = db.groups[0];
		group.memberIds = [group.leaderId];
		const outsider = db.users.find((user) => user.role === 'STUDENT' && user.id !== group.leaderId);
		expect((await call('put', `/admin/users/${group.leaderId}/led-groups/${group.id}/leader`, { token, data: { studentId: outsider.id } })).status).toBe(400);
		expect((await call('delete', `/admin/users/${group.leaderId}`, { token })).status).toBe(409);
	});
});
