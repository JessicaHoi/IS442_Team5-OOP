package is442t1.studybuddy.auth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import is442t1.studybuddy.common.exception.ValidationException;

class PasswordPolicyTest {

    @Test
    void acceptsSixCharactersAndAtMostSeventyTwoUtf8BytesWithoutTrimming() {
        for (String password : new String[] { "123456", " 1234 ", "a".repeat(72), "é".repeat(36), "😀".repeat(18) }) {
            assertThatCode(() -> PasswordPolicy.validate(password)).doesNotThrowAnyException();
        }
    }

    @Test
    void rejectsNullBlankShortAndOverlongUtf8Passwords() {
        for (String password : new String[] { null, "", "      ", "12345", "a".repeat(73), "é".repeat(37), "😀".repeat(19) }) {
            assertThatThrownBy(() -> PasswordPolicy.validate(password)).isInstanceOf(ValidationException.class);
        }
    }

    @Test
    void usesTheSameUnicodeBlankDefinitionAsTheFrontend() {
        for (char character : new char[] { '\u00a0', '\u2007', '\u202f', '\u0085', '\ufeff' }) {
            assertThatCode(() -> PasswordPolicy.validate(String.valueOf(character).repeat(6)))
                    .doesNotThrowAnyException();
        }
        for (char character : new char[] { '\t', '\n', '\u001c', '\u001d', '\u001e', '\u001f',
                '\u1680', '\u2003', '\u2028', '\u205f', '\u3000' }) {
            assertThatThrownBy(() -> PasswordPolicy.validate(String.valueOf(character).repeat(6)))
                    .isInstanceOf(ValidationException.class);
        }
    }
}
