package is442t1.studybuddy.group.event;

import java.util.UUID;

/** A student stopped being a member, by leaving or being removed. Published inside the same transaction. */
public record MemberLeft(UUID groupId, UUID studentId) {
}
