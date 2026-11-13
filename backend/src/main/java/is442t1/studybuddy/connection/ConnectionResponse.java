package is442t1.studybuddy.connection;

import java.time.Instant;
import java.util.UUID;

import is442t1.studybuddy.student.StudentProfileResponse;

/**
 * Buddy request or active connection as seen by one participant.
 *
 * @param id request ID for create/accept/decline and pending lists; connection ID for the active
 *     list
 * @param status PENDING, ACCEPTED or DECLINED; persisted REJECTED is mapped to DECLINED and ACTIVE
 *     to ACCEPTED
 * @param direction INCOMING or OUTGOING relative to the caller
 * @param message request message, or null for an active-connection response
 * @param createdAt request creation time or connection start time
 * @param respondedAt request answer time, null while pending, or connection start time for an
 *     active response
 * @param other other participant, with contact details visible only when connected
 */
public record ConnectionResponse(
        UUID id,
        String status,
        String direction,
        String message,
        Instant createdAt,
        Instant respondedAt,
        StudentProfileResponse other
) {
}
