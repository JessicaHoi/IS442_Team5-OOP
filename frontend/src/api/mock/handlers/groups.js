import { badRequest, conflict, created, forbidden, noContent, notFound } from '../errors';
import { route } from '../router';
import { nextId } from '../db';
import { toGroupDetail, toGroupSummary, toStudentRef } from '../serializers';
import { validateSlots } from '../../../utils/availability';
import { GROUP_FORMATS, MAX_GROUP_SIZE, MEETING_MODES, MIN_GROUP_SIZE, STUDY_GOALS } from '../../../utils/constants';

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
		const group = loadLedGroup(db, user, params.id);
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
		return noContent();
	},
	{ role: 'STUDENT' },
);
