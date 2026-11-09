package is442t1.studybuddy.admin;

import java.util.List;
import java.util.UUID;

import is442t1.studybuddy.group.StudyGroupResponse;
import is442t1.studybuddy.model.enums.GroupStatus;

/** Group membership information for the administrator's account view. */
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
