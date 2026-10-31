import { badRequest, conflict, created, forbidden, notFound } from '../errors';
import { route } from '../router';
import { nextId } from '../db';

const STUDENT_ONLY = { role: 'STUDENT' };
const HOURS_8 = 8 * 60 * 60 * 1000;

function loadGroup(db, id) {
	const group = db.groups.find((item) => item.id === Number(id));
	if (!group) throw notFound('Study group not found.');
	return group;
}

function memberGroup(db, user, id) {
	const group = loadGroup(db, id);
	if (!group.memberIds.includes(user.id)) throw forbidden('Only group members can see study sessions.');
	return group;
}

function ledGroup(db, user, id) {
	const group = loadGroup(db, id);
	if (group.leaderId !== user.id) throw forbidden('Only the group leader can do that.');
	return group;
}

function loadSession(db, group, id) {
	const session = db.studySessions.find((item) => item.id === Number(id) && item.groupId === group.id);
	if (!session) throw notFound('Study session not found.');
	return session;
}

function requireChangeable(session) {
	if (session.status === 'CANCELLED') throw conflict('This session has been cancelled.');
	if (new Date(session.startsAt) <= new Date()) throw conflict('This session has already started.');
}

/** How an online session meets, checked the way the backend's StudySession checks it. */
function validOnlineOption(body) {
	const meetingLink = body.meetingLink?.trim() || null;
	const groupVideoChat = body.groupVideoChat === true;
	if (!meetingLink && !groupVideoChat) return { meetingLink, groupVideoChat };
	if (body.meetingMode !== 'ONLINE') throw badRequest('Only online sessions can have a meeting link or video chat.');
	if (meetingLink && groupVideoChat) throw badRequest('Choose a meeting link or the group\'s video chat, not both.');
	if (meetingLink && (meetingLink.length > 500 || !/^https?:\/\/[^\s/]+\S*$/i.test(meetingLink))) throw badRequest('Enter a full meeting link starting with https://.');
	return { meetingLink, groupVideoChat };
}

/** The editable fields, checked the way the backend's StudySession checks them. */
function validFields(body) {
	const title = body.title?.trim();
	if (!title) throw badRequest('Give the session a title.');
	if (title.length > 80) throw badRequest('Title must be at most 80 characters.');
	const start = new Date(body.startsAt).getTime();
	const end = new Date(body.endsAt).getTime();
	if (Number.isNaN(start) || Number.isNaN(end)) throw badRequest('Choose a start and end time.');
	if (end <= start) throw badRequest('The session must end after it starts.');
	if (end - start > HOURS_8) throw badRequest('A session can be at most 8 hours long.');
	if (start <= Date.now()) throw badRequest('Choose a start time in the future.');
	if (!['IN_PERSON', 'ONLINE'].includes(body.meetingMode)) throw badRequest('Choose in-person or online.');
	const location = body.location?.trim() || null;
	if (location?.length > 200) throw badRequest('Location must be at most 200 characters.');
	const note = body.note?.trim() || null;
	if (note?.length > 500) throw badRequest('Note must be at most 500 characters.');
	return { title, startsAt: body.startsAt, endsAt: body.endsAt, meetingMode: body.meetingMode, location, note, ...validOnlineOption(body) };
}

function toSession(db, session, viewerId) {
	const group = db.groups.find((item) => item.id === session.groupId);
	const attendees = session.rsvps
		.map((rsvp) => ({ id: rsvp.studentId, name: db.users.find((user) => user.id === rsvp.studentId).name, response: rsvp.response }))
		.sort((a, b) => a.name.localeCompare(b.name));
	return {
		id: session.id,
		groupId: session.groupId,
		groupName: group.name,
		title: session.title,
		startsAt: session.startsAt,
		endsAt: session.endsAt,
		meetingMode: session.meetingMode,
		location: session.location,
		note: session.note,
		meetingLink: session.meetingLink ?? null,
		groupVideoChat: session.groupVideoChat ?? false,
		status: session.status,
		myRsvp: session.rsvps.find((rsvp) => rsvp.studentId === viewerId)?.response ?? null,
		goingCount: attendees.filter((attendee) => attendee.response === 'GOING').length,
		notGoingCount: attendees.filter((attendee) => attendee.response === 'NOT_GOING').length,
		attendees,
	};
}

