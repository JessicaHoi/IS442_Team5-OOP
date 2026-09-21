import { describe, expect, it } from 'vitest';
import { combineScore, rankCandidates, scoreCriteria } from './matching';
import { DEFAULT_MATCHING_CONFIG } from '../../utils/constants';

const studentA = {
	courses: ['IS442'],
	preferences: {
		course: 'IS442',
		meetingMode: 'IN_PERSON',
		groupFormat: 'SMALL_GROUP',
		goals: ['EXAM_PREPARATION'],
		availability: [{ day: 'WED', start: '19:00', end: '21:00' }, { day: 'FRI', start: '14:00', end: '16:00' }],
	},
};

const perfect = {
	courses: ['IS442'],
	preferences: {
		course: 'IS442',
		meetingMode: 'EITHER',
		groupFormat: 'EITHER',
		goals: ['EXAM_PREPARATION'],
		availability: [{ day: 'WED', start: '19:00', end: '21:00' }, { day: 'FRI', start: '14:00', end: '16:00' }],
	},
};

const sameCourseNoOverlap = {
	courses: ['IS442'],
	preferences: {
		course: 'IS442',
		meetingMode: 'IN_PERSON',
		groupFormat: 'SMALL_GROUP',
		goals: ['EXAM_PREPARATION'],
		availability: [{ day: 'SAT', start: '10:00', end: '12:00' }],
	},
};

const otherCourseGreatTimes = {
	courses: ['ECON101', 'IS442'],
	preferences: {
		course: 'ECON101',
		meetingMode: 'IN_PERSON',
		groupFormat: 'SMALL_GROUP',
		goals: ['EXAM_PREPARATION'],
		availability: [{ day: 'WED', start: '19:00', end: '21:00' }, { day: 'FRI', start: '14:00', end: '16:00' }],
	},
};

const config = (strategy) => ({ ...structuredClone(DEFAULT_MATCHING_CONFIG), strategy });
const candidate = (name, profile) => ({ id: name, name, profile });

describe('scoreCriteria', () => {
	it('rates flexible modes as compatible and conflicting modes as zero', () => {
		expect(scoreCriteria(studentA, perfect).studyMode).toBe(0.75);
		const online = { ...perfect, preferences: { ...perfect.preferences, meetingMode: 'ONLINE' } };
		expect(scoreCriteria(studentA, online).studyMode).toBe(0);
	});

	it('gives full availability credit for 3+ shared hours', () => {
		expect(scoreCriteria(studentA, perfect).availability).toBe(1);
		expect(scoreCriteria(studentA, sameCourseNoOverlap).availability).toBe(0);
	});
});

describe('combineScore', () => {
	it('scores 100 for a perfect match on every enabled criterion', () => {
		const criteria = { course: 1, availability: 1, studyMode: 1, studyGoal: 1, groupSize: 1 };
		expect(combineScore(DEFAULT_MATCHING_CONFIG, criteria)).toBe(100);
	});

	it('ignores disabled criteria', () => {
		const cfg = structuredClone(DEFAULT_MATCHING_CONFIG);
		cfg.criteria.forEach((item) => (item.enabled = item.key === 'course'));
		expect(combineScore(cfg, { course: 1, availability: 0, studyMode: 0, studyGoal: 0, groupSize: 0 })).toBe(100);
	});

	it('returns 0 when nothing is enabled', () => {
		const cfg = structuredClone(DEFAULT_MATCHING_CONFIG);
		cfg.criteria.forEach((item) => (item.enabled = false));
		expect(combineScore(cfg, { course: 1, availability: 1, studyMode: 1, studyGoal: 1, groupSize: 1 })).toBe(0);
	});
});

describe('rankCandidates', () => {
	const candidates = [
		candidate('Same course, no overlap', sameCourseNoOverlap),
		candidate('Other course, great times', otherCourseGreatTimes),
		candidate('Perfect', perfect),
	];

	it('puts the best overall match first with the balanced strategy', () => {
		const ranked = rankCandidates(studentA, candidates, config('BALANCED'), 'IS442');
		expect(ranked[0].name).toBe('Perfect');
		expect(ranked.map((r) => r.score)).toEqual([...ranked.map((r) => r.score)].sort((a, b) => b - a));
	});

	it('changes the order when the strategy prioritises availability or course', () => {
		const byAvailability = rankCandidates(studentA, candidates, config('AVAILABILITY_FIRST'), 'IS442');
		const byCourse = rankCandidates(studentA, candidates, config('COURSE_FIRST'), 'IS442');
		// Both IS442 takers with the target course rank above the timetable-only match by course...
		expect(byCourse[byCourse.length - 1].name).toBe('Other course, great times');
		// ...but a strong timetable overlap outranks a same-course, no-overlap student by availability.
		const idx = (list, name) => list.findIndex((r) => r.name === name);
		expect(idx(byAvailability, 'Other course, great times')).toBeLessThan(idx(byAvailability, 'Same course, no overlap'));
	});
});
