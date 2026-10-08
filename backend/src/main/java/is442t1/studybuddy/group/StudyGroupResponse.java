package is442t1.studybuddy.group;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import is442t1.studybuddy.common.web.TimeSlotDto;
import is442t1.studybuddy.model.enums.GroupStatus;
import is442t1.studybuddy.model.enums.StudyGoal;
import is442t1.studybuddy.model.enums.StudyMode;

/**
 * A study group as seen by the caller. List endpoints leave {@code members}
 * out. The detail endpoint fills it for members and sends an empty list to
 * everyone else.
 */
public record StudyGroupResponse(
        UUID id,
        String name,
        String description,
        String courseCode,
        String courseName,
        Set<StudyGoal> goals,
        StudyMode meetingMode,
        List<TimeSlotDto> availability,
        int maxSize,
        int memberCount,
        GroupStatus status,
        Instant createdAt,
        Leader leader,
        Role myRole,
        RequestState myRequestStatus,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        List<Member> members
) {

    public record Leader(UUID id, String name) {
    }

    public record Member(UUID id, String name, String programme, Integer yearOfStudy, boolean isLeader) {
    }

    /** The caller's place in the group. */
    public enum Role {
        LEADER,
        MEMBER,
        NONE
    }

    /** Whether the caller is waiting for an answer to a join request. */
    public enum RequestState {
        PENDING,
        NONE
    }
}
