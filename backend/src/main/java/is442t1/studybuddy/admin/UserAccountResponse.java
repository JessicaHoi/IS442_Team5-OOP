package is442t1.studybuddy.admin;

import java.time.Instant;
import java.util.UUID;

import is442t1.studybuddy.model.User;
import is442t1.studybuddy.model.enums.AccountStatus;
import is442t1.studybuddy.model.enums.Role;

/**
 * Account summary for administrators, including account status, login and creation timestamps, and
 *     connection, group and leadership counts.
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
    public static UserAccountResponse from(User user, long connectionCount, long groupCount, long ledGroupCount) {
        return new UserAccountResponse(user.getId(), user.getDisplayName(), user.getEmail(), user.getRole(),
                user.getStatus(), user.getLastLogin(), user.getCreatedAt(), connectionCount, groupCount, ledGroupCount);
    }
}
