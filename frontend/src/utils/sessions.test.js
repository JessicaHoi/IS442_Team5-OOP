import { describe, expect, it } from 'vitest';
import { isTooLateToCancel, nextSessionBadge } from './sessions';

const NOW = new Date('2026-11-04T11:00:00Z');
const session = (id, status, startsAt) => ({ id, status, startsAt });

describe('isTooLateToCancel', () => {
	const ended = (minutesAgo) => ({ endsAt: new Date(NOW.getTime() - minutesAgo * 60000).toISOString() });

	it('allows cancelling up to and including an hour after the end', () => {
		expect(isTooLateToCancel(ended(-30), NOW)).toBe(false);
		expect(isTooLateToCancel(ended(30), NOW)).toBe(false);
		expect(isTooLateToCancel(ended(60), NOW)).toBe(false);
	});

	it('refuses after that', () => {
		expect(isTooLateToCancel(ended(61), NOW)).toBe(true);
	});
});

describe('nextSessionBadge', () => {
	it('picks the earliest scheduled session and skips cancelled ones', () => {
		expect(nextSessionBadge([session(1, 'CANCELLED', '2026-11-05T00:00:00Z'), session(2, 'SCHEDULED', '2026-11-06T00:00:00Z')], NOW))
			.toEqual({ sessionId: 2, started: false });
	});

	it('says when that session has started', () => {
		expect(nextSessionBadge([session(1, 'SCHEDULED', '2026-11-04T11:00:00Z')], NOW)).toEqual({ sessionId: 1, started: true });
	});

	it('finds nothing when no session is scheduled', () => {
		expect(nextSessionBadge([session(1, 'CANCELLED', '2026-11-05T00:00:00Z')], NOW)).toBeNull();
		expect(nextSessionBadge([], NOW)).toBeNull();
	});
});
