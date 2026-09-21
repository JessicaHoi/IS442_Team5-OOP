/**
 * Turn database records into the JSON shapes described in the front-end README.
 * The privacy rule lives here: contact numbers are only revealed to the owner
 * or to a student with an accepted connection.
 */
import { CONNECTION_STATUS } from '../../utils/constants';

export function findUser(db, id) {
	return db.users.find((user) => user.id === id);
}

/** The pending/accepted connection between two students, if any. */
export function findActiveConnection(db, idA, idB) {
	return db.connections.find(
		(connection) =>
			['PENDING', 'ACCEPTED'].includes(connection.status) &&
			((connection.fromId === idA && connection.toId === idB) ||
				(connection.fromId === idB && connection.toId === idA)),
	);
}

/** Connection state as seen from `viewerId`'s side. */
export function connectionStatusBetween(db, viewerId, otherId) {
	const connection = findActiveConnection(db, viewerId, otherId);
	if (!connection) return { status: CONNECTION_STATUS.NONE, connection: null };
	if (connection.status === 'ACCEPTED') return { status: CONNECTION_STATUS.CONNECTED, connection };
	return {
		status: connection.fromId === viewerId ? CONNECTION_STATUS.PENDING_SENT : CONNECTION_STATUS.PENDING_RECEIVED,
		connection,
	};
}

export function toPublicProfile(db, studentId, viewerId) {
	const user = findUser(db, studentId);
	const student = db.students[studentId];
	const { status, connection } = connectionStatusBetween(db, viewerId, studentId);
	const contactVisible = viewerId === studentId || status === CONNECTION_STATUS.CONNECTED;
	return {
		id: studentId,
		name: user.name,
		school: student.school,
		programme: student.programme,
		yearOfStudy: student.yearOfStudy,
		courses: student.courses,
		preferences: student.preferences,
		contactNumber: contactVisible ? student.contactNumber : null,
		contactVisible,
		connectionStatus: status,
		connectionId: connection?.id ?? null,
	};
}

/** Own profile: public profile plus email. */
export function toOwnProfile(db, studentId) {
	return { ...toPublicProfile(db, studentId, studentId), email: findUser(db, studentId).email };
}

export function toConnectionView(db, connection, viewerId) {
	const otherId = connection.fromId === viewerId ? connection.toId : connection.fromId;
	return {
		id: connection.id,
		status: connection.status,
		direction: connection.fromId === viewerId ? 'OUTGOING' : 'INCOMING',
		message: connection.message,
		createdAt: connection.createdAt,
		respondedAt: connection.respondedAt,
		other: toPublicProfile(db, otherId, viewerId),
	};
}

/** Compact student reference used in group member and join-request lists. */
export function toStudentRef(db, studentId) {
	const student = db.students[studentId];
	return {
		id: studentId,
		name: findUser(db, studentId).name,
		programme: student.programme,
		yearOfStudy: student.yearOfStudy,
	};
}

export function toGroupSummary(db, group, viewerId) {
	const course = db.courses.find((c) => c.code === group.courseCode);
	const isLeader = group.leaderId === viewerId;
	const isMember = group.memberIds.includes(viewerId);
	const hasPendingRequest = db.joinRequests.some(
		(request) => request.groupId === group.id && request.studentId === viewerId && request.status === 'PENDING',
	);
	return {
		id: group.id,
		name: group.name,
		description: group.description,
		courseCode: group.courseCode,
		courseName: course?.name ?? '',
		goals: group.goals,
		meetingMode: group.meetingMode,
		availability: group.availability,
		maxSize: group.maxSize,
		status: group.status,
		createdAt: group.createdAt,
		leader: { id: group.leaderId, name: findUser(db, group.leaderId).name },
		memberCount: group.memberIds.length,
		myRole: isLeader ? 'LEADER' : isMember ? 'MEMBER' : 'NONE',
		myRequestStatus: hasPendingRequest ? 'PENDING' : 'NONE',
	};
}

export function toGroupDetail(db, group, viewerId) {
	const summary = toGroupSummary(db, group, viewerId);
	const canSeeMembers = summary.myRole !== 'NONE';
	return {
		...summary,
		members: canSeeMembers
			? group.memberIds.map((id) => ({ ...toStudentRef(db, id), isLeader: id === group.leaderId }))
			: [],
	};
}

export function toAccountView(db, user) {
	return {
		id: user.id,
		name: user.name,
		email: user.email,
		role: user.role,
		status: user.status,
		lastLogin: user.lastLogin,
		createdAt: user.createdAt,
		connectionCount: db.connections.filter(
			(c) => c.status === 'ACCEPTED' && (c.fromId === user.id || c.toId === user.id),
		).length,
		groupCount: db.groups.filter((g) => g.memberIds.includes(user.id)).length,
	};
}
