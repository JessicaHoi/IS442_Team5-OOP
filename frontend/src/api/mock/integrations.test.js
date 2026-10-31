import { beforeEach, describe, expect, it } from 'vitest';
import { mockAdapter } from './adapter';
import { getDb, resetDb } from './db';

async function call(method, url, token) {
	try {
		return await mockAdapter({ method, url, headers: token ? { Authorization: `Bearer ${token}` } : {} });
	} catch (error) {
		return error.response;
	}
}

async function login(user) {
	const response = await mockAdapter({ method: 'post', url: '/auth/login', data: JSON.stringify({ email: user.email, password: 'password' }), headers: {} });
	return response.data.token;
}

describe('Telegram with the mock (behaves like a server with it off)', () => {
	let studentToken, adminToken;
	beforeEach(async () => {
		resetDb();
		const { users } = getDb();
		studentToken = await login(users.find((user) => user.role === 'STUDENT' && user.status === 'ACTIVE'));
		adminToken = await login(users.find((user) => user.role === 'ADMIN'));
	});

	it('reports Telegram as off to any signed-in user', async () => {
		for (const token of [studentToken, adminToken]) {
			const response = await call('get', '/integrations', token);
			expect(response.status).toBe(200);
			expect(response.data).toEqual({ telegram: { enabled: false, botUsername: null } });
		}
	});

	it('needs a sign-in to read integrations', async () => {
		expect((await call('get', '/integrations')).status).toBe(401);
	});

	it('shows a student as not linked', async () => {
		const response = await call('get', '/students/me/telegram', studentToken);
		expect(response.data).toEqual({ linked: false, username: null, firstName: null, canMessage: false, linkedAt: null });
	});

	it('refuses to create a link and accepts a disconnect', async () => {
		expect((await call('post', '/students/me/telegram/link', studentToken)).status).toBe(409);
		expect((await call('delete', '/students/me/telegram', studentToken)).status).toBe(204);
	});

	it('keeps the student endpoints away from admins', async () => {
		expect((await call('get', '/students/me/telegram', adminToken)).status).toBe(403);
	});
});
