package is442t1.studybuddy.student;

import java.util.UUID;

/**
 * Compact student reference used in member and join-request lists.
 *
 * @param id the student identifier
 * @param name the student's name
 * @param programme the student's programme
 * @param yearOfStudy the year of study
 */
public record StudentSummary(UUID id, String name, String programme, Integer yearOfStudy) {

    /**
     * Converts a student to a summary.
     *
     * @param student the student
     * @return the student summary
     */
    public static StudentSummary from(Student student) {
        return new StudentSummary(student.getId(), student.getName(), student.getProgram(), student.getYearOfStudy());
    }
}
