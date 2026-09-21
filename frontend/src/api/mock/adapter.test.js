import { beforeAll, describe, expect, it } from 'vitest';
import { mockAdapter } from './adapter';
import { resetDb } from './db';
import { COURSES } from './seed';

async function call(method, url, { data, params, token } = {}) {
	const config = { method, url, params, data: data ? JSON.stringify(data) : undefined, headers: token ? { Authorization: `Bearer ${token}` } : {} };
	try {
		return await mockAdapter(config);
	} catch (error) {
		return error.response;
	}
}

async function login(email) {
	const { data } = await call('post', '/auth/login', { data: { email, password: 'password' } });
	return data.token;
}

const AISHA = 'aisha.rahman@smu.edu.sg';

describe('mock API', () => {
	let token;
	beforeAll(async () => {
		resetDb();
		token = await login(AISHA);
	});

	it('is seeded with at least 10 courses and 50 student profiles', async () => {
		const adminToken = await login('admin@smu.edu.sg');
		const { data: users } = await call('get', '/admin/users', { token: adminToken });
		expect(COURSES.length).toBeGreaterThanOrEqual(10);
		expect(users.filter((u) => u.role === 'STUDENT').length).toBeGreaterThanOrEqual(50);
	});

	it('rejects bad credentials and protects routes by role', async () => {
		expect((await call('post', '/auth/login', { data: { email: AISHA, password: 'nope' } })).status).toBe(401);
		expect((await call('get', '/admin/users', { token })).status).toBe(403);
		expect((await call('get', '/students/me')).status).toBe(401);
	});

	it('ranks IS442 candidates by score, best first, and the seeded demo pair is on top', async () => {
		const { data } = await call('get', '/matches', { token, params: { course: 'IS442' } });
		expect(data.length).toBeGreaterThanOrEqual(8);
		const scores = data.map((m) => m.score);
		expect(scores).toEqual([...scores].sort((a, b) => b - a));
		expect(data[0].student.name).toBe('Wei Jie Tan');
	});

	it('hides contact numbers until a request is accepted', async () => {
		const { data: before } = await call('get', '/students/3', { token });
		expect(before.contactNumber).toBeNull();

		const sent = await call('post', '/connections', { token, data: { toStudentId: 3, message: 'Hi' } });
		expect(sent.status).toBe(201);
		expect((await call('get', '/students/3', { token })).data.contactNumber).toBeNull();

		const weiJieToken = await login('wei.jie.tan@smu.edu.sg');
		const { data: incoming } = await call('get', '/connections', { token: weiJieToken, params: { view: 'incoming' } });
		const request = incoming.find((c) => c.other.id === 2);
		await call('post', `/connections/${request.id}/accept`, { token: weiJieToken });

		expect((await call('get', '/students/3', { token })).data.contactNumber).toBeTruthy();
		expect((await call('delete', `/connections/${request.id}`, { token })).status).toBe(204);
		expect((await call('get', '/students/3', { token })).data.contactNumber).toBeNull();
	});

	it('lets a leader accept a join request but not exceed the group size', async () => {
		const { data: requests } = await call('get', '/groups/1/join-requests', { token });
		expect(requests.length).toBeGreaterThan(0);
		const accepted = await call('post', `/groups/1/join-requests/${requests[0].id}/accept`, { token });
		expect(accepted.status).toBe(200);
		expect(accepted.data.members.some((m) => m.id === requests[0].student.id)).toBe(true);
		// A non-leader cannot manage the group.
		const other = await login('priya.nair@smu.edu.sg');
		expect((await call('get', '/groups/1/join-requests', { token: other })).status).toBe(403);
	});

	it('changing the matching strategy is stored by the admin endpoint', async () => {
		const adminToken = await login('admin@smu.edu.sg');
		const { data: config } = await call('get', '/admin/matching-config', { token: adminToken });
		const update = await call('put', '/admin/matching-config', { token: adminToken, data: { ...config, strategy: 'COURSE_FIRST' } });
		expect(update.data.strategy).toBe('COURSE_FIRST');
		const invalid = { ...config, criteria: config.criteria.map((c) => ({ ...c, enabled: false })) };
		expect((await call('put', '/admin/matching-config', { token: adminToken, data: invalid })).status).toBe(400);
	});
});
