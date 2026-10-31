package is442t1.studybuddy.group.event;

import java.util.UUID;

/** A group's name or description changed. Published inside the same transaction. */
public record GroupRenamed(UUID groupId) {
}
