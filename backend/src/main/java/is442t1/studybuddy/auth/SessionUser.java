package is442t1.studybuddy.auth;

import java.util.UUID;

import is442t1.studybuddy.model.User;
import is442t1.studybuddy.model.enums.Role;

/** Signed-in user as the front end stores it: {@code {id, name, email, role}}. */
public record SessionUser(UUID id, String name, String email, Role role) {

    public static SessionUser from(User user) {
        return new SessionUser(user.getId(), user.getDisplayName(), user.getEmail(), user.getRole());
    }
}
