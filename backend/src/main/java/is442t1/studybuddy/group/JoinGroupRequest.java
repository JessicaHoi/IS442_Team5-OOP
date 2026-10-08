package is442t1.studybuddy.group;

import jakarta.validation.constraints.Size;

/** Body of {@code POST /api/groups/{id}/join-requests}. The message is optional. */
public record JoinGroupRequest(
        @Size(max = MembershipRequest.MESSAGE_MAX_LENGTH, message = "Message must be at most 300 characters.")
        String message
) {

    String trimmedMessage() {
        return message == null ? "" : message.trim();
    }
}
