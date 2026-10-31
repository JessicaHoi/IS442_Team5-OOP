/** Telegram usernames and links, shared by the profile form and the mock API. */
const LINK_PREFIX = /^(https?:\/\/)?(www\.)?t\.me\//i;
const USERNAME = /^[A-Za-z][A-Za-z0-9_]{3,30}[A-Za-z0-9]$/;
const CHAT_LINK = /^https:\/\/t\.me\/[A-Za-z0-9_+/?=&-]{1,240}$/;

export const INVALID_HANDLE_MESSAGE = 'Enter a valid Telegram username, e.g. @aisha_r.';
export const INVALID_CHAT_LINK_MESSAGE = 'Enter a Telegram link that starts with https://t.me/.';

/** The bare username, an empty string for an empty field, or null if it isn't a valid username. */
export function normalizeTelegramHandle(raw) {
	const trimmed = (raw ?? '').trim();
	if (!trimmed) return '';
	const handle = trimmed.replace(LINK_PREFIX, '').replace(/^@/, '');
	return USERNAME.test(handle) ? handle : null;
}

export const isValidChatLink = (url) => CHAT_LINK.test((url ?? '').trim());

export const telegramProfileUrl = (handle) => `https://t.me/${handle}`;

/** Whether a member's card should fetch an invite: they have none, were removed, or theirs has expired. */
export function needsInvite(me, now = Date.now()) {
	if (me?.state === 'NOT_INVITED' || me?.state === 'REMOVED') return true;
	return me?.state === 'INVITED' && new Date(me.inviteExpiresAt).getTime() <= now;
}