const byStart = (a, b) => new Date(a.startsAt) - new Date(b.startsAt);
const hasEnded = (session) => new Date(session.endsAt) <= new Date();
const compactUtc = (iso) => new Date(iso).toISOString().replace(/[-:]|\.\d{3}/g, '');
const escapeIcs = (text) => text.replace(/\\/g, '\\\\').replace(/;/g, '\\;').replace(/,/g, '\\,').replace(/\r?\n/g, '\\n');

/** Enough of a calendar file to try the download. The backend's IcsWriter also folds long lines and adds a reminder. */
function toIcs(session) {
	return [
		'BEGIN:VCALENDAR',
		'VERSION:2.0',
		'PRODID:-//StudyBuddy//Study Sessions//EN',
		'METHOD:PUBLISH',
		'BEGIN:VEVENT',
		`UID:${session.id}@studybuddy`,
		`SEQUENCE:${session.icsSequence}`,
		`DTSTAMP:${compactUtc(new Date())}`,
		`DTSTART:${compactUtc(session.startsAt)}`,
		`DTEND:${compactUtc(session.endsAt)}`,
		`SUMMARY:${escapeIcs(session.title)}`,
		`STATUS:${session.status === 'CANCELLED' ? 'CANCELLED' : 'CONFIRMED'}`,
		'END:VEVENT',
		'END:VCALENDAR',
		'',
	].join('\r\n');
}

route('GET', '/sessions/upcoming', ({ db, user }) => db.studySessions
	.filter((session) => session.status === 'SCHEDULED' && !hasEnded(session)
		&& db.groups.find((group) => group.id === session.groupId).memberIds.includes(user.id))
	.sort(byStart)
	.map((session) => toSession(db, session, user.id)), STUDENT_ONLY);

route('GET', '/groups/:id/sessions', ({ db, user, params, query }) => {
	const group = memberGroup(db, user, params.id);
	const scope = query.scope ?? 'upcoming';
	if (!['upcoming', 'past'].includes(scope)) throw badRequest('Scope must be upcoming or past.');
	const sessions = db.studySessions.filter((session) => session.groupId === group.id && hasEnded(session) === (scope === 'past')).sort(byStart);
	return (scope === 'past' ? sessions.reverse() : sessions).map((session) => toSession(db, session, user.id));
}, STUDENT_ONLY);

route('POST', '/groups/:id/sessions', ({ db, user, params, body }) => {
	const group = ledGroup(db, user, params.id);
	if (group.status === 'CLOSED') throw conflict('This group is closed.');
	const session = { id: nextId('studySession'), groupId: group.id, ...validFields(body), status: 'SCHEDULED', icsSequence: 0, rsvps: [] };
	db.studySessions.push(session);
	return created(toSession(db, session, user.id));
}, STUDENT_ONLY);

route('PUT', '/groups/:id/sessions/:sid', ({ db, user, params, body }) => {
	const group = ledGroup(db, user, params.id);
	const session = loadSession(db, group, params.sid);
	requireChangeable(session);
	Object.assign(session, validFields(body));
	session.icsSequence += 1;
	return toSession(db, session, user.id);
}, STUDENT_ONLY);

route('POST', '/groups/:id/sessions/:sid/cancel', ({ db, user, params }) => {
	const group = ledGroup(db, user, params.id);
	const session = loadSession(db, group, params.sid);
	requireChangeable(session);
	session.status = 'CANCELLED';
	session.icsSequence += 1;
	return toSession(db, session, user.id);
}, STUDENT_ONLY);

route('PUT', '/groups/:id/sessions/:sid/rsvp', ({ db, user, params, body }) => {
	const group = loadGroup(db, params.id);
	const session = loadSession(db, group, params.sid);
	if (!group.memberIds.includes(user.id)) throw forbidden('Only group members can respond.');
	requireChangeable(session);
	if (!['GOING', 'NOT_GOING'].includes(body.response)) throw badRequest('Choose GOING or NOT_GOING.');
	const existing = session.rsvps.find((rsvp) => rsvp.studentId === user.id);
	if (existing) existing.response = body.response;
	else session.rsvps.push({ studentId: user.id, response: body.response });
	return toSession(db, session, user.id);
}, STUDENT_ONLY);

route('GET', '/groups/:id/sessions/:sid/calendar.ics', ({ db, user, params }) => {
	const group = memberGroup(db, user, params.id);
	return toIcs(loadSession(db, group, params.sid));
}, STUDENT_ONLY);
