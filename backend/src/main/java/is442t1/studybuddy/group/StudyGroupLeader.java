package is442t1.studybuddy.group;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import is442t1.studybuddy.common.exception.PermissionDeniedException;
import is442t1.studybuddy.course.Course;
import is442t1.studybuddy.student.Student;

/**
 * A student who leads one or more study groups. A student becomes a leader
 * when they create their first group (see {@link StudyGroupLeaderRepository#promoteStudent}).
 * Every management operation first checks that this leader leads the group.
 */
@Entity
@Table(name = "study_group_leader")
public class StudyGroupLeader extends Student {

    private static final String NOT_LEADER = "Only the group leader can do that.";

    @OneToMany(mappedBy = "leader")
    private List<StudyGroup> ledGroups = new ArrayList<>();

    public List<StudyGroup> getLedGroups() {
        return Collections.unmodifiableList(ledGroups);
    }

    public boolean leads(StudyGroup group) {
        return group.isLedBy(getId());
    }

    public StudyGroup createGroup(Course course, GroupDetails details) {
        StudyGroup group = new StudyGroup(this, course, details);
        ledGroups.add(group);
        return group;
    }

    /** Pending join requests for a group this leader leads. */
    public List<MembershipRequest> viewJoinRequests(StudyGroup group) {
        requireLeads(group);
        return group.getPendingRequests();
    }

    public void updateGroup(StudyGroup group, GroupDetails details) {
        requireLeads(group);
        group.applyDetails(details);
    }

    public void closeGroup(StudyGroup group) {
        requireLeads(group);
        group.close();
    }

    public void acceptMember(MembershipRequest request) {
        requireLeads(request.getStudyGroup());
        request.getStudyGroup().acceptRequest(request);
    }

    public void rejectMember(MembershipRequest request) {
        requireLeads(request.getStudyGroup());
        request.getStudyGroup().rejectRequest(request);
    }

    public void removeMember(StudyGroup group, Student student) {
        requireLeads(group);
        group.removeMember(student);
    }

    private void requireLeads(StudyGroup group) {
        if (!leads(group)) {
            throw new PermissionDeniedException(NOT_LEADER);
        }
    }
}
