import { describe, expect, it } from 'vitest';
import { isValidChatLink, normalizeTelegramHandle } from './telegram';

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
