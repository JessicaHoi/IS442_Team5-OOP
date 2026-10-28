package is442t1.studybuddy.group;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record TransferLeadershipRequest(
        @NotNull(message = "Choose a new group leader.") UUID studentId
) {
}
