package is442t1.studybuddy.student;

import java.util.List;
import java.util.UUID;

import is442t1.studybuddy.connection.BuddyStatus;

/**
 * A student's profile. {@code email} is only filled in for the owner, and
 * {@code contactNumber} is null unless the viewer owns the profile or is
 * connected to the student.
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
        boolean contactVisible,
        BuddyStatus connectionStatus,
        UUID connectionId,
        List<StudyPreferenceDto> preferences
) {
}
