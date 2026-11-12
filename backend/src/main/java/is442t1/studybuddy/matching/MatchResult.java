package is442t1.studybuddy.matching;

import java.util.List;

import is442t1.studybuddy.connection.BuddyStatus;
import is442t1.studybuddy.student.StudentProfileResponse;

/**
 * A candidate and the details of their match with the requesting student.
 *
 * @param student the candidate's profile
 * @param score the overall match score
 * @param breakdown the individual criterion scores
 * @param overlapMinutes the minutes of shared weekly availability
 * @param sharedCourses the shared course codes
 * @param connectionStatus the current buddy connection status
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
