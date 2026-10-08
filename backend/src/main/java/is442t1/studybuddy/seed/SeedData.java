package is442t1.studybuddy.seed;

import java.util.List;
import java.util.Set;

import is442t1.studybuddy.common.web.TimeSlotDto;
import is442t1.studybuddy.model.enums.GroupStatus;
import is442t1.studybuddy.model.enums.RequestStatus;
import is442t1.studybuddy.model.enums.StudyGoal;
import is442t1.studybuddy.model.enums.StudyMode;
import is442t1.studybuddy.student.StudyPreferenceDto;

/**
 * Shape of {@code seed/demo-data.json}. Students are referred to by a
 * {@code key} so groups and connections can point at them.
 */
record SeedData(
        List<CourseSeed> courses,
        List<AdministratorSeed> administrators,
        List<StudentSeed> students,
        List<ConnectionSeed> connections,
        List<GroupSeed> groups
) {

    record CourseSeed(String code, String name, String school) {
    }

    record AdministratorSeed(String email) {
    }

    record StudentSeed(String key, String name, String email, String school, String programme, int yearOfStudy,
            String contactNumber, List<String> courses, List<StudyPreferenceDto> preferences) {
    }

    record ConnectionSeed(String fromKey, String toKey, RequestStatus status, String message) {
    }

    record GroupSeed(String name, String description, String courseCode, String leaderKey, List<String> memberKeys,
            Set<StudyGoal> goals, StudyMode meetingMode, List<TimeSlotDto> availability, int maxSize,
            GroupStatus status, List<JoinRequestSeed> joinRequests) {
    }

    record JoinRequestSeed(String studentKey, String message) {
    }
}
