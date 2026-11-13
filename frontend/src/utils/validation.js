/** Mirrors the existing profile/registration phone constraint. */
export const PHONE_PATTERN = /^\+?[\d\s-]{8,15}$/;

// Character.isWhitespace / String.isBlank in Java: Unicode separators except
// nonbreaking spaces, plus the ASCII whitespace and separator controls below.
const JAVA_SPACE_PATTERN = /^[\u0020\u1680\u2000-\u2006\u2008-\u200a\u2028\u2029\u205f\u3000]$/;

function isBlankPassword(password) {
	return [...password].every((character) => {
		const code = character.charCodeAt(0);
		return (code >= 0x09 && code <= 0x0d) || (code >= 0x1c && code <= 0x1f) || JAVA_SPACE_PATTERN.test(character);
	});
}

export function passwordError(password) {
	if (typeof password !== 'string' || isBlankPassword(password) || password.length < 6) return 'Password must be at least 6 characters and must not be blank.';
	if (new TextEncoder().encode(password).length > 72) return 'Password is too long. Use fewer characters.';
	return null;
}

/** Inputs are normalized by each workflow before validation. */
export function courseInputError(code, name) {
	if (!/^[A-Z0-9]{1,20}$/.test(code)) return 'Enter a course code using up to 20 letters or numbers.';
	if (!name || name.length > 255) return 'Enter a course name of at most 255 characters.';
	return null;
}
