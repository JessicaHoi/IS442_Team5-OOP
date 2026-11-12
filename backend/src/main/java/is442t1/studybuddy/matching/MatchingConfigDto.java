package is442t1.studybuddy.matching;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import is442t1.studybuddy.model.enums.MatchingStrategy;

/**
 * Body of {@code GET} and {@code PUT /api/admin/matching-config}: the ranking
 * strategy and the setting of every matching criterion.
 *
 * @param strategy the ranking strategy
 * @param criteria the settings for the matching criteria
 */
public record MatchingConfigDto(
        @NotNull(message = "Choose a matching strategy.")
        MatchingStrategy strategy,

        @NotNull(message = "Every matching criterion must be configured.")
        List<@Valid @NotNull CriterionSetting> criteria
) {

    /**
     * One criterion as shown on the admin page.
     *
     * @param key the criterion key, matching {@link MatchingCriterion#key()}
     * @param enabled whether the criterion is enabled
     * @param weight the criterion weight, from 0 to 100
     */
    public record CriterionSetting(
            @NotBlank(message = "Every matching criterion must be configured.")
            String key,

            boolean enabled,

            @Min(value = 0, message = "Weights must be whole numbers from 0 to 100.")
            @Max(value = 100, message = "Weights must be whole numbers from 0 to 100.")
            int weight
    ) {
    }
}
