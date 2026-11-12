package is442t1.studybuddy.connection;

import java.time.Instant;
import java.util.UUID;

import is442t1.studybuddy.student.StudentProfileResponse;

/**
 * A connection or connection request as seen by the current student.
 *
 * @param id the connection or request identifier
 * @param status the connection or request status
 * @param direction the request direction relative to the viewer
 * @param message the request message
 * @param createdAt the creation time
 * @param respondedAt the response time, if answered
 * @param other the other student's profile
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
