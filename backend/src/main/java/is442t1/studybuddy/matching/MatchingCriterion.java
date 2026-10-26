package is442t1.studybuddy.matching;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.ToDoubleFunction;

/**
 * The criteria the matching engine can score. Each one knows its API key
 * (shared with the front end) and how to read its 0-1 score from a breakdown.
 */
public enum MatchingCriterion {
    COURSE("course", MatchScoreBreakdown::course),
    AVAILABILITY("availability", MatchScoreBreakdown::availability),
    STUDY_MODE("studyMode", MatchScoreBreakdown::studyMode),
    STUDY_GOAL("studyGoal", MatchScoreBreakdown::studyGoal),
    GROUP_SIZE("groupSize", MatchScoreBreakdown::groupSize);

    private final String key;
    private final ToDoubleFunction<MatchScoreBreakdown> scoreReader;

    MatchingCriterion(String key, ToDoubleFunction<MatchScoreBreakdown> scoreReader) {
        this.key = key;
        this.scoreReader = scoreReader;
    }

    public String key() {
        return key;
    }

    /** This criterion's score (0 to 1) in the given breakdown. */
    public double scoreIn(MatchScoreBreakdown breakdown) {
        return scoreReader.applyAsDouble(breakdown);
    }

    public static Optional<MatchingCriterion> fromKey(String key) {
        return Arrays.stream(values()).filter(criterion -> criterion.key.equals(key)).findFirst();
    }
}
