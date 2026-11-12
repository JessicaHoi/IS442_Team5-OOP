package is442t1.studybuddy.auth;

import java.util.UUID;

import is442t1.studybuddy.model.User;
import is442t1.studybuddy.model.enums.Role;

/**
 * Signed-in user as stored by the front end.
 *
 * @param id the account identifier
 * @param name the display name
 * @param email the account email
 * @param role the account role
 */
public record SessionUser(UUID id, String name, String email, Role role) {

    /**
     * Converts an account to the session representation.
     *
     * @param user the account
     * @return the session user
     */
    public static SessionUser from(User user) {
        return new SessionUser(user.getId(), user.getDisplayName(), user.getEmail(), user.getRole());
    }
}
