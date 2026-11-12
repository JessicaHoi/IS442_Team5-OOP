package is442t1.studybuddy.matching;

/**
 * Scores for the criteria used to compare two students.
 *
 * @param course the course score
 * @param availability the availability score
 * @param studyMode the study mode score
 * @param studyGoal the study goal score
 * @param groupSize the group size score
 */
public record MatchScoreBreakdown(
        double course,
        double availability,
        double studyMode,
        double studyGoal,
        double groupSize
) {
}
