import { conflict, forbidden, noContent, notFound } from '../errors';
import { route } from '../router';

/** The mock behaves like a backend with Telegram off, since it can't talk to a real bot. */
const STUDENT_ONLY = { role: 'STUDENT' };
const TELEGRAM_OFF = "Telegram isn't set up on this StudyBuddy server.";

route('GET', '/integrations', () => ({ telegram: { enabled: false, botUsername: null } }));

route('GET', '/students/me/telegram', () => ({ linked: false, username: null, firstName: null, canMessage: false, linkedAt: null }), STUDENT_ONLY);

route(
	'POST',
	'/students/me/telegram/link',
	() => {
		throw conflict(TELEGRAM_OFF);
	},
	STUDENT_ONLY,
);

route('DELETE', '/students/me/telegram', () => noContent(), STUDENT_ONLY);

function memberGroup(db, user, id) {
	const group = db.groups.find((item) => item.id === Number(id));
	if (!group) throw notFound('Study group not found.');
	if (!group.memberIds.includes(user.id)) throw forbidden('Only group members can see the group chat.');
	return group;
}

function refuse({ db, user, params }) {
	memberGroup(db, user, params.id);
	throw conflict(TELEGRAM_OFF);
}

route(
	'GET',
	'/groups/:id/chat',
	({ db, user, params }) => {
		memberGroup(db, user, params.id);
		return { status: 'NOT_LINKED', title: null, chatType: null, missingRights: null, ownedByFormerLeader: false, me: null, members: null };
	},
	STUDENT_ONLY,
);
route('POST', '/groups/:id/chat/setup', refuse, STUDENT_ONLY);
route('POST', '/groups/:id/chat/verify', refuse, STUDENT_ONLY);
route('POST', '/groups/:id/chat/invite', refuse, STUDENT_ONLY);
route('DELETE', '/groups/:id/chat', refuse, STUDENT_ONLY);
