import { describe, expect, it } from 'vitest';
import { calendarFileName, defaultSessionTimes, endAfterStartChange, endTimeOptions, googleCalendarUrl, nextOccurrence, outlookCalendarUrl, sessionTimesFromSlot, startTimeOptions, toLocalInput } from './calendar';

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

describe('endAfterStartChange', () => {
	it('gives a new session two hours', () => {
		expect(endAfterStartChange('2026-11-04T19:00', '', '')).toBe('2026-11-04T21:00');
	});

	it('keeps the length when the start moves', () => {
		expect(endAfterStartChange('2026-11-06T09:00', '2026-11-04T19:00', '2026-11-04T19:45')).toBe('2026-11-06T09:45');
	});

	it('leaves an end alone when the old start is unknown and the end still works', () => {
		expect(endAfterStartChange('2026-11-04T19:00', '', '2026-11-04T19:30')).toBe('2026-11-04T19:30');
	});

	it('replaces an end that was never after the start', () => {
		expect(endAfterStartChange('2026-11-04T19:00', '2026-11-04T18:00', '2026-11-04T17:00')).toBe('2026-11-04T21:00');
	});

	it('leaves the end alone while the start is incomplete', () => {
		expect(endAfterStartChange('', '2026-11-04T19:00', '2026-11-04T21:00')).toBe('2026-11-04T21:00');
	});

	it('writes years with four digits while one is being typed', () => {
		expect(endAfterStartChange('0202-11-04T19:00', '0020-11-04T19:00', '0020-11-04T21:00').slice(0, 4)).toBe('0202');
	});
});

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

describe('defaultSessionTimes', () => {
	it('starts on the next full hour and lasts two hours', () => {
		expect(defaultSessionTimes(new Date(2026, 10, 4, 18, 20))).toEqual({ startsAt: '2026-11-04T19:00', endsAt: '2026-11-04T21:00' });
	});

	it('moves to the next day when it needs to', () => {
		expect(defaultSessionTimes(new Date(2026, 10, 4, 23, 5))).toEqual({ startsAt: '2026-11-05T00:00', endsAt: '2026-11-05T02:00' });
	});
});

describe('startTimeOptions', () => {
	const NOW = new Date(2026, 10, 4, 18, 20);

	it('lists every 15 minutes of the day', () => {
		const options = startTimeOptions('2026-11-05T19:00', NOW);
		expect(options).toHaveLength(96);
		expect(options[0].value).toBe('2026-11-05T00:00');
		expect(options.find((option) => option.value === '2026-11-05T19:00').label).toBe(new Date(2026, 10, 5, 19).toLocaleTimeString('en-SG', { hour: 'numeric', minute: '2-digit' }));
	});

	it('keeps a time off the grid, in order', () => {
		const values = startTimeOptions('2026-11-05T19:07', NOW).map((option) => option.value);
		expect(values).toHaveLength(97);
		expect(values.indexOf('2026-11-05T19:07')).toBe(values.indexOf('2026-11-05T19:00') + 1);
	});

	it('disables only the times already past', () => {
		const options = startTimeOptions('2026-11-04T19:00', NOW);
		expect(options.find((option) => option.value === '2026-11-04T18:15').disabled).toBe(true);
		expect(options.find((option) => option.value === '2026-11-04T18:30').disabled).toBe(false);
	});
});

describe('endTimeOptions', () => {
	it('runs from 15 minutes to 8 hours after the start and shows each length', () => {
		const options = endTimeOptions('2026-11-04T10:00', '2026-11-04T12:00');
		expect(options).toHaveLength(32);
		expect(options[0].label).toMatch(/\(15 mins\)$/);
		expect(options[4].label).toMatch(/\(1 hr 15 mins\)$/);
		expect(options[7].label).toMatch(/\(2 hrs\)$/);
		expect(options.at(-1).value).toBe('2026-11-04T18:00');
	});

	it('says when it runs past midnight', () => {
		const options = endTimeOptions('2026-11-04T23:00', '2026-11-05T00:30');
		expect(options.find((option) => option.value === '2026-11-05T00:30').label).toMatch(/next day \(1 hr 30 mins\)$/);
		expect(options.find((option) => option.value === '2026-11-04T23:45').label).not.toContain('next day');
	});

	it('keeps an end off the grid', () => {
		expect(endTimeOptions('2026-11-04T10:00', '2026-11-04T12:07').map((option) => option.value)).toContain('2026-11-04T12:07');
	});

	it('shows no length for an end that is not after the start', () => {
		expect(endTimeOptions('2026-11-04T10:00', '2026-11-04T09:00')[0].label).not.toContain('(');
	});
});
