package is442t1.studybuddy.connection;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMatchRequest(
        @NotNull(message = "Choose a student to send a request to.")
        UUID toStudentId,

        @Size(max = 300, message = "Message is too long.")
        String message
) {
    public String trimmedMessage() {
        return message == null ? null : message.trim();
    }
}
