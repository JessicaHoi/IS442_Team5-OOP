import { DAYS } from './constants';
import { toMinutes } from './availability';

export const GRID_START = 7 * 60;
export const GRID_END = 23 * 60;
export const BLOCK_MINUTES = 30;
export const GRID_TIMES = Array.from({ length: (GRID_END - GRID_START) / BLOCK_MINUTES }, (_, i) => GRID_START + i * BLOCK_MINUTES);
export const minuteLabel = (minute) => `${String(Math.floor(minute / 60)).padStart(2, '0')}:${String(minute % 60).padStart(2, '0')}`;

export function mergeAvailability(slots) {
	const result = [];
	for (const { value: day } of DAYS) {
		for (const slot of slots.filter((item) => item.day === day).sort((a, b) => toMinutes(a.start) - toMinutes(b.start))) {
			const last = result.at(-1);
			if (last?.day === day && toMinutes(slot.start) <= toMinutes(last.end)) {
				if (toMinutes(slot.end) > toMinutes(last.end)) last.end = slot.end;
			} else result.push({ ...slot });
		}
	}
	return result;
}

export function coversBlock(slots, day, minute) {
	return mergeAvailability(slots).some((slot) => slot.day === day && toMinutes(slot.start) <= minute && toMinutes(slot.end) >= minute + BLOCK_MINUTES);
}

export function paintAvailability(slots, anchor, target, adding) {
	const firstDay = Math.min(anchor.dayIndex, target.dayIndex);
	const lastDay = Math.max(anchor.dayIndex, target.dayIndex);
	const start = Math.min(anchor.minute, target.minute);
	const end = Math.max(anchor.minute, target.minute) + BLOCK_MINUTES;
	const selectedDays = DAYS.slice(firstDay, lastDay + 1).map((day) => day.value);
	const result = [];
	for (const slot of slots) {
		if (adding || !selectedDays.includes(slot.day) || toMinutes(slot.end) <= start || toMinutes(slot.start) >= end) result.push({ ...slot });
		else {
			if (toMinutes(slot.start) < start) result.push({ ...slot, end: minuteLabel(start) });
			if (toMinutes(slot.end) > end) result.push({ ...slot, start: minuteLabel(end) });
		}
	}
	if (adding) selectedDays.forEach((day) => result.push({ day, start: minuteLabel(start), end: minuteLabel(end) }));
	return mergeAvailability(result);
}

export function heatmapColor(freeCount, memberCount) {
	const ratio = memberCount > 0 ? Math.max(0, Math.min(1, freeCount / memberCount)) : 0;
	return ratio ? `rgba(var(--bs-primary-rgb), ${ratio})` : '#fff';
}

export function buildGroupHeatmap(members) {
	const heatmap = [];
	for (const day of DAYS) {
		for (const minute of GRID_TIMES) {
			const availableMembers = members.filter((member) => coversBlock(member.availability, day.value, minute))
				.map(({ id, name }) => ({ id, name })).sort((a, b) => a.name.localeCompare(b.name) || String(a.id).localeCompare(String(b.id)));
			heatmap.push({ day: day.value, startTime: minuteLabel(minute), endTime: minuteLabel(minute + BLOCK_MINUTES), freeCount: availableMembers.length, availableMembers });
		}
	}
	const windows = [];
	for (let i = 0; i < heatmap.length;) {
		const first = heatmap[i];
		let j = i + 1;
		while (j < heatmap.length && heatmap[j].day === first.day && JSON.stringify(heatmap[j].availableMembers) === JSON.stringify(first.availableMembers)) j++;
		if (first.freeCount >= 2) windows.push({ slot: { day: first.day, start: first.startTime, end: heatmap[j - 1].endTime }, freeCount: first.freeCount });
		i = j;
	}
	windows.sort((a, b) => b.freeCount - a.freeCount || (toMinutes(b.slot.end) - toMinutes(b.slot.start)) - (toMinutes(a.slot.end) - toMinutes(a.slot.start))
		|| DAYS.findIndex((day) => day.value === a.slot.day) - DAYS.findIndex((day) => day.value === b.slot.day) || toMinutes(a.slot.start) - toMinutes(b.slot.start));
	return { heatmap, suggestions: windows.slice(0, 3), memberCount: members.length };
}
