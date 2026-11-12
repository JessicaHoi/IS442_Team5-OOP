package is442t1.studybuddy.student;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import is442t1.studybuddy.common.web.TimeSlotDto;
import is442t1.studybuddy.model.enums.GroupPreference;
import is442t1.studybuddy.model.enums.StudyGoal;
import is442t1.studybuddy.model.enums.StudyMode;

/**
 * Study preference for one course, in both requests and responses.
 * {@code meetingMode} is the in-person/online choice and {@code groupFormat}
 * the one-to-one/small-group choice (the brief calls both "study mode").
 *
 * @param course the course code
 * @param meetingMode the preferred meeting mode
 * @param groupFormat the preferred group format
 * @param goals the study goals
 * @param availability the weekly availability slots
 */
public record StudyPreferenceDto(
        @NotBlank(message = "Choose the course you need a study buddy for.")
        String course,

        @NotNull(message = "Choose a meeting mode.")
        StudyMode meetingMode,

        @NotNull(message = "Choose a group format.")
        GroupPreference groupFormat,

        @NotEmpty(message = "Choose at least one study goal.")
        Set<StudyGoal> goals,

        @NotEmpty(message = "Add at least one availability slot.")
        List<@Valid TimeSlotDto> availability
) {

    /**
     * Converts a preference and copies its collections for use after the transaction.
     *
     * @param preference the stored preference
     * @return the API representation
     */
    public static StudyPreferenceDto from(StudyPreference preference) {
        return new StudyPreferenceDto(
                preference.getCourse().getCourseCode(),
                preference.getStudyMode(),
                preference.getGroupPreference(),
                new TreeSet<>(preference.getStudyGoals()),
                TimeSlotDto.fromTimeSlots(preference.getAvailability()));
    }
}
