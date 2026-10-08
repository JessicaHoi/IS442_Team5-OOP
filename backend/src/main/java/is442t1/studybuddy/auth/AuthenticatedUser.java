package is442t1.studybuddy.auth;

import java.util.UUID;

import is442t1.studybuddy.model.enums.Role;

/** The signed-in caller of the current request. */
public record AuthenticatedUser(UUID id, Role role) {
}
