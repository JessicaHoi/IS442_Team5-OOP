package is442t1.studybuddy.matching;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

import is442t1.studybuddy.model.enums.MatchingStrategy;

/**
 * Body of {@code GET} and {@code PUT /api/admin/matching-config}: the ranking
 * strategy and the setting of every matching criterion.
 */
public record MatchingConfigDto(
        @NotNull(message = "Choose a matching strategy.")
        MatchingStrategy strategy,

        @NotNull(message = "Every matching criterion must be configured.")
        List<@Valid @NotNull CriterionSetting> criteria
) {

    /** One criterion as shown on the admin page. {@code key} matches {@link MatchingCriterion#key()}. */
    @JsonDeserialize(using = CriterionSettingDeserializer.class)
    public record CriterionSetting(
            @NotBlank(message = "Every matching criterion must be configured.")
            String key,

            boolean enabled,

            @Min(value = 0, message = "Weights must be whole numbers from 0 to 100.")
            @Max(value = 100, message = "Weights must be whole numbers from 0 to 100.")
            int weight
    ) {
    }

    /** Keep this input's JSON types strict without changing coercion for other API payloads. */
    public static final class CriterionSettingDeserializer extends ValueDeserializer<CriterionSetting> {
        @Override
        public CriterionSetting deserialize(JsonParser parser, DeserializationContext context) {
            JsonNode node = context.readTree(parser);
            if (!node.isObject()) {
                return context.reportInputMismatch(CriterionSetting.class, "Send a matching criterion object.");
            }
            JsonNode enabled = node.get("enabled");
            if (enabled == null || !enabled.isBoolean()) {
                return context.reportInputMismatch(CriterionSetting.class, "Choose whether each criterion is enabled.");
            }
            JsonNode weight = node.get("weight");
            // JSON 30.0 and 3e1 are whole numbers too, as in JavaScript's Number.isInteger.
            if (weight == null || !weight.isNumber() || !weight.canConvertToExactIntegral() || !weight.canConvertToInt()) {
                return context.reportInputMismatch(CriterionSetting.class, "Weights must be whole numbers from 0 to 100.");
            }
            JsonNode key = node.get("key");
            return new CriterionSetting(key == null ? null : context.readTreeAsValue(key, String.class),
                    enabled.booleanValue(), weight.intValue());
        }
    }
}
