import { createSeedData } from './seed';

// Bump the version whenever the stored shape changes, so old browser data is not reused.
const STORAGE_KEY = 'studybuddy-mock-db-v2';
let db = null;

function readStorage() {
	try {
		const raw = globalThis.localStorage?.getItem(STORAGE_KEY);
		return raw ? JSON.parse(raw) : null;
	} catch {
		return null;
	}
}

/** The mock database. Loaded from localStorage if present, otherwise seeded. */
export function getDb() {
	if (!db) db = readStorage() ?? createSeedData();
	return db;
}

/** Persist changes so a page refresh does not wipe a live demo. */
export function saveDb() {
	try {
		globalThis.localStorage?.setItem(STORAGE_KEY, JSON.stringify(getDb()));
	} catch {
		// Storage can be unavailable (private mode, quota); the in-memory copy still works.
	}
}

/** Discard all changes and re-seed. */
export function resetDb() {
	db = createSeedData();
	try {
		globalThis.localStorage?.removeItem(STORAGE_KEY);
	} catch {
		// ignore
	}
}

export function nextId(kind) {
	const database = getDb();
	return database.nextIds[kind]++;
}
