/** Rules for showing study sessions. */

/** How long after the end a leader can still cancel. The backend has the same limit in StudySession. */
const CANCEL_BUFFER_MS = 60 * 60 * 1000;

/** A session stays cancellable until an hour after it ends, because a meeting can fall through after its start. */
export function isTooLateToCancel(session, now = new Date()) {
	return now.getTime() > new Date(session.endsAt).getTime() + CANCEL_BUFFER_MS;
}

/** Which session to highlight in a list sorted by start time: the earliest one still scheduled, or null if there's none. */
export function nextSessionBadge(upcoming, now = new Date()) {
	const next = upcoming.find((session) => session.status === 'SCHEDULED');
	return next ? { sessionId: next.id, started: new Date(next.startsAt) <= now } : null;
}
