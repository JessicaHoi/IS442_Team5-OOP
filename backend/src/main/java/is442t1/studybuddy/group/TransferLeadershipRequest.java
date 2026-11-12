package is442t1.studybuddy.group;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * A request to transfer group leadership.
 *
 * @param studentId the student selected as the new leader
 */
public record TransferLeadershipRequest(
        @NotNull(message = "Choose a new group leader.") UUID studentId
) {
}
