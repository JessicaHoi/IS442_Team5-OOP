package is442t1.studybuddy.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Enter your email.") String email,
        @NotBlank(message = "Enter your password.") String password
) {
}
