package is442t1.studybuddy.student;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import is442t1.studybuddy.connection.BuddyState;
import is442t1.studybuddy.course.Course;

/** Builds profile responses and applies the contact-number privacy rule. */
@Component
public class StudentProfileMapper {

    /**
     * Builds the signed-in student's profile, including their email and contact details.
     *
     * @param student the student whose profile is being viewed
     * @return the profile response with contact details visible
     */
    public StudentProfileResponse toOwnProfile(Student student) {
        return toResponse(student, student.getEmail(), true, BuddyState.NONE);
    }

    /**
     * Builds another student's profile without exposing their email or contact details.
     *
     * @param student the student whose profile is being viewed
     * @param buddyState the signed-in student's connection state with this student
     * @return the public profile response with connection information
     */
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
                contactVisible ? student.getTelegramHandle() : null,
                contactVisible,
                buddyState.status(),
                buddyState.connectionId(),
                preferences);
    }
}
