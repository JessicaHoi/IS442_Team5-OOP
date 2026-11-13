package is442t1.studybuddy.group;

import java.time.Instant;
import java.util.UUID;

import is442t1.studybuddy.student.StudentSummary;

/**
 * Pending group join request readable by group members, containing the applicant summary, optional
 *     message and creation time.
 */
public record JoinRequestResponse(UUID id, StudentSummary student, String message, Instant createdAt) {

    public static JoinRequestResponse from(MembershipRequest request) {
        return new JoinRequestResponse(request.getId(), StudentSummary.from(request.getStudent()),
                request.getMessage(), request.getRequestDate());
    }
}
