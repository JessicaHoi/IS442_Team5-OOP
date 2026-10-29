import { beforeAll, describe, expect, it } from 'vitest';
import { mockAdapter } from './adapter';
import { resetDb } from './db';
import { DEFAULT_MATCHING_CONFIG } from '../../utils/constants';

async function call(method, url, { data, token } = {}) {
	const config = { method, url, data: data ? JSON.stringify(data) : undefined, headers: token ? { Authorization: `Bearer ${token}` } : {} };
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

describe('mock strategy preview', () => {
	let adminToken;
	beforeAll(async () => {
		resetDb();
		adminToken = await login('admin@smu.edu.sg');
	});

	it('ranks the same student under saved and proposed settings without saving', async () => {
		const { data: students } = await call('get', '/admin/matching-config/preview/students', { token: adminToken });
		const sample = students.find((s) => s.courses.length);
		const proposed = { ...structuredClone(DEFAULT_MATCHING_CONFIG), strategy: 'AVAILABILITY_FIRST' };

		const { status, data } = await call('post', '/admin/matching-config/preview', {
			token: adminToken,
			data: { studentId: sample.id, course: sample.courses[0], proposed },
		});

		expect(status).toBe(200);
		expect(data.current.strategy).toBe('BALANCED');
		expect(data.proposed.strategy).toBe('AVAILABILITY_FIRST');
		expect(data.proposed.results[0]).toHaveProperty('otherRank');
		const { data: saved } = await call('get', '/admin/matching-config', { token: adminToken });
		expect(saved.strategy).toBe('BALANCED');
	});

	it('rejects strategies that are not built yet and non-admin callers', async () => {
		const proposed = { ...structuredClone(DEFAULT_MATCHING_CONFIG), strategy: 'COURSE_FIRST' };
		const { status } = await call('post', '/admin/matching-config/preview', { token: adminToken, data: { studentId: 2, proposed } });
		expect(status).toBe(400);
		const studentToken = await login('aisha.rahman@smu.edu.sg');
		expect((await call('get', '/admin/matching-config/preview/students', { token: studentToken })).status).toBe(403);
	});

});
