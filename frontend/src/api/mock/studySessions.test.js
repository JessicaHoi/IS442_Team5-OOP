import { beforeEach, describe, expect, it } from 'vitest';
import { mockAdapter } from './adapter';
import { getDb, resetDb } from './db';

async function call(method, url, { data, token, params } = {}) {
	try {
		return await mockAdapter({ method, url, params, data: data ? JSON.stringify(data) : undefined, headers: token ? { Authorization: `Bearer ${token}` } : {} });
	} catch (error) {
		return error.response;
	}
}

const login = async (user) => (await call('post', '/auth/login', { data: { email: user.email, password: 'password' } })).data.token;
const hoursFromNow = (hours) => new Date(Date.now() + hours * 3600000).toISOString();
const details = (overrides = {}) => ({ title: 'Paper drill', startsAt: hoursFromNow(24), endsAt: hoursFromNow(26), meetingMode: 'IN_PERSON', location: 'GSR 2-1', note: '', ...overrides });

describe('study sessions', () => {
	let db, group, leader, member, stranger, leaderToken, memberToken, strangerToken, url;

	beforeEach(async () => {
		resetDb();
		db = getDb();
		group = db.groups.find((candidate) => candidate.status === 'OPEN' && candidate.memberIds.length > 1);
		leader = db.users.find((user) => user.id === group.leaderId);
		member = db.users.find((user) => group.memberIds.includes(user.id) && user.id !== leader.id && user.status === 'ACTIVE');
		stranger = db.users.find((user) => user.role === 'STUDENT' && user.status === 'ACTIVE' && !group.memberIds.includes(user.id));
		[leaderToken, memberToken, strangerToken] = await Promise.all([leader, member, stranger].map(login));
		url = `/groups/${group.id}/sessions`;
	});

	const schedule = async (overrides) => (await call('post', url, { token: leaderToken, data: details(overrides) })).data;

	it('lets the leader schedule, and members see and answer', async () => {
		const session = await schedule();
		expect(session.status).toBe('SCHEDULED');
		expect((await call('get', url, { token: memberToken, params: { scope: 'upcoming' } })).data[0].id).toBe(session.id);

		const answered = (await call('put', `${url}/${session.id}/rsvp`, { token: memberToken, data: { response: 'GOING' } })).data;
		expect(answered.myRsvp).toBe('GOING');
		expect(answered.goingCount).toBe(1);
		expect(answered.attendees[0].name).toBe(member.name);

		const changed = (await call('put', `${url}/${session.id}/rsvp`, { token: memberToken, data: { response: 'NOT_GOING' } })).data;
		expect(changed.goingCount).toBe(0);
		expect(changed.notGoingCount).toBe(1);
	});

	it('refuses non-leaders before checking the values, and non-members altogether', async () => {
		const session = await schedule();
		expect((await call('post', url, { token: memberToken, data: { title: '' } })).status).toBe(403);
		expect((await call('post', `${url}/${session.id}/cancel`, { token: memberToken })).status).toBe(403);
		expect((await call('get', url, { token: strangerToken })).status).toBe(403);
		expect((await call('put', `${url}/${session.id}/rsvp`, { token: strangerToken, data: { response: 'GOING' } })).status).toBe(403);
		expect((await call('get', `${url}/${session.id}/calendar.ics`, { token: strangerToken })).status).toBe(403);
	});

	it('validates the details', async () => {
		const post = (overrides) => call('post', url, { token: leaderToken, data: details(overrides) });
		expect((await post({ title: ' ' })).status).toBe(400);
		expect((await post({ endsAt: hoursFromNow(24) })).status).toBe(400);
		expect((await post({ endsAt: hoursFromNow(33) })).status).toBe(400);
		expect((await post({ startsAt: hoursFromNow(-1), endsAt: hoursFromNow(1) })).status).toBe(400);
		expect((await post({ meetingMode: 'EITHER' })).status).toBe(400);
		expect((await call('get', url, { token: leaderToken, params: { scope: 'later' } })).status).toBe(400);
	});

	it('cancelling keeps the session visible, stops answers, and leaves the upcoming list', async () => {
		const session = await schedule();
		expect((await call('post', `${url}/${session.id}/cancel`, { token: leaderToken })).data.status).toBe('CANCELLED');
		expect((await call('put', `${url}/${session.id}/rsvp`, { token: memberToken, data: { response: 'GOING' } })).status).toBe(409);
		expect((await call('get', url, { token: memberToken })).data[0].status).toBe('CANCELLED');
		expect((await call('get', '/sessions/upcoming', { token: memberToken })).data).toEqual([]);
	});

	it('closing the group cancels upcoming sessions', async () => {
		const session = await schedule();
		await call('post', `/groups/${group.id}/close`, { token: leaderToken });
		expect(db.studySessions.find((item) => item.id === session.id).status).toBe('CANCELLED');
		expect((await call('post', url, { token: leaderToken, data: details() })).status).toBe(409);
	});

	it('leaving or being removed deletes answers to upcoming sessions', async () => {
		const session = await schedule();
		const stored = () => db.studySessions.find((item) => item.id === session.id);
		await call('put', `${url}/${session.id}/rsvp`, { token: memberToken, data: { response: 'GOING' } });
		await call('post', `/groups/${group.id}/leave`, { token: memberToken });
		expect(stored().rsvps).toEqual([]);

		group.memberIds.push(member.id);
		await call('put', `${url}/${session.id}/rsvp`, { token: memberToken, data: { response: 'GOING' } });
		await call('delete', `/groups/${group.id}/members/${member.id}`, { token: leaderToken });
		expect(stored().rsvps).toEqual([]);
	});

	it('serves a calendar file whose sequence goes up with changes', async () => {
		const session = await schedule();
		const calendarUrl = `${url}/${session.id}/calendar.ics`;
		const file = (await call('get', calendarUrl, { token: memberToken })).data;
		expect(file).toContain(`UID:${session.id}@studybuddy\r\n`);
		expect(file).toContain('SEQUENCE:0\r\n');

		await call('put', `${url}/${session.id}`, { token: leaderToken, data: details({ title: 'New title' }) });
		expect((await call('get', calendarUrl, { token: memberToken })).data).toContain('SEQUENCE:1\r\n');
	});

	it('keeps a meeting link or video chat on online sessions only', async () => {
		const create = (overrides) => call('post', url, { token: leaderToken, data: details({ meetingMode: 'ONLINE', ...overrides }) });

		const linked = await create({ meetingLink: ' https://meet.example.com/abc ' });
		expect(linked.status).toBe(201);
		expect(linked.data).toMatchObject({ meetingLink: 'https://meet.example.com/abc', groupVideoChat: false });
		expect((await create({ groupVideoChat: true })).data).toMatchObject({ meetingLink: null, groupVideoChat: true });

		expect((await create({ meetingLink: 'javascript:alert(1)' })).status).toBe(400);
		expect((await create({ meetingLink: 'meet.example.com' })).status).toBe(400);
		expect((await create({ meetingLink: 'https://meet.example.com', groupVideoChat: true })).status).toBe(400);
		expect((await create({ meetingMode: 'IN_PERSON', groupVideoChat: true })).status).toBe(400);
	});
});
