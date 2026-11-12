package is442t1.studybuddy.group.event;

import java.util.UUID;

/**
 * A study group was closed. Published inside the transaction that closed it.
 *
 * @param groupId the closed group's identifier
 */
public record GroupClosed(UUID groupId) {
}
