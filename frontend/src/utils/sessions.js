/** Rules for showing study sessions. */

/** Which session to highlight in a list sorted by start time: the earliest one still scheduled, or null if there's none. */
export function nextSessionBadge(upcoming, now = new Date()) {
	const next = upcoming.find((session) => session.status === 'SCHEDULED');
	return next ? { sessionId: next.id, started: new Date(next.startsAt) <= now } : null;
}
