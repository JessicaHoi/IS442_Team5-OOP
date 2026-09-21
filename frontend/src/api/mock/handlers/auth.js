import { HttpError } from '../errors';
import { route } from '../router';
import { saveDb } from '../db';

export const TOKEN_PREFIX = 'mock-token-';

const toSessionUser = (user) => ({ id: user.id, name: user.name, email: user.email, role: user.role });

route(
	'POST',
	'/auth/login',
	({ db, body }) => {
		const user = db.users.find((u) => u.email.toLowerCase() === String(body.email ?? '').trim().toLowerCase());
		if (!user || user.password !== body.password) throw new HttpError(401, 'Invalid email or password.');
		if (user.status === 'SUSPENDED') throw new HttpError(403, 'This account is suspended. Contact an administrator.');
		user.lastLogin = new Date().toISOString();
		saveDb();
		return { token: `${TOKEN_PREFIX}${user.id}`, user: toSessionUser(user) };
	},
	{ public: true },
);

route('GET', '/auth/me', ({ user }) => toSessionUser(user));

route('GET', '/courses', ({ db }) => db.courses);
