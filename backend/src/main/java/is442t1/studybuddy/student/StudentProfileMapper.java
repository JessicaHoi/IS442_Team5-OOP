package is442t1.studybuddy.student;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import is442t1.studybuddy.connection.BuddyState;
import is442t1.studybuddy.course.Course;

/** Builds profile responses and applies the contact-number privacy rule. */
@Component
public class StudentProfileMapper {

    public StudentProfileResponse toOwnProfile(Student student) {
        return toResponse(student, student.getEmail(), true, BuddyState.NONE);
    }

    public StudentProfileResponse toPublicProfile(Student student, BuddyState buddyState) {
        return toResponse(student, null, buddyState.isConnected(), buddyState);
    }

    private StudentProfileResponse toResponse(Student student, String email, boolean contactVisible,
            BuddyState buddyState) {
        List<String> courses = student.getCoursesTaken().stream().map(Course::getCourseCode).sorted().toList();
        List<StudyPreferenceDto> preferences = student.getStudyPreferences().stream()
                .map(StudyPreferenceDto::from)
                .sorted(Comparator.comparing(StudyPreferenceDto::course))
                .toList();
        return new StudentProfileResponse(
                student.getId(),
                student.getName(),
                email,
                student.getSchool(),
                student.getProgram(),
                student.getYearOfStudy(),
                courses,
                contactVisible ? student.getContactNum() : null,
                contactVisible,
                buddyState.status(),
                buddyState.connectionId(),
                preferences);
    }
}
