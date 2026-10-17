package is442t1.studybuddy.matching;

import java.util.List;

import is442t1.studybuddy.connection.BuddyStatus;
import is442t1.studybuddy.student.StudentProfileResponse;

public record MatchResult(
        StudentProfileResponse student,
        int score,
        MatchScoreBreakdown breakdown,
        int overlapMinutes,
        List<String> sharedCourses,
        BuddyStatus connectionStatus
) {
}
