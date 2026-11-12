package is442t1.studybuddy.group.event;

import java.util.UUID;

/**
 * A group has a new leader, whether a leader or an administrator made the
 * change. Published inside the same transaction.
 *
 * @param groupId the group's identifier
 * @param newLeaderId the new leader's student identifier
 */
public record LeadershipTransferred(UUID groupId, UUID newLeaderId) {
}
