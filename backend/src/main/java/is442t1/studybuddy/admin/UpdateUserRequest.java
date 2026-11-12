package is442t1.studybuddy.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import is442t1.studybuddy.model.enums.AccountStatus;
import is442t1.studybuddy.model.enums.Role;

/**
 * Account details supplied by an administrator.
 *
 * @param name the account holder's name
 * @param email the account email
 * @param password an optional replacement password
 * @param role the account role
 * @param status the account status
 */
public record UpdateUserRequest(
        @NotBlank(message = "Name is required.")
        @Size(max = 100, message = "Name must be at most 100 characters.") String name,
        @NotBlank(message = "Email is required.")
        @Email(message = "Enter a valid email address.")
        @Size(max = 255, message = "Email must be at most 255 characters.") String email,
        String password,
        @NotNull(message = "Choose a role.") Role role,
        @NotNull(message = "Choose an account status.") AccountStatus status
) {
    /**
     * Creates updated account details, trimming the name and email.
     *
     * @param name the account holder's name
     * @param email the account email
     * @param password an optional replacement password
     * @param role the account role
     * @param status the account status
     */
    public UpdateUserRequest {
        name = name == null ? null : name.trim();
        email = email == null ? null : email.trim();
    }
}
