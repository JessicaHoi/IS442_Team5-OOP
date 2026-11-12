package is442t1.studybuddy.matching;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.ToDoubleFunction;

/**
 * The criteria the matching engine can score. Each one knows its API key
 * (shared with the front end) and how to read its 0-1 score from a breakdown.
 */
public enum MatchingCriterion {
    /** Course overlap. */
    COURSE("course", MatchScoreBreakdown::course),
    /** Availability overlap. */
    AVAILABILITY("availability", MatchScoreBreakdown::availability),
    /** Meeting-mode compatibility. */
    STUDY_MODE("studyMode", MatchScoreBreakdown::studyMode),
    /** Study-goal compatibility. */
    STUDY_GOAL("studyGoal", MatchScoreBreakdown::studyGoal),
    /** Group-format compatibility. */
    GROUP_SIZE("groupSize", MatchScoreBreakdown::groupSize);

    private final String key;
    private final ToDoubleFunction<MatchScoreBreakdown> scoreReader;

    MatchingCriterion(String key, ToDoubleFunction<MatchScoreBreakdown> scoreReader) {
        this.key = key;
        this.scoreReader = scoreReader;
    }

    /**
     * Returns the API key for this criterion.
     *
     * @return the API key for this criterion
     */
    public String key() {
        return key;
    }

    /**
     * Reads this criterion's score from a breakdown.
     *
     * @param breakdown the score breakdown
     * @return the criterion score from 0 to 1
     */
    public double scoreIn(MatchScoreBreakdown breakdown) {
        return scoreReader.applyAsDouble(breakdown);
    }

    /**
     * Finds a criterion by its API key.
     *
     * @param key the API key
     * @return the criterion, if recognized
     */
    public static Optional<MatchingCriterion> fromKey(String key) {
        return Arrays.stream(values()).filter(criterion -> criterion.key.equals(key)).findFirst();
    }
}
