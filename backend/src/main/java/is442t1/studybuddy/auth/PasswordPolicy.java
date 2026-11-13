package is442t1.studybuddy.auth;

import java.nio.charset.StandardCharsets;

import is442t1.studybuddy.common.exception.ValidationException;

/** Password constraints shared by registration and administrator account changes. */
public final class PasswordPolicy {

    private PasswordPolicy() {
    }

    public static void validate(String password) {
        if (password == null || password.isBlank() || password.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters and must not be blank.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ValidationException("Password is too long. Use fewer characters.");
        }
    }
}
