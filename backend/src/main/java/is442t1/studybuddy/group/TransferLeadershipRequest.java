package is442t1.studybuddy.group;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * Leadership-transfer payload containing the student ID of another active member of the group.
 */
public record TransferLeadershipRequest(
        @NotNull(message = "Choose a new group leader.") UUID studentId
) {
}
