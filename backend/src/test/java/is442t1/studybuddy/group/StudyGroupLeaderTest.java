package is442t1.studybuddy.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import is442t1.studybuddy.common.exception.BusinessRuleException;
import is442t1.studybuddy.common.exception.PermissionDeniedException;
import is442t1.studybuddy.course.Course;
import is442t1.studybuddy.model.TimeSlot;
import is442t1.studybuddy.model.enums.GroupStatus;
import is442t1.studybuddy.model.enums.RequestStatus;
import is442t1.studybuddy.model.enums.StudyGoal;
import is442t1.studybuddy.model.enums.StudyMode;
import is442t1.studybuddy.student.Student;

/** Domain rules for study groups, tested without Spring or a database. */
class StudyGroupLeaderTest {

    private static final List<TimeSlot> WEDNESDAY_EVENING =
            List.of(new TimeSlot(DayOfWeek.WEDNESDAY, LocalTime.of(19, 0), LocalTime.of(21, 0)));

    private StudyGroupLeader leader;
    private StudyGroup group;

    @BeforeEach
    void setUp() {
        leader = withId(new StudyGroupLeader(), "Leader");
        group = leader.createGroup(new Course(), details(3));
    }

    @Test
    void createGroupMakesTheLeaderTheFirstMember() {
        assertThat(group.isOpen()).isTrue();
        assertThat(group.getMemberCount()).isEqualTo(1);
        assertThat(group.isLedBy(leader.getId())).isTrue();
        assertThat(leader.getLedGroups()).containsExactly(group);
    }

    @Test
    void acceptingARequestAddsTheMember() {
        Student student = student("Ben");
        MembershipRequest request = group.requestToJoin(student, "Hi");

        leader.acceptMember(request);

        assertThat(group.hasMember(student.getId())).isTrue();
        assertThat(request.getStatus()).isEqualTo(RequestStatus.ACCEPTED);
        assertThat(leader.viewJoinRequests(group)).isEmpty();
    }

    @Test
    void cannotAcceptWhenTheGroupIsFull() {
        leader.acceptMember(group.requestToJoin(student("Ben"), ""));
        MembershipRequest pending = group.requestToJoin(student("Cara"), "");
        leader.acceptMember(group.requestToJoin(student("Dan"), ""));

        assertThatThrownBy(() -> leader.acceptMember(pending))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("full");
    }

    @Test
    void rejectsDuplicateAndMemberJoinRequests() {
        Student student = student("Ben");
        group.requestToJoin(student, "");

        assertThatThrownBy(() -> group.requestToJoin(student, ""))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("pending");
        assertThatThrownBy(() -> group.requestToJoin(leader, ""))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already in this group");
    }

    @Test
    void anotherLeaderCannotManageTheGroup() {
        StudyGroupLeader otherLeader = withId(new StudyGroupLeader(), "Other");
        MembershipRequest request = group.requestToJoin(student("Ben"), "");

        assertThatThrownBy(() -> otherLeader.updateGroup(group, details(5))).isInstanceOf(PermissionDeniedException.class);
        assertThatThrownBy(() -> otherLeader.closeGroup(group)).isInstanceOf(PermissionDeniedException.class);
        assertThatThrownBy(() -> otherLeader.acceptMember(request)).isInstanceOf(PermissionDeniedException.class);
        assertThatThrownBy(() -> otherLeader.rejectMember(request)).isInstanceOf(PermissionDeniedException.class);
        assertThatThrownBy(() -> otherLeader.viewJoinRequests(group)).isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void maximumSizeCannotDropBelowTheMemberCount() {
        leader.acceptMember(group.requestToJoin(student("Ben"), ""));
        leader.acceptMember(group.requestToJoin(student("Cara"), ""));

        assertThatThrownBy(() -> leader.updateGroup(group, details(2)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("3 members");
    }

    @Test
    void updateReplacesTheGroupDetails() {
        GroupDetails updated = new GroupDetails("Renamed", "New", Set.of(StudyGoal.CONCEPT_REVIEW), StudyMode.ONLINE,
                WEDNESDAY_EVENING, 6);

        leader.updateGroup(group, updated);

        assertThat(group.getGroupName()).isEqualTo("Renamed");
        assertThat(group.getStudyGoals()).containsExactly(StudyGoal.CONCEPT_REVIEW);
        assertThat(group.getStudyMode()).isEqualTo(StudyMode.ONLINE);
        assertThat(group.getMaxSize()).isEqualTo(6);
    }

    @Test
    void closingRejectsPendingRequestsAndFreezesTheGroup() {
        MembershipRequest request = group.requestToJoin(student("Ben"), "");

        leader.closeGroup(group);

        assertThat(group.getStatus()).isEqualTo(GroupStatus.CLOSED);
        assertThat(request.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThatThrownBy(() -> leader.updateGroup(group, details(5))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> group.requestToJoin(student("Cara"), "")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void leaderCanRemoveMembersButNotThemselves() {
        Student member = student("Ben");
        leader.acceptMember(group.requestToJoin(member, ""));

        leader.removeMember(group, member);

        assertThat(group.hasMember(member.getId())).isFalse();
        assertThatThrownBy(() -> leader.removeMember(group, leader))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("leader cannot be removed");
        assertThatThrownBy(() -> leader.removeMember(group, student("Stranger")))
                .isInstanceOf(BusinessRuleException.class);
    }

    private static GroupDetails details(int maxSize) {
        return new GroupDetails("IS442 Crew", "Exam prep", Set.of(StudyGoal.EXAM_PREPARATION), StudyMode.IN_PERSON,
                WEDNESDAY_EVENING, maxSize);
    }

    private static Student student(String name) {
        return withId(new Student(), name);
    }

    private static <T extends Student> T withId(T student, String name) {
        student.setId(UUID.randomUUID());
        student.setName(name);
        return student;
    }
}
