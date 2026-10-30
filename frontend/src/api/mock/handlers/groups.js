import { badRequest, conflict, created, forbidden, noContent, notFound } from '../errors';
import { route } from '../router';
import { nextId } from '../db';
import { cancelUpcomingSessions, removeUpcomingRsvps } from '../studySessions';
import { ledGroupView, transferGroupLeadership } from '../groupLeadership';
import { toGroupDetail, toGroupSummary, toStudentRef } from '../serializers';
import { validateSlots } from '../../../utils/availability';
import { INVALID_CHAT_LINK_MESSAGE, isValidChatLink } from '../../../utils/telegram';
import { MAX_GROUP_SIZE, MEETING_MODES, MIN_GROUP_SIZE, STUDY_GOALS } from '../../../utils/constants';

const isOneOf = (options, value) => options.some((option) => option.value === value);

function loadGroup(db, id) {
	const group = db.groups.find((g) => g.id === Number(id));
	if (!group) throw notFound('Study group not found.');
	return group;
}

function loadLedGroup(db, user, id) {
	const group = loadGroup(db, id);
	if (group.leaderId !== user.id) throw forbidden('Only the group leader can do that.');
	return group;
}

function requireOpen(group) {
	if (group.status === 'CLOSED') throw conflict('This group is closed.');
}

/** Validate the editable fields of a group. Throws a 400 describing the first problem. */
function validateGroupFields(fields, minimumSize) {
	const { name, description, goals, meetingMode, availability, maxSize } = fields;
	if (!name?.trim()) throw badRequest('Group name is required.');
	if (!description?.trim()) throw badRequest('Description is required.');
	if (!Array.isArray(goals) || goals.length === 0 || goals.some((goal) => !isOneOf(STUDY_GOALS, goal))) {
		throw badRequest('Choose at least one study goal.');
	}
	if (!isOneOf(MEETING_MODES, meetingMode)) throw badRequest('Choose a meeting mode.');
	if (!Array.isArray(availability) || availability.length === 0 || validateSlots(availability).some(Boolean)) {
		throw badRequest('Add at least one valid availability slot.');
	}
	const size = Number(maxSize);
	if (!Number.isInteger(size) || size < Math.max(MIN_GROUP_SIZE, minimumSize) || size > MAX_GROUP_SIZE) {
		throw badRequest(`Maximum size must be between ${Math.max(MIN_GROUP_SIZE, minimumSize)} and ${MAX_GROUP_SIZE}.`);
	}
}

const pendingRequests = (db, groupId) =>
	db.joinRequests.filter((request) => request.groupId === groupId && request.status === 'PENDING');

route('GET', '/groups', ({ db, user, query }) =>
	db.groups
		.filter((group) => group.status === 'OPEN' && (!query.course || group.courseCode === query.course))
		.map((group) => toGroupSummary(db, group, user.id)),
);

route(
	'GET',
	'/groups/mine',
	({ db, user }) =>
		db.groups
			.filter((group) => group.memberIds.includes(user.id))
			.map((group) => toGroupSummary(db, group, user.id))
			.sort((a, b) => (b.myRole === 'LEADER') - (a.myRole === 'LEADER') || a.name.localeCompare(b.name)),
	{ role: 'STUDENT' },
);

route(
	'POST',
	'/groups',
	({ db, user, body }) => {
		if (!db.courses.some((course) => course.code === body.courseCode)) throw badRequest('Choose a course.');
		validateGroupFields(body, 1);
		const group = {
			id: nextId('group'),
			name: body.name.trim(),
			description: body.description.trim(),
			courseCode: body.courseCode,
			goals: body.goals,
			meetingMode: body.meetingMode,
			availability: body.availability,
			maxSize: Number(body.maxSize),
			status: 'OPEN',
			leaderId: user.id,
			memberIds: [user.id],
			createdAt: new Date().toISOString(),
		};
		db.groups.push(group);
		return created(toGroupDetail(db, group, user.id));
	},
	{ role: 'STUDENT' },
);

route('GET', '/groups/:id', ({ db, user, params }) => toGroupDetail(db, loadGroup(db, params.id), user.id), {
	role: 'STUDENT',
});

route('GET', '/groups/:id/leadership', ({ db, user, params }) => ledGroupView(db, loadLedGroup(db, user, params.id)), {
	role: 'STUDENT',
});

route('PUT', '/groups/:id/leader', ({ db, user, params, body }) => transferGroupLeadership(db, loadLedGroup(db, user, params.id), body.studentId), {
	role: 'STUDENT',
});

route(
	'PUT',
	'/groups/:id',
	({ db, user, params, body }) => {
		const group = loadLedGroup(db, user, params.id);
		requireOpen(group);
		validateGroupFields(body, group.memberIds.length);
		Object.assign(group, {
			name: body.name.trim(),
			description: body.description.trim(),
			goals: body.goals,
			meetingMode: body.meetingMode,
			availability: body.availability,
			maxSize: Number(body.maxSize),
		});
		return toGroupDetail(db, group, user.id);
	},
	{ role: 'STUDENT' },
);

