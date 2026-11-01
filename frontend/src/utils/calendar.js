/** Dates for study sessions, and links that add a session to someone's calendar. */

const DAY_INDEX = { SUN: 0, MON: 1, TUE: 2, WED: 3, THU: 4, FRI: 5, SAT: 6 };
const pad = (value) => String(value).padStart(2, '0');
const MINUTE_MS = 60 * 1000;
const STEP_MINUTES = 15;
const DEFAULT_LENGTH_MS = 2 * 60 * MINUTE_MS;
/** The backend's limit on a session's length, as in StudySession. */
const MAX_LENGTH_MS = 8 * 60 * MINUTE_MS;
const timeLabel = (date) => date.toLocaleTimeString('en-SG', { hour: 'numeric', minute: '2-digit' });

/** The value a datetime-local input expects, in the browser's time zone. */
export function toLocalInput(value) {
	const date = new Date(value);
	return `${String(date.getFullYear()).padStart(4, '0')}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

/**
 * The end time to show after the start changes: the session keeps its length, a new one gets two hours, and an end
 * that can still stand is left alone. All three are datetime-local values.
 */
export function endAfterStartChange(start, previousStart, end) {
	const startMs = new Date(start).getTime();
	if (Number.isNaN(startMs)) return end;
	const endMs = new Date(end).getTime();
	const previousMs = new Date(previousStart).getTime();
	if (endMs > previousMs) return toLocalInput(startMs + endMs - previousMs);
	return endMs > startMs ? end : toLocalInput(startMs + DEFAULT_LENGTH_MS);
}

/** A new session's times: the next full hour, for the default length. */
export function defaultSessionTimes(now = new Date()) {
	const start = new Date(now);
	start.setHours(start.getHours() + 1, 0, 0, 0);
	return { startsAt: toLocalInput(start), endsAt: toLocalInput(start.getTime() + DEFAULT_LENGTH_MS) };
}

/** "45 mins", "2 hrs", "1 hr 30 mins". */
function lengthLabel(minutes) {
	const hours = Math.floor(minutes / 60);
	const rest = minutes % 60;
	return [hours && `${hours} ${hours === 1 ? 'hr' : 'hrs'}`, rest && `${rest} mins`].filter(Boolean).join(' ');
}

/** Every 15 minutes on the start's day. A time off that grid stays listed, so opening the form never changes it. Past times are disabled. */
export function startTimeOptions(start, now = new Date()) {
	const day = start.slice(0, 10);
	const values = new Set([start.slice(0, 16)]);
	for (let minutes = 0; minutes < 24 * 60; minutes += STEP_MINUTES) {
		values.add(`${day}T${pad(Math.floor(minutes / 60))}:${pad(minutes % 60)}`);
	}
	return [...values].sort().map((value) => {
		const date = new Date(value);
		return { value, label: timeLabel(date), disabled: date <= now };
	});
}

/** Every 15 minutes up to the longest a session can be, each showing the length. The current end stays listed, as with the start. */
export function endTimeOptions(start, end) {
	const startMs = new Date(start).getTime();
	const values = new Set(end ? [end.slice(0, 16)] : []);
	for (let minutes = STEP_MINUTES; minutes * MINUTE_MS <= MAX_LENGTH_MS; minutes += STEP_MINUTES) {
		values.add(toLocalInput(startMs + minutes * MINUTE_MS));
	}
	return [...values].sort().map((value) => {
		const date = new Date(value);
		const minutes = Math.round((date.getTime() - startMs) / MINUTE_MS);
		const nextDay = value.slice(0, 10) !== start.slice(0, 10) ? ' next day' : '';
		return { value, label: `${timeLabel(date)}${nextDay}${minutes > 0 ? ` (${lengthLabel(minutes)})` : ''}` };
	});
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
const howToJoin = (session) => {
	if (session.meetingLink) return `Join: ${session.meetingLink}`;
	return session.groupVideoChat ? 'Video chat in the group\'s Telegram chat' : null;
};
const calendarDetails = (session) => [`Study group: ${session.groupName}`, howToJoin(session), session.note].filter(Boolean).join('\n');

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
