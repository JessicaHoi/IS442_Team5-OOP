package is442t1.studybuddy.group;

import java.time.Instant;
import java.util.UUID;

import is442t1.studybuddy.student.StudentSummary;

/**
 * A request to join a study group.
 *
 * @param id the request identifier
 * @param student the student requesting membership
 * @param message the student's message
 * @param createdAt the request time
 */
public record JoinRequestResponse(UUID id, StudentSummary student, String message, Instant createdAt) {

    /**
     * Converts a membership request to its API representation.
     *
     * @param request the membership request
     * @return the request response
     */
    public static JoinRequestResponse from(MembershipRequest request) {
        return new JoinRequestResponse(request.getId(), StudentSummary.from(request.getStudent()),
                request.getMessage(), request.getRequestDate());
    }
}
