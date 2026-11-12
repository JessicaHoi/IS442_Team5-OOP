package is442t1.studybuddy.connection;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A request to connect with another student.
 *
 * @param toStudentId the recipient's student identifier
 * @param message an optional message to the recipient
 */
public record CreateMatchRequest(
        @NotNull(message = "Choose a student to send a request to.")
        UUID toStudentId,

        @Size(max = 300, message = "Message is too long.")
        String message
) {
    /**
     * Trims the optional message.
     *
     * @return the trimmed message, or {@code null} if none was supplied
     */
    public String trimmedMessage() {
        return message == null ? null : message.trim();
    }
}
