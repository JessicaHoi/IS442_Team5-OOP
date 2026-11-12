package is442t1.studybuddy.group;

import java.util.List;
import java.util.Set;

import is442t1.studybuddy.model.TimeSlot;
import is442t1.studybuddy.model.enums.StudyGoal;
import is442t1.studybuddy.model.enums.StudyMode;

/**
 * The fields a leader can set when creating or updating a study group.
 *
 * @param groupName the group's display name
 * @param description the group's description
 * @param studyGoals the goals selected for the group
 * @param studyMode the group's meeting mode
 * @param availability the group's proposed time slots
 * @param maxSize the maximum number of members, including the leader
 */
public record GroupDetails(
        String groupName,
        String description,
        Set<StudyGoal> studyGoals,
        StudyMode studyMode,
        List<TimeSlot> availability,
        int maxSize
) {
}
