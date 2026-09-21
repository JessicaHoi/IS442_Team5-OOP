import { DAYS } from './constants';

const DAY_ORDER = DAYS.map((day) => day.value);

/** Convert "HH:mm" to minutes since midnight. */
export function toMinutes(time) {
	const [hours, minutes] = time.split(':').map(Number);
	return hours * 60 + minutes;
}

/** Length of one slot in minutes. */
export function slotMinutes(slot) {
	return toMinutes(slot.end) - toMinutes(slot.start);
}

/** Sort slots by day of week, then by start time. Returns a new array. */
export function sortSlots(slots) {
	return [...slots].sort(
		(a, b) => DAY_ORDER.indexOf(a.day) - DAY_ORDER.indexOf(b.day) || toMinutes(a.start) - toMinutes(b.start),
	);
}

/** Total minutes of weekly time shared by two availability lists. */
export function overlapMinutes(slotsA, slotsB) {
	let total = 0;
	for (const a of slotsA) {
		for (const b of slotsB) {
			if (a.day !== b.day) continue;
			const start = Math.max(toMinutes(a.start), toMinutes(b.start));
			const end = Math.min(toMinutes(a.end), toMinutes(b.end));
			if (end > start) total += end - start;
		}
	}
	return total;
}

/** Sum of all slot lengths, in minutes. */
export function totalMinutes(slots) {
	return slots.reduce((sum, slot) => sum + Math.max(0, slotMinutes(slot)), 0);
}

/**
 * Validate one slot list. Returns an array with one entry per slot:
 * an error message, or null when the slot is fine.
 */
export function validateSlots(slots) {
	return slots.map((slot, index) => {
		if (!slot.day || !slot.start || !slot.end) return 'Choose a day, start and end time.';
		if (toMinutes(slot.end) <= toMinutes(slot.start)) return 'End time must be after start time.';
		const clashes = slots.some(
			(other, otherIndex) =>
				otherIndex !== index && other.day === slot.day && overlapMinutes([slot], [other]) > 0,
		);
		return clashes ? 'This slot overlaps another slot on the same day.' : null;
	});
}

/** "Wed 19:00–21:00" */
export function formatSlot(slot) {
	const day = DAYS.find((d) => d.value === slot.day)?.label ?? slot.day;
	return `${day} ${slot.start}–${slot.end}`;
}

/** 150 -> "2h 30m", 60 -> "1h", 0 -> "0h" */
export function formatDuration(minutes) {
	const hours = Math.floor(minutes / 60);
	const rest = minutes % 60;
	if (hours === 0 && rest === 0) return '0h';
	return [hours ? `${hours}h` : '', rest ? `${rest}m` : ''].filter(Boolean).join(' ');
}
