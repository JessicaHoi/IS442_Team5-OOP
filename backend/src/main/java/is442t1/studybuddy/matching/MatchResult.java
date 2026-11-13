package is442t1.studybuddy.matching;

import java.util.List;

import is442t1.studybuddy.connection.BuddyStatus;
import is442t1.studybuddy.student.StudentProfileResponse;

/**
 * Ranked candidate response with the privacy-filtered student profile, overall 0-100 score,
 *     component scores, overlap minutes, shared courses and viewer relationship.
 */
public record MatchResult(
        StudentProfileResponse student,
        int score,
        MatchScoreBreakdown breakdown,
        int overlapMinutes,
        List<String> sharedCourses,
        BuddyStatus connectionStatus
) {
}
