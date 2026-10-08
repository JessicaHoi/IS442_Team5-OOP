package is442t1.studybuddy.student;

import java.util.UUID;

/** Compact student reference used in member and join-request lists. */
public record StudentSummary(UUID id, String name, String programme, Integer yearOfStudy) {

    public static StudentSummary from(Student student) {
        return new StudentSummary(student.getId(), student.getName(), student.getProgram(), student.getYearOfStudy());
    }
}
