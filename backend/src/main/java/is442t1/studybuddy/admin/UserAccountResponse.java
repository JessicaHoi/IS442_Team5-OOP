package is442t1.studybuddy.admin;

import java.time.Instant;
import java.util.UUID;

import is442t1.studybuddy.model.User;
import is442t1.studybuddy.model.enums.AccountStatus;
import is442t1.studybuddy.model.enums.Role;

public record UserAccountResponse(
        UUID id,
        String name,
        String email,
        Role role,
        AccountStatus status,
        Instant lastLogin,
        Instant createdAt,
        long connectionCount,
        long groupCount
) {
    public static UserAccountResponse from(User user, long connectionCount, long groupCount) {
        return new UserAccountResponse(user.getId(), user.getDisplayName(), user.getEmail(), user.getRole(),
                user.getStatus(), user.getLastLogin(), user.getCreatedAt(), connectionCount, groupCount);
    }
}
