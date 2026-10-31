package is442t1.studybuddy.group.event;

import java.util.UUID;

/** A student became a member of a group. Published inside the transaction that accepted them. */
public record MemberJoined(UUID groupId, UUID studentId) {
}
