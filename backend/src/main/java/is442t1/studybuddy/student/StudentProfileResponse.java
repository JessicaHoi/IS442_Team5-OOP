package is442t1.studybuddy.student;

import java.util.List;
import java.util.UUID;

import is442t1.studybuddy.connection.BuddyStatus;

/**
 * A student's profile. {@code email} is only filled in for the owner, and
 * {@code contactNumber} and {@code telegramHandle} are null unless the viewer owns the profile or is
 * connected to the student.
 *
 * @param id the student's identifier
 * @param name the student's name
 * @param email the account email, visible only to the owner
 * @param school the student's school
 * @param programme the student's programme
 * @param yearOfStudy the year of study
 * @param courses the student's course codes
 * @param contactNumber the phone number, when visible to the viewer
 * @param telegramHandle the Telegram handle, when visible to the viewer
 * @param contactVisible whether contact information is visible
 * @param connectionStatus the viewer's connection status with the student
 * @param connectionId the connection or request identifier, if any
 * @param preferences the student's study preferences
 */
public record StudentProfileResponse(
        UUID id,
        String name,
        String email,
        String school,
        String programme,
        Integer yearOfStudy,
        List<String> courses,
        String contactNumber,
        String telegramHandle,
        boolean contactVisible,
        BuddyStatus connectionStatus,
        UUID connectionId,
        List<StudyPreferenceDto> preferences
) {
}
