import { describe, expect, it } from 'vitest';
import { emptyPreference, preferenceFor } from './preferences';

const IS442 = { ...emptyPreference('IS442'), goals: ['EXAM_PREPARATION'] };
const IS212 = { ...emptyPreference('IS212'), goals: ['CONCEPT_REVIEW'] };

describe('preferenceFor', () => {
	it('returns the preference for the requested course', () => {
		expect(preferenceFor([IS442, IS212], 'IS212')).toBe(IS212);
	});

	it('falls back to the first preference', () => {
		expect(preferenceFor([IS442, IS212], 'STAT101')).toBe(IS442);
		expect(preferenceFor([IS442, IS212])).toBe(IS442);
	});

	it('returns null when there are no preferences', () => {
		expect(preferenceFor([], 'IS442')).toBeNull();
		expect(preferenceFor(undefined)).toBeNull();
	});
});
