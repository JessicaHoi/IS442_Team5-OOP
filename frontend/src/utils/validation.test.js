import { describe, expect, it } from 'vitest';
import { courseInputError, passwordError } from './validation';

describe('password policy', () => {
	it.each(['123456', ' 1234 ', 'a'.repeat(72), 'é'.repeat(36), '😀'.repeat(18)])('accepts a valid boundary password', (password) => {
		expect(passwordError(password)).toBeNull();
	});
	it.each([null, '', '      ', '12345', 'a'.repeat(73), 'é'.repeat(37), '😀'.repeat(19)])('rejects an invalid boundary password', (password) => {
		expect(passwordError(password)).toBeTypeOf('string');
	});
	it.each(['\u00a0', '\u2007', '\u202f', '\u0085', '\ufeff'])('accepts characters Java does not classify as blank', (character) => {
		expect(passwordError(character.repeat(6))).toBeNull();
	});
	it.each(['\t', '\n', '\u001c', '\u001d', '\u001e', '\u001f', '\u1680', '\u2003', '\u2028', '\u205f', '\u3000'])('rejects Java whitespace-only passwords', (character) => {
		expect(passwordError(character.repeat(6))).toMatch(/must not be blank/);
	});
});

describe('course input validation', () => {
	it.each([['A', 'N'], ['X'.repeat(20), 'N'.repeat(255)]])('accepts boundary inputs', (code, name) => {
		expect(courseInputError(code, name)).toBeNull();
	});
	it.each([['', 'Name'], ['X'.repeat(21), 'Name'], ['IS-442', 'Name'], ['IS442', ''], ['IS442', 'N'.repeat(256)]])('rejects invalid inputs', (code, name) => {
		expect(courseInputError(code, name)).toBeTypeOf('string');
	});
});
