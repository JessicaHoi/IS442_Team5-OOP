package is442t1.studybuddy.matching.preview;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import is442t1.studybuddy.matching.MatchingConfigDto;

/**
 * Body of {@code POST /api/admin/matching-config/preview}.
 *
 * @param studentId the sample student whose matches are ranked
 * @param course    course to match for; blank uses the student's first study preference
 * @param proposed  the unsaved settings to compare with the saved ones
 */
public record PreviewRequest(
        @NotNull(message = "Choose a sample student.")
        UUID studentId,

        String course,

        @NotNull(message = "Send the settings to preview.")
        @Valid
        MatchingConfigDto proposed
) {
}
