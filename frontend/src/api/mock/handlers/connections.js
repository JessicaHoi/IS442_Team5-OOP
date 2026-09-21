import { badRequest, conflict, created, forbidden, noContent, notFound } from '../errors';
import { route } from '../router';
import { nextId } from '../db';
import { findActiveConnection, toConnectionView } from '../serializers';
import { MESSAGE_MAX_LENGTH } from '../../../utils/constants';

function loadConnection(db, id) {
	const connection = db.connections.find((c) => c.id === Number(id));
	if (!connection) throw notFound('Connection not found.');
	return connection;
}

const newestFirst = (a, b) => new Date(b.createdAt) - new Date(a.createdAt);

route(
	'POST',
	'/connections',
	({ db, user, body }) => {
		const toId = Number(body.toStudentId);
		if (toId === user.id) throw badRequest('You cannot send a request to yourself.');
		if (!db.students[toId]) throw notFound('Student not found.');
		if ((body.message ?? '').length > MESSAGE_MAX_LENGTH) throw badRequest('Message is too long.');
		if (findActiveConnection(db, user.id, toId)) {
			throw conflict('You already have a pending request or connection with this student.');
		}
		const connection = {
			id: nextId('connection'),
			fromId: user.id,
			toId,
			message: (body.message ?? '').trim(),
			status: 'PENDING',
			createdAt: new Date().toISOString(),
			respondedAt: null,
		};
		db.connections.push(connection);
		return created(toConnectionView(db, connection, user.id));
	},
	{ role: 'STUDENT' },
);

route(
	'GET',
	'/connections',
	({ db, user, query }) => {
		const filters = {
			incoming: (c) => c.status === 'PENDING' && c.toId === user.id,
			sent: (c) => c.status === 'PENDING' && c.fromId === user.id,
			active: (c) => c.status === 'ACCEPTED' && (c.fromId === user.id || c.toId === user.id),
		};
		const matches = filters[query.view] ?? filters.active;
		return db.connections
			.filter(matches)
			.sort(newestFirst)
			.map((c) => toConnectionView(db, c, user.id));
	},
	{ role: 'STUDENT' },
);

function respond(newStatus) {
	return ({ db, user, params }) => {
		const connection = loadConnection(db, params.id);
		if (connection.toId !== user.id) throw forbidden('Only the recipient can respond to this request.');
		if (connection.status !== 'PENDING') throw conflict('This request has already been answered.');
		connection.status = newStatus;
		connection.respondedAt = new Date().toISOString();
		return toConnectionView(db, connection, user.id);
	};
}

route('POST', '/connections/:id/accept', respond('ACCEPTED'), { role: 'STUDENT' });
route('POST', '/connections/:id/decline', respond('DECLINED'), { role: 'STUDENT' });

route(
	'DELETE',
	'/connections/:id',
	({ db, user, params }) => {
		const connection = loadConnection(db, params.id);
		if (![connection.fromId, connection.toId].includes(user.id)) throw forbidden();
		if (connection.status !== 'ACCEPTED') throw conflict('Only active connections can be ended.');
		connection.status = 'ENDED';
		connection.respondedAt = new Date().toISOString();
		return noContent();
	},
	{ role: 'STUDENT' },
);
