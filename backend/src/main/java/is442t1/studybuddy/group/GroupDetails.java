package is442t1.studybuddy.group;

import java.util.List;
import java.util.Set;

import is442t1.studybuddy.model.TimeSlot;
import is442t1.studybuddy.model.enums.StudyGoal;
import is442t1.studybuddy.model.enums.StudyMode;

/** The fields a leader can set when creating or updating a study group. */
public record GroupDetails(
        String groupName,
        String description,
        Set<StudyGoal> studyGoals,
        StudyMode studyMode,
        List<TimeSlot> availability,
        int maxSize
) {
}
