import { describe, expect, it } from 'vitest';
import { calendarFileName, googleCalendarUrl, nextOccurrence, outlookCalendarUrl, sessionTimesFromSlot, toLocalInput } from './calendar';

// Wednesday 4 Nov 2026, 18:00 local time.
const NOW = new Date(2026, 10, 4, 18, 0);
const session = {
	title: 'Paper & drill',
	groupName: 'IS442 Crew',
	startsAt: '2026-11-04T11:00:00Z',
	endsAt: '2026-11-04T13:00:00Z',
	location: 'GSR 2-1',
	note: 'Bring notes',
};

describe('nextOccurrence', () => {
	it('uses today when the slot is still ahead', () => {
		expect(toLocalInput(nextOccurrence('WED', '19:00', NOW))).toBe('2026-11-04T19:00');
	});

	it('uses next week when the slot has passed or is starting now', () => {
		expect(toLocalInput(nextOccurrence('WED', '17:00', NOW))).toBe('2026-11-11T17:00');
		expect(toLocalInput(nextOccurrence('WED', '18:00', NOW))).toBe('2026-11-11T18:00');
	});

	it('accepts the full day names and seconds the backend sends', () => {
		expect(toLocalInput(nextOccurrence('MONDAY', '09:30:00', NOW))).toBe('2026-11-09T09:30');
	});
});

describe('sessionTimesFromSlot', () => {
	it('fills both times from either slot shape', () => {
		expect(sessionTimesFromSlot({ day: 'FRIDAY', startTime: '19:00:00', endTime: '21:00:00' }, NOW)).toEqual({ startsAt: '2026-11-06T19:00', endsAt: '2026-11-06T21:00' });
		expect(sessionTimesFromSlot({ day: 'FRI', start: '19:00', end: '21:00' }, NOW)).toEqual({ startsAt: '2026-11-06T19:00', endsAt: '2026-11-06T21:00' });
	});
});

describe('calendar links', () => {
	it('builds a Google link with UTC times and encoded text', () => {
		const url = new URL(googleCalendarUrl(session));
		expect(url.searchParams.get('dates')).toBe('20261104T110000Z/20261104T130000Z');
		expect(url.searchParams.get('text')).toBe('Paper & drill');
		expect(url.searchParams.get('details')).toBe('Study group: IS442 Crew\nBring notes');
		expect(url.searchParams.get('location')).toBe('GSR 2-1');
	});

	it('builds an Outlook link', () => {
		const url = new URL(outlookCalendarUrl({ ...session, note: null, location: null }));
		expect(url.searchParams.get('subject')).toBe('Paper & drill');
		expect(url.searchParams.get('startdt')).toBe('2026-11-04T11:00:00.000Z');
		expect(url.searchParams.get('body')).toBe('Study group: IS442 Crew');
		expect(url.searchParams.get('location')).toBe('');
	});

	it('makes a safe file name', () => {
		expect(calendarFileName('Paper drill!')).toBe('paper-drill.ics');
		expect(calendarFileName('复习')).toBe('study-session.ics');
	});
});

describe('how to join', () => {
	it('puts the meeting link or video chat in the calendar details', () => {
		const details = (extra) => new URL(googleCalendarUrl({ ...session, ...extra })).searchParams.get('details');
		expect(details({ meetingLink: 'https://meet.example.com/abc' })).toContain('Join: https://meet.example.com/abc');
		expect(details({ groupVideoChat: true })).toContain('Telegram chat');
		expect(details({})).toBe('Study group: IS442 Crew\nBring notes');
	});
});
