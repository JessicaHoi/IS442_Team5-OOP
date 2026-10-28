import { describe, expect, it } from 'vitest';
import { buildGroupHeatmap, coversBlock, heatmapColor, paintAvailability } from './groupAvailability';

describe('availability grid selection', () => {
	it.each([
		[[1, 600], [3, 660]], [[3, 660], [1, 600]], [[1, 660], [3, 600]], [[3, 600], [1, 660]],
	])('paints the entire rectangle in either diagonal direction: %j to %j', (a, b) => {
		const slots = paintAvailability([], { dayIndex: a[0], minute: a[1] }, { dayIndex: b[0], minute: b[1] }, true);
		expect(slots).toEqual(['TUE', 'WED', 'THU'].map((day) => ({ day, start: '10:00', end: '11:30' })));
	});
	it.each([
		[[1, 600], [1, 660]], [[1, 660], [1, 600]], [[1, 600], [3, 600]], [[3, 600], [1, 600]],
	])('supports vertical and horizontal drags: %j to %j', (a, b) => {
		const slots = paintAvailability([], { dayIndex: a[0], minute: a[1] }, { dayIndex: b[0], minute: b[1] }, true);
		expect(slots).toHaveLength(Math.abs(a[0] - b[0]) + 1);
		expect(slots[0].start).toBe('10:00');
		expect(slots[0].end).toBe(a[0] === b[0] ? '11:30' : '10:30');
	});
	it('erases a rectangle, splits ranges and preserves exact and out-of-grid timings', () => {
		const slots = [{ day: 'MON', start: '05:15', end: '06:45' }, { day: 'TUE', start: '09:15', end: '12:45' }, { day: 'SUN', start: '23:15', end: '23:45' }];
		expect(paintAvailability(slots, { dayIndex: 1, minute: 600 }, { dayIndex: 1, minute: 630 }, false)).toEqual([
			slots[0], { day: 'TUE', start: '09:15', end: '10:00' }, { day: 'TUE', start: '11:00', end: '12:45' }, slots[2],
		]);
		expect(slots[1].end).toBe('12:45');
	});
	it('merges adjacent selected cells into a single display range', () => {
		expect(paintAvailability([{ day: 'TUE', start: '19:00', end: '20:30' }], { dayIndex: 1, minute: 1230 }, { dayIndex: 1, minute: 1230 }, true))
			.toEqual([{ day: 'TUE', start: '19:00', end: '21:00' }]);
	});
	it('requires complete cell coverage while respecting adjacent exact slots', () => {
		const slots = [{ day: 'TUE', start: '14:15', end: '14:45' }, { day: 'TUE', start: '14:45', end: '15:00' }];
		expect(coversBlock(slots, 'TUE', 840)).toBe(false);
		expect(coversBlock(slots, 'TUE', 870)).toBe(true);
	});
});

describe('group heatmap', () => {
	it('uses white for nobody, percentage opacity for partial attendance, and full opacity for everyone', () => {
		expect(heatmapColor(0, 5)).toBe('#fff');
		expect(heatmapColor(1, 5)).toBe('rgba(var(--bs-primary-rgb), 0.2)');
		expect(heatmapColor(3, 5)).toBe('rgba(var(--bs-primary-rgb), 0.6)');
		expect(heatmapColor(5, 5)).toBe('rgba(var(--bs-primary-rgb), 1)');
	});
	it('updates the member names independently for each block and does not merge different attendees', () => {
		const member = (id, start, end) => ({ id, name: String(id), availability: [{ day: 'TUE', start, end }] });
		const result = buildGroupHeatmap([member(1, '14:00', '15:00'), member(2, '14:00', '16:00'), member(3, '15:00', '16:00')]);
		expect(result.heatmap).toHaveLength(224);
		expect(result.heatmap.find((cell) => cell.day === 'TUE' && cell.startTime === '14:30').availableMembers.map((item) => item.id)).toEqual([1, 2]);
		expect(result.heatmap.find((cell) => cell.day === 'TUE' && cell.startTime === '15:00').availableMembers.map((item) => item.id)).toEqual([2, 3]);
		expect(result.suggestions.map(({ slot }) => [slot.start, slot.end])).toEqual([['14:00', '15:00'], ['15:00', '16:00']]);
	});
});
