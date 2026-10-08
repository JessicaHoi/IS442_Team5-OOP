package is442t1.studybuddy.student;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Body of {@code PUT /api/students/me/preferences}: the student's complete
 * list of preferences, at most one per course. Courses missing from the list
 * lose their preference.
 */
public record UpdatePreferencesRequest(
        @NotNull(message = "Send the list of study preferences.")
        List<@Valid StudyPreferenceDto> preferences
) {
}
