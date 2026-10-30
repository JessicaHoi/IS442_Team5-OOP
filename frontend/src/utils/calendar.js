/** Dates for study sessions, and links that add a session to someone's calendar. */

const DAY_INDEX = { SUN: 0, MON: 1, TUE: 2, WED: 3, THU: 4, FRI: 5, SAT: 6 };
const pad = (value) => String(value).padStart(2, '0');

/** The value a datetime-local input expects, in the browser's time zone. */
export function toLocalInput(value) {
	const date = new Date(value);
	return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

/** The next time a weekly slot happens. A day may be `WED` or `WEDNESDAY`, a time `19:00` or `19:00:00`. */
export function nextOccurrence(day, time, now = new Date()) {
	const [hours, minutes] = time.split(':').map(Number);
	const date = new Date(now);
	date.setHours(hours, minutes, 0, 0);
	date.setDate(date.getDate() + ((DAY_INDEX[day.slice(0, 3)] - date.getDay() + 7) % 7));
	if (date <= now) date.setDate(date.getDate() + 7);
	return date;
}

/** Start and end form values for a best-time suggestion. */
export function sessionTimesFromSlot(slot, now = new Date()) {
	const start = slot.startTime ?? slot.start;
	const end = slot.endTime ?? slot.end;
	const startsAt = nextOccurrence(slot.day, start, now);
	const [hours, minutes] = end.split(':').map(Number);
	const endsAt = new Date(startsAt);
	endsAt.setHours(hours, minutes, 0, 0);
	return { startsAt: toLocalInput(startsAt), endsAt: toLocalInput(endsAt) };
}

const compactUtc = (iso) => new Date(iso).toISOString().replace(/[-:]|\.\d{3}/g, '');
const calendarDetails = (session) => [`Study group: ${session.groupName}`, session.note].filter(Boolean).join('\n');

export function googleCalendarUrl(session) {
	const params = new URLSearchParams({
		action: 'TEMPLATE',
		text: session.title,
		dates: `${compactUtc(session.startsAt)}/${compactUtc(session.endsAt)}`,
		details: calendarDetails(session),
		location: session.location ?? '',
	});
	return `https://calendar.google.com/calendar/render?${params}`;
}

export function outlookCalendarUrl(session) {
	const params = new URLSearchParams({
		path: '/calendar/action/compose',
		rru: 'addevent',
		subject: session.title,
		startdt: new Date(session.startsAt).toISOString(),
		enddt: new Date(session.endsAt).toISOString(),
		body: calendarDetails(session),
		location: session.location ?? '',
	});
	return `https://outlook.office.com/calendar/0/deeplink/compose?${params}`;
}

/** "Paper drill!" -> "paper-drill.ics" */
export function calendarFileName(title) {
	const slug = title.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '');
	return `${slug || 'study-session'}.ics`;
}
