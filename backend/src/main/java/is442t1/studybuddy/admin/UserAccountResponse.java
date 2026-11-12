package is442t1.studybuddy.admin;

import java.time.Instant;
import java.util.UUID;

import is442t1.studybuddy.model.User;
import is442t1.studybuddy.model.enums.AccountStatus;
import is442t1.studybuddy.model.enums.Role;

/**
 * An account and usage counts for the administrator's account view.
 *
 * @param id the account identifier
 * @param name the display name
 * @param email the account email
 * @param role the account role
 * @param status the account status
 * @param lastLogin the last successful login time, if any
 * @param createdAt the account creation time
 * @param connectionCount the number of connections
 * @param groupCount the number of group memberships
 * @param ledGroupCount the number of groups led
 */
public record UserAccountResponse(
        UUID id,
        String name,
        String email,
        Role role,
        AccountStatus status,
        Instant lastLogin,
        Instant createdAt,
        long connectionCount,
        long groupCount,
        long ledGroupCount
) {
    /**
     * Builds an account response with the supplied usage counts.
     *
     * @param user the account
     * @param connectionCount the number of connections
     * @param groupCount the number of group memberships
     * @param ledGroupCount the number of groups led
     * @return the account response
     */
    public static UserAccountResponse from(User user, long connectionCount, long groupCount, long ledGroupCount) {
        return new UserAccountResponse(user.getId(), user.getDisplayName(), user.getEmail(), user.getRole(),
                user.getStatus(), user.getLastLogin(), user.getCreatedAt(), connectionCount, groupCount, ledGroupCount);
    }
}
