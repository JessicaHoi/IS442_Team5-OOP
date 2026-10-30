/** Telegram usernames, shared by the profile form and the mock API. */
const LINK_PREFIX = /^(https?:\/\/)?(www\.)?t\.me\//i;
const USERNAME = /^[A-Za-z][A-Za-z0-9_]{3,30}[A-Za-z0-9]$/;

export const INVALID_HANDLE_MESSAGE = 'Enter a valid Telegram username, e.g. @aisha_r.';

/** The bare username, an empty string for an empty field, or null if it isn't a valid username. */
export function normalizeTelegramHandle(raw) {
	const trimmed = (raw ?? '').trim();
	if (!trimmed) return '';
	const handle = trimmed.replace(LINK_PREFIX, '').replace(/^@/, '');
	return USERNAME.test(handle) ? handle : null;
}

export const telegramProfileUrl = (handle) => `https://t.me/${handle}`;
