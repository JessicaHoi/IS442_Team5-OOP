package is442t1.studybuddy.group.event;

import java.util.UUID;

/**
 * A student stopped being a member, by leaving or being removed. Published inside the same transaction.
 *
 * @param groupId the group's identifier
 * @param studentId the former member's student identifier
 */
public record MemberLeft(UUID groupId, UUID studentId) {
}
