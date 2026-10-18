package is442t1.studybuddy.connection;

import java.time.Instant;
import java.util.UUID;

import is442t1.studybuddy.student.StudentProfileResponse;

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
