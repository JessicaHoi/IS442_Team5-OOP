package is442t1.studybuddy.admin;

import java.util.List;
import java.util.UUID;

import is442t1.studybuddy.group.StudyGroupResponse;
import is442t1.studybuddy.model.enums.GroupStatus;

/**
 * Group membership information for the administrator's account view.
 *
 * @param id the group identifier
 * @param name the group name
 * @param courseCode the group's course code
 * @param courseName the group's course name
 * @param status the group's status
 * @param memberCount the number of members
 * @param maxSize the maximum group size
 * @param members the group members
 */
public record UserGroupResponse(
        UUID id,
        String name,
        String courseCode,
        String courseName,
        GroupStatus status,
        int memberCount,
        int maxSize,
        List<StudyGroupResponse.Member> members
) {
}
