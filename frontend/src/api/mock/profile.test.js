import { beforeEach, describe, expect, it } from 'vitest';
import { mockAdapter } from './adapter';
import { getDb, resetDb } from './db';
import { issueSession } from './sessions';

describe('mock profile validation', () => {
	let token, profile;
	beforeEach(() => {
		resetDb();
		const db = getDb();
		const user = db.users.find((user) => user.id === 2);
		token = issueSession(db, user);
		profile = { ...structuredClone(db.students[2]), name: user.name };
	});

	it.each([
		{ yearOfStudy: 99 }, { yearOfStudy: -1 }, { yearOfStudy: 1.5 }, { yearOfStudy: 'invalid' },
		{ yearOfStudy: true }, { yearOfStudy: [1] },
		{ contactNumber: 'bad' }, { contactNumber: '1234567' }, { contactNumber: '1'.repeat(16) },
		{ name: 'x'.repeat(101) }, { programme: 'x'.repeat(101) }, { school: '  ' },
	])('rejects invalid fields without changing the stored profile: %j', async (fields) => {
		const original = structuredClone(getDb().students[2]);
		await expect(mockAdapter({ method: 'put', url: '/students/me/profile', headers: { Authorization: `Bearer ${token}` }, data: { ...profile, ...fields } }))
			.rejects.toMatchObject({ response: { status: 400 } });
		expect(getDb().students[2]).toEqual(original);
	});

	it.each([1, 5])('accepts boundary year %i and maximum text lengths', async (yearOfStudy) => {
		const response = await mockAdapter({ method: 'put', url: '/students/me/profile', headers: { Authorization: `Bearer ${token}` },
			data: { ...profile, yearOfStudy, name: 'N'.repeat(100), programme: 'P'.repeat(100), contactNumber: '+65 9123 4567' } });
		expect(response.status).toBe(200);
		expect(response.data.yearOfStudy).toBe(yearOfStudy);
	});

	it('saves profile details after clearing every study preference', async () => {
		const headers = { Authorization: `Bearer ${token}` };
		expect((await mockAdapter({ method: 'put', url: '/students/me/preferences', headers, data: { preferences: [] } })).data.preferences).toEqual([]);
		const response = await mockAdapter({ method: 'put', url: '/students/me/profile', headers,
			data: { ...profile, contactNumber: '+65 8000 0000' } });
		expect(response.data.contactNumber).toBe('+65 8000 0000');
		expect(response.data.preferences).toEqual([]);
	});
});
