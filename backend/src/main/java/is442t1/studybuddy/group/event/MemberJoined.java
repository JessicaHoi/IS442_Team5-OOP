package is442t1.studybuddy.group.event;

import java.util.UUID;

/**
 * A student became a member of a group. Published inside the transaction that accepted them.
 *
 * @param groupId the group's identifier
 * @param studentId the new member's student identifier
 */
public record MemberJoined(UUID groupId, UUID studentId) {
}
