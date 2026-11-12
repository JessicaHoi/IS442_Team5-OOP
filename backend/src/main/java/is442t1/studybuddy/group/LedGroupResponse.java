package is442t1.studybuddy.group;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import is442t1.studybuddy.model.enums.GroupStatus;

/**
 * Leadership details visible to the current group leader.
 *
 * @param id the group's identifier
 * @param name the group name
 * @param status whether the group is open or closed
 * @param eligibleMembers active group members other than the current leader
 */
public record LedGroupResponse(UUID id, String name, GroupStatus status, List<Member> eligibleMembers) {

    /**
     * A group member who can be selected as the next leader.
     *
     * @param id the student's identifier
     * @param name the student's name
     */
    public record Member(UUID id, String name) {
    }

    /**
     * Creates leadership details from a group, sorting eligible members by name and then identifier.
     *
     * @param group the group to represent
     * @return its leadership details
     */
    public static LedGroupResponse from(StudyGroup group) {
        List<Member> members = group.getMembers().stream()
                .filter(member -> member.isActive() && !group.isLedBy(member.getId()))
                .map(member -> new Member(member.getId(), member.getName()))
                .sorted(Comparator.comparing(Member::name).thenComparing(Member::id))
                .toList();
        return new LedGroupResponse(group.getId(), group.getGroupName(), group.getStatus(), members);
    }
}
