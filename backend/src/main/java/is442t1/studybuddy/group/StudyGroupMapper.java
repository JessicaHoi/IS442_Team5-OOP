package is442t1.studybuddy.group;

import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

import org.springframework.stereotype.Component;

import is442t1.studybuddy.common.web.TimeSlotDto;
import is442t1.studybuddy.student.Student;

/** Builds {@link StudyGroupResponse}s from the caller's point of view. */
@Component
public class StudyGroupMapper {

    /** Creates the mapper. */
    public StudyGroupMapper() {
    }

    /**
     * Maps a group to its summary representation without detail-only fields.
     *
     * @param group the group to map
     * @param viewerId the caller's identifier
     * @return the summary visible to the caller
     */
    public StudyGroupResponse toSummary(StudyGroup group, UUID viewerId) {
        return toResponse(group, viewerId, null, null);
    }

    /**
     * Maps a group to its detail representation. Only members see the member list
     * and chat link; other callers receive an empty member list and no chat link.
     *
     * @param group the group to map
     * @param viewerId the caller's identifier
     * @return the detail visible to the caller
     */
    public StudyGroupResponse toDetail(StudyGroup group, UUID viewerId) {
        List<StudyGroupResponse.Member> members = group.hasMember(viewerId) ? membersOf(group) : List.of();
        return toResponse(group, viewerId, members, group.hasMember(viewerId) ? group.getChatLink() : null);
    }

    /**
     * Determines the caller's role in a group.
     *
     * @param group the group to check
     * @param viewerId the caller's identifier
     * @return the caller's role
     */
    public StudyGroupResponse.Role roleOf(StudyGroup group, UUID viewerId) {
        if (group.isLedBy(viewerId)) {
            return StudyGroupResponse.Role.LEADER;
        }
        return group.hasMember(viewerId) ? StudyGroupResponse.Role.MEMBER : StudyGroupResponse.Role.NONE;
    }

    private StudyGroupResponse toResponse(StudyGroup group, UUID viewerId, List<StudyGroupResponse.Member> members,
            String chatLink) {
        StudyGroupLeader leader = group.getLeader();
        return new StudyGroupResponse(
                group.getId(),
                group.getGroupName(),
                group.getDescription(),
                group.getCourse().getCourseCode(),
                group.getCourse().getCourseName(),
                // A copy: the response is written after the transaction, when lazy collections are closed.
                new TreeSet<>(group.getStudyGoals()),
                group.getStudyMode(),
                TimeSlotDto.fromTimeSlots(group.getAvailability()),
                group.getMaxSize(),
                group.getMemberCount(),
                group.getStatus(),
                group.getCreatedAt(),
                new StudyGroupResponse.Leader(leader.getId(), leader.getName()),
                roleOf(group, viewerId),
                group.hasPendingRequestFrom(viewerId)
                        ? StudyGroupResponse.RequestState.PENDING
                        : StudyGroupResponse.RequestState.NONE,
                chatLink,
                members);
    }

    /**
     * Maps a group's members, sorting the leader first and the other members by name.
     *
     * @param group the group whose members to map
     * @return the ordered member representations
     */
    public List<StudyGroupResponse.Member> membersOf(StudyGroup group) {
        return group.getMembers().stream()
                .sorted(Comparator.comparing((Student member) -> !group.isLedBy(member.getId()))
                        .thenComparing(Student::getName))
                .map(member -> new StudyGroupResponse.Member(member.getId(), member.getName(), member.getProgram(),
                        member.getYearOfStudy(), group.isLedBy(member.getId())))
                .toList();
    }
}
