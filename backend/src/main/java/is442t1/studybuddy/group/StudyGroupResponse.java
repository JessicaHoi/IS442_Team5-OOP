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
 *
 * @param id the group's identifier
 * @param name the group name
 * @param description the group description
 * @param courseCode the code of the course the group studies
 * @param courseName the name of the course the group studies
 * @param goals the group's study goals
 * @param meetingMode the group's meeting mode
 * @param availability the group's proposed availability
 * @param maxSize the maximum number of members, including the leader
 * @param memberCount the current number of members
 * @param status whether the group is open or closed
 * @param createdAt when the group was created
 * @param leader the group's current leader
 * @param myRole the caller's role in the group
 * @param myRequestStatus the caller's join-request state
 * @param chatLink the Telegram link, included only in detail responses for members
 * @param members the member list in detail responses; empty for non-members and omitted from summaries
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
        String chatLink,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        List<Member> members
) {

    /**
     * The group leader shown in a group response.
     *
     * @param id the leader's student identifier
     * @param name the leader's name
     */
    public record Leader(UUID id, String name) {
    }

    /**
     * A member shown in a group detail response.
     *
     * @param id the member's student identifier
     * @param name the member's name
     * @param programme the member's programme of study
     * @param yearOfStudy the member's year of study, or {@code null} if unset
     * @param isLeader whether this member leads the group
     */
    public record Member(UUID id, String name, String programme, Integer yearOfStudy, boolean isLeader) {
    }

    /** The caller's place in the group. */
    public enum Role {
        /** The caller leads the group. */
        LEADER,
        /** The caller is a member but not the leader. */
        MEMBER,
        /** The caller is not a member of the group. */
        NONE
    }

    /** Whether the caller is waiting for an answer to a join request. */
    public enum RequestState {
        /** The caller has an unanswered request to join. */
        PENDING,
        /** The caller has no unanswered request to join. */
        NONE
    }
}
