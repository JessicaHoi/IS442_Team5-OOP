import { describe, expect, it } from 'vitest';
import { formatDuration, formatSlot, overlapMinutes, sortSlots, totalMinutes, validateSlots } from './availability';

describe('overlapMinutes', () => {
	it('matches the Appendix A example: Wed 7-9pm shared gives a 2-hour overlap', () => {
		const a = [{ day: 'WED', start: '19:00', end: '21:00' }, { day: 'FRI', start: '14:00', end: '16:00' }];
		const b = [{ day: 'WED', start: '19:00', end: '21:00' }, { day: 'THU', start: '18:00', end: '20:00' }];
		expect(overlapMinutes(a, b)).toBe(120);
	});

	it('counts partial overlaps and ignores different days', () => {
		const a = [{ day: 'MON', start: '10:00', end: '12:00' }];
		expect(overlapMinutes(a, [{ day: 'MON', start: '11:00', end: '13:00' }])).toBe(60);
		expect(overlapMinutes(a, [{ day: 'TUE', start: '10:00', end: '12:00' }])).toBe(0);
	});
});

describe('validateSlots', () => {
	it('accepts valid, non-overlapping slots', () => {
		const slots = [{ day: 'MON', start: '10:00', end: '12:00' }, { day: 'MON', start: '13:00', end: '14:00' }];
		expect(validateSlots(slots)).toEqual([null, null]);
	});

	it('flags end-before-start, missing values and same-day clashes', () => {
		const [backwards] = validateSlots([{ day: 'MON', start: '12:00', end: '10:00' }]);
		expect(backwards).toMatch(/after start/);
		const [missing] = validateSlots([{ day: '', start: '10:00', end: '11:00' }]);
		expect(missing).toMatch(/Choose/);
		const clash = validateSlots([
			{ day: 'TUE', start: '10:00', end: '12:00' },
			{ day: 'TUE', start: '11:00', end: '13:00' },
		]);
		expect(clash.every((message) => /overlaps/.test(message))).toBe(true);
	});
});

describe('formatting helpers', () => {
	it('sorts by weekday then start time', () => {
		const sorted = sortSlots([
			{ day: 'FRI', start: '09:00', end: '10:00' },
			{ day: 'MON', start: '15:00', end: '16:00' },
			{ day: 'MON', start: '09:00', end: '10:00' },
		]);
		expect(sorted.map(formatSlot)).toEqual(['Mon 09:00–10:00', 'Mon 15:00–16:00', 'Fri 09:00–10:00']);
	});

	it('formats durations and totals', () => {
		expect(formatDuration(150)).toBe('2h 30m');
		expect(formatDuration(60)).toBe('1h');
		expect(formatDuration(0)).toBe('0h');
		expect(totalMinutes([{ day: 'MON', start: '10:00', end: '11:30' }])).toBe(90);
	});
});
