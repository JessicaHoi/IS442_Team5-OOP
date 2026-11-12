package is442t1.studybuddy.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Credentials supplied when signing in.
 *
 * @param email the account email
 * @param password the account password
 */
public record LoginRequest(
        @NotBlank(message = "Enter your email.") String email,
        @NotBlank(message = "Enter your password.") String password
) {
}
