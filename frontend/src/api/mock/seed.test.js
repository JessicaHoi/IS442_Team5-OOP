import { describe, expect, it } from 'vitest';
import { createSeedData } from './seed';
import { getDb, resetDb } from './db';
import { readFileSync } from 'node:fs';

describe('demo seed isolation', () => {
	it('keeps shared catalogue and student profile fields aligned with the canonical backend fixture', () => {
		const fixture = JSON.parse(readFileSync(new URL('../../../../backend/src/main/resources/seed/demo-data.json', import.meta.url), 'utf8'));
		const db = createSeedData();
		expect(fixture.courses.length).toBeGreaterThanOrEqual(10);
		expect(fixture.students.length).toBeGreaterThanOrEqual(50);
		expect(db.courses).toEqual(fixture.courses);
		for (const student of fixture.students) {
			const user = db.users.find((user) => user.email === student.email);
			const profile = Object.fromEntries(Object.entries(student).filter(([key]) => !['key', 'email', 'name'].includes(key)));
			expect(user.name).toBe(student.name);
			expect(db.students[user.id]).toEqual(profile);
		}
	});
	it('restores the original catalogue and records on reset', () => {
		resetDb();
		const original = structuredClone(getDb().courses);
		getDb().courses[0].name = 'Changed';
		getDb().courses.push({ code: 'NEW101', name: 'New course' });
		getDb().students[2].preferences[0].goals.push('CHANGED');
		getDb().matchingConfig.criteria[0].weight = 1;
		resetDb();
		expect(getDb().courses).toEqual(original);
		expect(getDb().courses).toHaveLength(10);
		expect(Object.keys(getDb().students)).toHaveLength(50);
		expect(getDb().students[2].preferences[0].goals).not.toContain('CHANGED');
		expect(getDb().matchingConfig.criteria[0].weight).toBe(30);
	});

	it('does not share mutable goals between preferences or seed instances', () => {
		const first = createSeedData();
		const student = Object.values(first.students).find((profile) => profile.preferences.length > 1);
		const original = [...student.preferences[1].goals];
		student.preferences[0].goals.push('CHANGED');
		expect(student.preferences[1].goals).toEqual(original);
		expect(createSeedData().courses).not.toBe(first.courses);
	});
});
