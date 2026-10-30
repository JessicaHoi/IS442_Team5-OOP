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

    public StudyGroupResponse toSummary(StudyGroup group, UUID viewerId) {
        return toResponse(group, viewerId, null, null);
    }

    /** Detail view. Only the leader and members see the member list. */
    public StudyGroupResponse toDetail(StudyGroup group, UUID viewerId) {
        List<StudyGroupResponse.Member> members = group.hasMember(viewerId) ? membersOf(group) : List.of();
        return toResponse(group, viewerId, members, group.hasMember(viewerId) ? group.getChatLink() : null);
    }

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

    /** Leader first, then the other members by name. */
    private List<StudyGroupResponse.Member> membersOf(StudyGroup group) {
        return group.getMembers().stream()
                .sorted(Comparator.comparing((Student member) -> !group.isLedBy(member.getId()))
                        .thenComparing(Student::getName))
                .map(member -> new StudyGroupResponse.Member(member.getId(), member.getName(), member.getProgram(),
                        member.getYearOfStudy(), group.isLedBy(member.getId())))
                .toList();
    }
}
