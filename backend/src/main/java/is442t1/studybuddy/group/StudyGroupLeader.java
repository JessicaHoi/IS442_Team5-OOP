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

    static final String NOT_LEADER = "Only the group leader can do that.";

    @OneToMany(mappedBy = "leader")
    private List<StudyGroup> ledGroups = new ArrayList<>();

    /**
     * Creates a leader entity instance.
     *
     * <p>Required by JPA when loading a persisted leader.</p>
     */
    public StudyGroupLeader() {
    }

    /**
     * Gets an unmodifiable view of the groups currently led by this student.
     *
     * @return the groups currently led by this student
     */
    public List<StudyGroup> getLedGroups() {
        return Collections.unmodifiableList(ledGroups);
    }

    /**
     * Checks whether this student leads a group.
     *
     * @param group the group to check
     * @return {@code true} if this student is its current leader
     */
    public boolean leads(StudyGroup group) {
        return group.isLedBy(getId());
    }

    /**
     * Creates and starts leading a group for the supplied course and details.
     *
     * @param course the course the group will study
     * @param details the initial group settings
     * @return the new group
     */
    public StudyGroup createGroup(Course course, GroupDetails details) {
        StudyGroup group = new StudyGroup(this, course, details);
        ledGroups.add(group);
        return group;
    }

    /**
     * Gets pending join requests for a group this leader leads.
     *
     * @param group the group whose requests to view
     * @return the group's pending requests
     * @throws PermissionDeniedException if this student does not lead the group
     */
    public List<MembershipRequest> viewJoinRequests(StudyGroup group) {
        requireLeads(group);
        return group.getPendingRequests();
    }

    /**
     * Replaces a group's editable settings.
     *
     * @param group the group to update
     * @param details the replacement settings
     * @throws PermissionDeniedException if this student does not lead the group
     */
    public void updateGroup(StudyGroup group, GroupDetails details) {
        requireLeads(group);
        group.applyDetails(details);
    }

    /**
     * Closes a group and rejects its pending join requests.
     *
     * @param group the group to close
     * @throws PermissionDeniedException if this student does not lead the group
     */
    public void closeGroup(StudyGroup group) {
        requireLeads(group);
        group.close();
    }

    /**
     * Accepts a pending request to join a group this student leads.
     *
     * @param request the pending request to accept
     * @throws PermissionDeniedException if this student does not lead the request's group
     */
    public void acceptMember(MembershipRequest request) {
        requireLeads(request.getStudyGroup());
        request.getStudyGroup().acceptRequest(request);
    }

    /**
     * Rejects a pending request to join a group this student leads.
     *
     * @param request the pending request to reject
     * @throws PermissionDeniedException if this student does not lead the request's group
     */
    public void rejectMember(MembershipRequest request) {
        requireLeads(request.getStudyGroup());
        request.getStudyGroup().rejectRequest(request);
    }

    /**
     * Removes a member from a group this student leads.
     *
     * @param group the group to update
     * @param student the member to remove
     * @throws PermissionDeniedException if this student does not lead the group
     */
    public void removeMember(StudyGroup group, Student student) {
        requireLeads(group);
        group.removeMember(student);
    }

    /**
     * Sets a group's Telegram chat link.
     *
     * @param group the group to update
     * @param url the Telegram chat URL
     * @throws PermissionDeniedException if this student does not lead the group
     */
    public void setChatLink(StudyGroup group, String url) {
        requireLeads(group);
        group.setChatLink(url);
    }

    /**
     * Removes a group's Telegram chat link.
     *
     * @param group the group to update
     * @throws PermissionDeniedException if this student does not lead the group
     */
    public void clearChatLink(StudyGroup group) {
        requireLeads(group);
        group.clearChatLink();
    }

    private void requireLeads(StudyGroup group) {
        if (!leads(group)) {
            throw new PermissionDeniedException(NOT_LEADER);
        }
    }
}
