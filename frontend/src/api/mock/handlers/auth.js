import { HttpError, noContent } from '../errors';
import { route } from '../router';
import { saveDb } from '../db';
import { issueSession, revokeSession } from '../sessions';

const toSessionUser = (user) => ({ id: user.id, name: user.name, email: user.email, role: user.role });

route(
	'POST',
	'/auth/login',
	({ db, body }) => {
		const user = db.users.find((u) => u.email.toLowerCase() === String(body.email ?? '').trim().toLowerCase());
		if (!user || user.password !== body.password) throw new HttpError(401, 'Invalid email or password.');
		if (user.status === 'SUSPENDED') throw new HttpError(403, 'This account is suspended. Contact an administrator.');
		user.lastLogin = new Date().toISOString();
		const token = issueSession(db, user);
		saveDb();
		return { token, user: toSessionUser(user) };
	},
	{ public: true },
);

route('GET', '/auth/me', ({ user }) => toSessionUser(user));

// Each login creates a separate mock session; logout revokes only that session.
route('POST', '/auth/logout', ({ db, token }) => {
	revokeSession(db, token);
	return noContent();
});

route('GET', '/courses', ({ db }) => db.courses);
