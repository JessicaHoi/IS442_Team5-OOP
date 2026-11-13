package is442t1.studybuddy.matching;

/**
 * Component compatibility scores from 0 to 1 for course, availability, study mode, study goal and
 *     group size. MatchingRules combines them using enabled criterion weights.
 */
public record MatchScoreBreakdown(
        double course,
        double availability,
        double studyMode,
        double studyGoal,
        double groupSize
) {
}
