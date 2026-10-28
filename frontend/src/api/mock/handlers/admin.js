import { badRequest, conflict, created, noContent, notFound } from '../errors';
import { route } from '../router';
import { nextId } from '../db';
import { revokeUserSessions } from '../sessions';
import { toAccountView } from '../serializers';
import { MATCHING_CRITERIA, MATCHING_STRATEGIES, ROLES, USER_STATUSES } from '../../../utils/constants';

const ADMIN_ONLY = { role: 'ADMIN' };
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const MIN_PASSWORD_LENGTH = 6;

const emptyStudentProfile = () => ({
	school: '',
	programme: '',
	yearOfStudy: null,
	contactNumber: '',
	courses: [],
	preferences: [],
});

function loadUser(db, id) {
	const user = db.users.find((u) => u.id === Number(id));
	if (!user) throw notFound('User not found.');
	return user;
}

function validateAccount(db, { name, email, role, status }, existingId) {
	if (typeof name !== 'string' || !name.trim()) throw badRequest('Name is required.');
	if (name.trim().length > 100) throw badRequest('Name must be at most 100 characters.');
	if (typeof email !== 'string' || !EMAIL_PATTERN.test(email.trim()) || email.trim().length > 255) throw badRequest('Enter a valid email address.');
	if (!Object.values(ROLES).includes(role)) throw badRequest('Choose a role.');
	if (!USER_STATUSES.some((s) => s.value === status)) throw badRequest('Choose an account status.');
	const taken = db.users.some((u) => u.id !== existingId && u.email.toLowerCase() === email.trim().toLowerCase());
	if (taken) throw conflict('Another account already uses this email address.');
}

function validatePassword(password) {
	if (typeof password !== 'string' || !password.trim() || password.length < MIN_PASSWORD_LENGTH) throw badRequest('Password must be at least 6 characters and must not be blank.');
	if (new TextEncoder().encode(password).length > 72) throw badRequest('Password is too long. Use fewer characters.');
}

function ledGroupView(db, group) {
	return {
		id: group.id,
		name: group.name,
		status: group.status,
		eligibleMembers: db.users.filter((user) => group.memberIds.includes(user.id) && user.id !== group.leaderId && user.status === 'ACTIVE' && user.role === ROLES.STUDENT)
			.map(({ id, name }) => ({ id, name })),
	};
}

route('GET', '/admin/users/:id/led-groups', ({ db, params }) => {
	const user = loadUser(db, params.id);
	return db.groups.filter((group) => group.leaderId === user.id).map((group) => ledGroupView(db, group));
}, ADMIN_ONLY);

route('PUT', '/admin/users/:id/led-groups/:groupId/leader', ({ db, params, body }) => {
	const user = loadUser(db, params.id);
	const group = db.groups.find((candidate) => candidate.id === Number(params.groupId) && candidate.leaderId === user.id);
	if (!group) throw notFound('Study group not found for this leader.');
	const successor = db.users.find((candidate) => candidate.id === Number(body.studentId));
	if (!successor || !ledGroupView(db, group).eligibleMembers.some((member) => member.id === successor.id)) {
		throw badRequest('Choose another active member as the new group leader.');
	}
	group.leaderId = successor.id;
	return ledGroupView(db, group);
}, ADMIN_ONLY);

route('GET', '/admin/users', ({ db }) => db.users.map((user) => toAccountView(db, user)), ADMIN_ONLY);

route(
	'POST',
	'/admin/users',
	({ db, body }) => {
		validateAccount(db, body);
		validatePassword(body.password);
		const user = {
			id: nextId('user'),
			name: body.name.trim(),
			email: body.email.trim().toLowerCase(),
			password: body.password,
			role: body.role,
			status: body.status,
			lastLogin: null,
			createdAt: new Date().toISOString(),
		};
		db.users.push(user);
		if (user.role === ROLES.STUDENT) db.students[user.id] = emptyStudentProfile();
		return created(toAccountView(db, user));
	},
	ADMIN_ONLY,
);

route(
	'PUT',
	'/admin/users/:id',
	({ db, user: actor, params, body }) => {
		const user = loadUser(db, params.id);
		validateAccount(db, body, user.id);
		if (body.role !== user.role) throw badRequest('Account roles cannot change after creation.');
		if (user.id === actor.id && (body.role !== user.role || body.status !== 'ACTIVE')) {
			throw badRequest('You cannot change your own role or suspend your own account.');
		}
		if (body.password) validatePassword(body.password);
		Object.assign(user, { name: body.name.trim(), email: body.email.trim().toLowerCase(), status: body.status });
		if (body.password || user.status === 'SUSPENDED') {
			user.sessionVersion = (user.sessionVersion ?? 0) + 1;
			revokeUserSessions(db, user.id);
		}
		if (body.password) user.password = body.password;
		if (user.role === ROLES.STUDENT && !db.students[user.id]) db.students[user.id] = emptyStudentProfile();
		return toAccountView(db, user);
	},
	ADMIN_ONLY,
);

route(
	'DELETE',
	'/admin/users/:id',
	({ db, user: actor, params }) => {
		const user = loadUser(db, params.id);
		if (user.id === actor.id) throw badRequest('You cannot delete your own account.');
		if (db.groups.some((group) => group.leaderId === user.id)) throw conflict('Transfer leadership of every group before deleting this account.');
		revokeUserSessions(db, user.id);
		db.users = db.users.filter((u) => u.id !== user.id);
		delete db.students[user.id];
		db.connections = db.connections.filter((c) => c.fromId !== user.id && c.toId !== user.id);
		db.joinRequests = db.joinRequests.filter((r) => r.studentId !== user.id);
		db.groups.forEach((g) => (g.memberIds = g.memberIds.filter((id) => id !== user.id)));
		return noContent();
	},
	ADMIN_ONLY,
);

route('GET', '/admin/matching-config', ({ db }) => db.matchingConfig, ADMIN_ONLY);

route(
	'PUT',
	'/admin/matching-config',
	({ db, body }) => {
		if (!MATCHING_STRATEGIES.some((s) => s.value === body.strategy)) throw badRequest('Choose a matching strategy.');
		const criteria = MATCHING_CRITERIA.map(({ key }) => body.criteria?.find((c) => c.key === key));
		if (criteria.some((c) => !c)) throw badRequest('Every matching criterion must be configured.');
		if (criteria.some((c) => !Number.isInteger(c.weight) || c.weight < 0 || c.weight > 100)) {
			throw badRequest('Weights must be whole numbers from 0 to 100.');
		}
		if (!criteria.some((c) => c.enabled && c.weight > 0)) {
			throw badRequest('Enable at least one criterion with a weight above 0.');
		}
		db.matchingConfig = {
			strategy: body.strategy,
			criteria: criteria.map(({ key, enabled, weight }) => ({ key, enabled: Boolean(enabled), weight })),
		};
		return db.matchingConfig;
	},
	ADMIN_ONLY,
);
