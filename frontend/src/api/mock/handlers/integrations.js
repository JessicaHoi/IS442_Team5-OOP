import { conflict, noContent } from '../errors';
import { route } from '../router';

/** The mock behaves like a backend with Telegram off, since it can't talk to a real bot. */
const STUDENT_ONLY = { role: 'STUDENT' };

route('GET', '/integrations', () => ({ telegram: { enabled: false, botUsername: null } }));

route('GET', '/students/me/telegram', () => ({ linked: false, username: null, firstName: null, canMessage: false, linkedAt: null }), STUDENT_ONLY);

route(
	'POST',
	'/students/me/telegram/link',
	() => {
		throw conflict("Telegram isn't set up on this StudyBuddy server.");
	},
	STUDENT_ONLY,
);

route('DELETE', '/students/me/telegram', () => noContent(), STUDENT_ONLY);
