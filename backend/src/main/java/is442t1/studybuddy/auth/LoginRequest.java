package is442t1.studybuddy.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Public sign-in payload containing email and password. Invalid credentials produce HTTP 401; a
 *     correctly authenticated suspended account produces HTTP 403.
 */
public record LoginRequest(
        @NotBlank(message = "Enter your email.") String email,
        @NotBlank(message = "Enter your password.") String password
) {
}
