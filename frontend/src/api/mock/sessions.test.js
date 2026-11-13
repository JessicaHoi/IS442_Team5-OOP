import { afterEach, describe, expect, it, vi } from 'vitest';
import { getDb, resetDb, saveDb } from './db';
import { issueSession, resolveSession, revokeSession, revokeUserSessions } from './sessions';

const HOUR = 60 * 60 * 1000;

describe('mock session expiry cleanup', () => {
	afterEach(() => {
		vi.useRealTimers();
		vi.unstubAllGlobals();
	});

	it('sweeps abandoned tokens at issuance and rejects them at the exact expiry boundary', () => {
		vi.useFakeTimers();
		vi.setSystemTime(0);
		resetDb();
		const db = getDb();
		const user = db.users.find((user) => user.id === 2);
		const abandoned = issueSession(db, user);
		vi.setSystemTime(HOUR);
		const live = issueSession(db, user);
		vi.setSystemTime(12 * HOUR);
		const fresh = issueSession(db, user);
		expect(Object.keys(db.sessions).sort()).toEqual([live, fresh].sort());
		expect(resolveSession(db, abandoned)).toBeNull();
		expect(resolveSession(db, live)).toBe(user);
		expect(resolveSession(db, fresh)).toBe(user);
		revokeSession(db, live);
		expect(resolveSession(db, live)).toBeNull();
		revokeUserSessions(db, user.id);
		expect(resolveSession(db, fresh)).toBeNull();
	});

	it('removes expired tokens before persistence while keeping live sessions', () => {
		vi.useFakeTimers();
		vi.setSystemTime(0);
		resetDb();
		const db = getDb();
		const user = db.users.find((user) => user.id === 2);
		const expired = issueSession(db, user);
		vi.setSystemTime(HOUR);
		const live = issueSession(db, user);
		vi.setSystemTime(12 * HOUR);
		const setItem = vi.fn();
		vi.stubGlobal('localStorage', { setItem });
		saveDb();
		expect(db.sessions[expired]).toBeUndefined();
		const persisted = JSON.parse(setItem.mock.calls[0][1]);
		expect(persisted.sessions[expired]).toBeUndefined();
		expect(persisted.sessions[live]).toBeDefined();
		expect(resolveSession(db, live)).toBe(user);
	});
});
