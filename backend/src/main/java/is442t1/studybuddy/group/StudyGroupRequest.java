package is442t1.studybuddy.group;

import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import is442t1.studybuddy.common.web.TimeSlotDto;
import is442t1.studybuddy.model.enums.StudyGoal;
import is442t1.studybuddy.model.enums.StudyMode;

/**
 * Body of {@code POST /api/groups} and {@code PUT /api/groups/{id}}.
 * {@code courseCode} is required on create and ignored on update, because a
 * group's course cannot change.
 */
public record StudyGroupRequest(
        String courseCode,

        @NotBlank(message = "Give your group a name.")
        @Size(max = 60, message = "Group name must be at most 60 characters.")
        String name,

        @NotBlank(message = "Add a short description.")
        @Size(max = 300, message = "Description must be at most 300 characters.")
        String description,

        @NotEmpty(message = "Choose at least one study goal.")
        Set<StudyGoal> goals,

        @NotNull(message = "Choose a meeting mode.")
        StudyMode meetingMode,

        @NotEmpty(message = "Add at least one availability slot.")
        List<@Valid TimeSlotDto> availability,

        @NotNull(message = "Enter a maximum group size.")
        Integer maxSize
) {

    GroupDetails toDetails() {
        return new GroupDetails(name.trim(), description.trim(), goals, meetingMode,
                TimeSlotDto.toTimeSlots(availability), maxSize);
    }
}
