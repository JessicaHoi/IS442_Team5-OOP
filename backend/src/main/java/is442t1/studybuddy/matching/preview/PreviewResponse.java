package is442t1.studybuddy.matching.preview;

import java.util.List;
import java.util.UUID;

import is442t1.studybuddy.matching.MatchScoreBreakdown;
import is442t1.studybuddy.model.enums.MatchingStrategy;

/**
 * The same student ranked under the saved settings and under the proposed ones.
 *
 * @param studentName the sample student
 * @param course      the course the ranking was made for, if any
 * @param current     ranking with the saved settings
 * @param proposed    ranking with the unsaved settings
 */
public record PreviewResponse(String studentName, String course, Ranking current, Ranking proposed) {

    /** One side of the comparison: the strategy used and the top matches. */
    public record Ranking(MatchingStrategy strategy, List<Entry> results) {
    }

    /**
     * One ranked match.
     *
     * @param rank          position in this ranking, starting at 1
     * @param otherRank     position of the same student in the other ranking (null if absent there)
     * @param studentId     the matched student
     * @param name          their name
     * @param score         overall score from 0 to 100
     * @param overlapMinutes shared weekly availability
     * @param breakdown     the 0-1 score for each criterion
     */
    public record Entry(int rank, Integer otherRank, UUID studentId, String name, int score, int overlapMinutes,
            MatchScoreBreakdown breakdown) {
    }
}
