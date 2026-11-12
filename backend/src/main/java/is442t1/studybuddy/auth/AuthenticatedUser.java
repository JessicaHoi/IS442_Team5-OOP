package is442t1.studybuddy.auth;

import java.util.UUID;

import is442t1.studybuddy.model.enums.Role;

/**
 * The signed-in caller of the current request.
 *
 * @param id the caller's account identifier
 * @param role the caller's role
 */
public record AuthenticatedUser(UUID id, Role role) {
}
