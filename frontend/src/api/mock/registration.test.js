import { beforeEach, describe, expect, it } from 'vitest';
import { mockAdapter } from './adapter';
import { getDb, resetDb } from './db';

async function call(method, url, { token, data } = {}) {
	try {
		return await mockAdapter({
			method,
			url,
			data: data ? JSON.stringify(data) : undefined,
			headers: token ? { Authorization: `Bearer ${token}` } : {},
		});
	} catch (error) {
		return error.response;
	}
}

function registration(overrides = {}) {
	return {
		name: 'New Student',
		email: 'new.student@example.com',
		password: 'secret-password',
		school: 'School of Computing and Information Systems',
		programme: 'Information Systems',
		yearOfStudy: 2,
		contactNumber: '+65 9123 4567',
		telegramHandle: '',
		courses: ['IS442'],
		...overrides,
	};
}

describe('student registration', () => {
	beforeEach(() => resetDb());

	it('creates a student profile and signs the student in', async () => {
		const response = await call('post', '/auth/register', { data: registration() });
		expect(response.status).toBe(201);
		expect(response.data.user).toMatchObject({
			name: 'New Student',
			email: 'new.student@example.com',
			role: 'STUDENT',
		});

		const profile = await call('get', '/students/me', { token: response.data.token });
		expect(profile.status).toBe(200);
		expect(profile.data).toMatchObject({
			name: 'New Student',
			email: 'new.student@example.com',
			school: 'School of Computing and Information Systems',
			programme: 'Information Systems',
			yearOfStudy: 2,
			contactNumber: '+65 9123 4567',
			courses: ['IS442'],
			preferences: [],
		});
		expect(getDb().students[response.data.user.id]).toBeDefined();
	});

	it('rejects duplicate email addresses and invalid signup data', async () => {
		expect((await call('post', '/auth/register', { data: registration() })).status).toBe(201);
		expect((await call('post', '/auth/register', {
			data: registration({ email: ' NEW.STUDENT@EXAMPLE.COM ' }),
		})).status).toBe(409);
		expect((await call('post', '/auth/register', {
			data: registration({ email: 'invalid.phone@example.com', contactNumber: 'not a phone' }),
		})).status).toBe(400);
		expect((await call('post', '/auth/register', {
			data: registration({ email: 'invalid.course@example.com', courses: ['NOT-A-COURSE'] }),
		})).status).toBe(400);
	});

	it('makes the signup course catalogue public without changing the existing endpoint', async () => {
		const response = await call('get', '/public/courses');
		expect(response.status).toBe(200);
		expect(response.data.length).toBeGreaterThan(0);
		expect((await call('get', '/courses')).status).toBe(401);
	});

	it('adds a new selected course to the shared catalogue during signup', async () => {
		const response = await call('post', '/auth/register', {
			data: registration({
				email: 'new.course.student@example.com',
				courses: ['NEW101'],
				newCourses: [{ courseCode: 'new101', courseName: 'New Course' }],
			}),
		});
		expect(response.status).toBe(201);
		expect(getDb().courses).toContainEqual({ code: 'NEW101', name: 'New Course', school: null });
		const profile = await call('get', '/students/me', { token: response.data.token });
		expect(profile.data.courses).toEqual(['NEW101']);
	});

	it('lets an authenticated student create a shared course and rejects duplicate codes', async () => {
		const student = getDb().users.find((user) => user.role === 'STUDENT');
		const login = await call('post', '/auth/login', { data: { email: student.email, password: student.password } });
		const first = await call('post', '/courses', {
			token: login.data.token,
			data: { courseCode: 'NEW102', courseName: 'Another New Course' },
		});
		expect(first.status).toBe(201);
		expect(first.data.code).toBe('NEW102');
		expect((await call('post', '/courses', {
			token: login.data.token,
			data: { courseCode: 'new102', courseName: 'Duplicate' },
		})).status).toBe(409);
		expect((await call('post', '/courses', { data: { courseCode: 'NEW103', courseName: 'Anonymous' } })).status).toBe(401);
	});
});