route(
	'POST',
	'/groups/:id/close',
	({ db, user, params }) => {
		const group = loadLedGroup(db, user, params.id);
		requireOpen(group);
		group.status = 'CLOSED';
		pendingRequests(db, group.id).forEach((request) => (request.status = 'REJECTED'));
		cancelUpcomingSessions(db, group.id);
		return toGroupDetail(db, group, user.id);
	},
	{ role: 'STUDENT' },
);

route(
	'POST',
	'/groups/:id/join-requests',
	({ db, user, params, body }) => {
		const group = loadGroup(db, params.id);
		requireOpen(group);
		if (db.users.find((candidate) => candidate.id === group.leaderId)?.status !== 'ACTIVE') throw conflict('The student and group leader must have active accounts to request membership.');
		if (group.memberIds.includes(user.id)) throw conflict('You are already in this group.');
		if (group.memberIds.length >= group.maxSize) throw conflict('This group is full.');
		if (pendingRequests(db, group.id).some((request) => request.studentId === user.id)) {
			throw conflict('You already have a pending request for this group.');
		}
		db.joinRequests.push({
			id: nextId('joinRequest'),
			groupId: group.id,
			studentId: user.id,
			message: (body.message ?? '').trim(),
			status: 'PENDING',
			createdAt: new Date().toISOString(),
		});
		return created(toGroupSummary(db, group, user.id));
	},
	{ role: 'STUDENT' },
);

route(
	'GET',
	'/groups/:id/join-requests',
	({ db, user, params }) => {
		const group = loadGroup(db, params.id);
		if (!group.memberIds.includes(user.id)) throw forbidden('Only group members can view join requests.');
		return pendingRequests(db, group.id).map((request) => ({
			id: request.id,
			student: toStudentRef(db, request.studentId),
			message: request.message,
			createdAt: request.createdAt,
		}));
	},
	{ role: 'STUDENT' },
);

function decideRequest(accept) {
	return ({ db, user, params }) => {
		const group = loadLedGroup(db, user, params.id);
		requireOpen(group);
		const request = pendingRequests(db, group.id).find((r) => r.id === Number(params.rid));
		if (!request) throw notFound('Join request not found.');
		if (accept) {
			if (db.users.find((candidate) => candidate.id === request.studentId)?.status !== 'ACTIVE') throw conflict('The student and group leader must have active accounts to accept membership.');
			if (group.memberIds.length >= group.maxSize) throw conflict('The group is full. Remove a member or raise the maximum size first.');
			group.memberIds.push(request.studentId);
		}
		request.status = accept ? 'ACCEPTED' : 'REJECTED';
		return toGroupDetail(db, group, user.id);
	};
}

route('POST', '/groups/:id/join-requests/:rid/accept', decideRequest(true), { role: 'STUDENT' });
route('POST', '/groups/:id/join-requests/:rid/reject', decideRequest(false), { role: 'STUDENT' });

route(
	'DELETE',
	'/groups/:id/members/:studentId',
	({ db, user, params }) => {
		const group = loadLedGroup(db, user, params.id);
		const studentId = Number(params.studentId);
		if (studentId === group.leaderId) throw badRequest('The group leader cannot be removed.');
		if (!group.memberIds.includes(studentId)) throw notFound('This student is not a member.');
		group.memberIds = group.memberIds.filter((id) => id !== studentId);
		delete group.availabilityOverrides?.[studentId];
		removeUpcomingRsvps(db, group.id, studentId);
		return noContent();
	},
	{ role: 'STUDENT' },
);

route(
	'POST',
	'/groups/:id/leave',
	({ db, user, params }) => {
		const group = loadGroup(db, params.id);
		requireOpen(group);
		if (group.leaderId === user.id) throw conflict('The group leader cannot leave. Transfer leadership or close the group first.');
		if (!group.memberIds.includes(user.id)) throw conflict('You are not a member of this group.');
		group.memberIds = group.memberIds.filter((id) => id !== user.id);
		delete group.availabilityOverrides?.[user.id];
		removeUpcomingRsvps(db, group.id, user.id);
		return noContent();
	},
	{ role: 'STUDENT' },
);

route(
	'PUT',
	'/groups/:id/chat-link',
	({ db, user, params, body }) => {
		const group = loadLedGroup(db, user, params.id);
		requireOpen(group);
		if (!isValidChatLink(body.url)) throw badRequest(INVALID_CHAT_LINK_MESSAGE);
		group.chatLink = body.url.trim();
		return toGroupDetail(db, group, user.id);
	},
	{ role: 'STUDENT' },
);

route(
	'DELETE',
	'/groups/:id/chat-link',
	({ db, user, params }) => {
		const group = loadLedGroup(db, user, params.id);
		requireOpen(group);
		delete group.chatLink;
		return noContent();
	},
	{ role: 'STUDENT' },
);
