import { describe, expect, it } from 'vitest';
import { isValidChatLink, needsInvite, normalizeTelegramHandle } from './telegram';

describe('normalizeTelegramHandle', () => {
	it.each(['aisha_r', '@aisha_r', '  @aisha_r ', 't.me/aisha_r', 'https://t.me/aisha_r'])('accepts %s', (typed) => {
		expect(normalizeTelegramHandle(typed)).toBe('aisha_r');
	});

	it.each(['abcd', '1abcde', '_abcde', 'abcde_', 'has space', 'a'.repeat(33)])('rejects %s', (typed) => {
		expect(normalizeTelegramHandle(typed)).toBeNull();
	});

	it('treats an empty field as no handle', () => {
		expect(normalizeTelegramHandle('  ')).toBe('');
		expect(normalizeTelegramHandle(null)).toBe('');
	});
});

describe('isValidChatLink', () => {
	it('only accepts https Telegram links', () => {
		expect(isValidChatLink('https://t.me/+AbC_123')).toBe(true);
		expect(isValidChatLink('http://t.me/x')).toBe(false);
		expect(isValidChatLink('https://evil.example/x')).toBe(false);
		expect(isValidChatLink('javascript:alert(1)')).toBe(false);
	});
});

describe('needsInvite', () => {
	const now = Date.parse('2026-11-01T12:00:00Z');

	it('asks for an invite when there is none, or after a removal', () => {
		expect(needsInvite({ state: 'NOT_INVITED' }, now)).toBe(true);
		expect(needsInvite({ state: 'REMOVED' }, now)).toBe(true);
	});

	it('replaces an expired invite and keeps a live one', () => {
		expect(needsInvite({ state: 'INVITED', inviteExpiresAt: '2026-11-01T11:59:00Z' }, now)).toBe(true);
		expect(needsInvite({ state: 'INVITED', inviteExpiresAt: '2026-11-01T12:01:00Z' }, now)).toBe(false);
	});

	it('leaves everyone else alone', () => {
		expect(needsInvite({ state: 'JOINED' }, now)).toBe(false);
		expect(needsInvite({ state: 'LEFT' }, now)).toBe(false);
		expect(needsInvite(null, now)).toBe(false);
	});
});
