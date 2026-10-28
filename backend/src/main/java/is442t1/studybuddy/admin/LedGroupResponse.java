package is442t1.studybuddy.admin;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import is442t1.studybuddy.group.StudyGroup;
import is442t1.studybuddy.model.enums.GroupStatus;

public record LedGroupResponse(UUID id, String name, GroupStatus status, List<Member> eligibleMembers) {

    public record Member(UUID id, String name) {
    }

    public static LedGroupResponse from(StudyGroup group) {
        List<Member> members = group.getMembers().stream()
                .filter(member -> member.isActive() && !group.isLedBy(member.getId()))
                .map(member -> new Member(member.getId(), member.getName()))
                .sorted(Comparator.comparing(Member::name).thenComparing(Member::id))
                .toList();
        return new LedGroupResponse(group.getId(), group.getGroupName(), group.getStatus(), members);
    }
}
