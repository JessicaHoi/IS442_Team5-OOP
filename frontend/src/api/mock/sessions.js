const TOKEN_PREFIX = 'mock-token-';
const SESSION_TTL_MS = 12 * 60 * 60 * 1000;

export function issueSession(db, user) {
	const token = `${TOKEN_PREFIX}${crypto.randomUUID()}`;
	db.sessions ??= {};
	db.sessions[token] = { userId: user.id, version: user.sessionVersion ?? 0, expiresAt: Date.now() + SESSION_TTL_MS };
	return token;
}

export function resolveSession(db, token) {
	const session = db.sessions?.[token];
	if (!session) return null;
	const user = db.users.find((candidate) => candidate.id === session.userId);
	if (!user || user.status !== 'ACTIVE' || (user.sessionVersion ?? 0) !== session.version || session.expiresAt <= Date.now()) {
		delete db.sessions[token];
		return null;
	}
	return user;
}

export function revokeSession(db, token) {
	if (db.sessions) delete db.sessions[token];
}

export function revokeUserSessions(db, userId) {
	for (const [token, session] of Object.entries(db.sessions ?? {})) {
		if (session.userId === userId) delete db.sessions[token];
	}
}
