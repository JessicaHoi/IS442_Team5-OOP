import { beforeEach, describe, expect, it } from 'vitest';
import { mockAdapter } from './adapter';
import { getDb, resetDb } from './db';
import { connectionStatusBetween } from './serializers';

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

describe('Telegram handle on a profile', () => {
	let owner, viewer, ownerToken, viewerToken, profile;
	beforeEach(async () => {
		resetDb();
		const db = getDb();
		const students = db.users.filter((user) => user.role === 'STUDENT' && user.status === 'ACTIVE');
		owner = students[0];
		viewer = students.find((user) => user.id !== owner.id && connectionStatusBetween(db, owner.id, user.id).connection === null);
		ownerToken = await login(owner);
		viewerToken = await login(viewer);
		const me = (await call('get', '/students/me', { token: ownerToken })).data;
		profile = { name: me.name, school: me.school, programme: me.programme, yearOfStudy: me.yearOfStudy, contactNumber: me.contactNumber, courses: me.courses };
	});

	it('stores the bare username, rejects invalid ones and clears it when blank', async () => {
		const save = (telegramHandle) => call('put', '/students/me/profile', { token: ownerToken, data: { ...profile, telegramHandle } });
		expect((await save('@aisha_r')).data.telegramHandle).toBe('aisha_r');
		expect((await save('not valid!')).status).toBe(400);
		expect((await save('')).data.telegramHandle).toBeNull();
	});

	it('is hidden until the students are connected', async () => {
		await call('put', '/students/me/profile', { token: ownerToken, data: { ...profile, telegramHandle: 'aisha_r' } });
		const seen = async () => (await call('get', `/students/${owner.id}`, { token: viewerToken })).data.telegramHandle;
		expect(await seen()).toBeNull();

		const request = await call('post', '/connections', { token: viewerToken, data: { toStudentId: owner.id } });
		expect(request.status).toBe(201);
		expect(await seen()).toBeNull();

		await call('post', `/connections/${request.data.id}/accept`, { token: ownerToken });
		expect(await seen()).toBe('aisha_r');
	});
});
