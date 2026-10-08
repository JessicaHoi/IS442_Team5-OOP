package is442t1.studybuddy.group;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import is442t1.studybuddy.common.exception.BusinessRuleException;
import is442t1.studybuddy.course.Course;
import is442t1.studybuddy.model.BaseEntity;
import is442t1.studybuddy.model.TimeSlot;
import is442t1.studybuddy.model.enums.GroupStatus;
import is442t1.studybuddy.model.enums.StudyGoal;
import is442t1.studybuddy.model.enums.StudyMode;
import is442t1.studybuddy.student.Student;

/**
 * A study group for one course. The leader is always a member. Changes that
 * only the leader may make are package-private and reached through
 * {@link StudyGroupLeader}, which checks leadership first.
 */
@Entity
@Table(name = "study_group")
public class StudyGroup extends BaseEntity {

    @Column(nullable = false)
    private String groupName;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_code", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leader_id", nullable = false)
    private StudyGroupLeader leader;

    @ManyToMany
    @JoinTable(
            name = "study_group_member",
            joinColumns = @JoinColumn(name = "study_group_id"),
            inverseJoinColumns = @JoinColumn(name = "student_id"))
    private Set<Student> members = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "study_group_goal", joinColumns = @JoinColumn(name = "study_group_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "study_goal", nullable = false)
    private Set<StudyGoal> studyGoals = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private StudyMode studyMode;

    @ElementCollection
    @CollectionTable(name = "study_group_availability", joinColumns = @JoinColumn(name = "study_group_id"))
    private List<TimeSlot> availability = new ArrayList<>();

    @Column(nullable = false)
    private int maxSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupStatus status = GroupStatus.OPEN;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "studyGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MembershipRequest> membershipRequests = new ArrayList<>();

    protected StudyGroup() {
        // Required by JPA.
    }

    StudyGroup(StudyGroupLeader leader, Course course, GroupDetails details) {
        this.leader = leader;
        this.course = course;
        this.members.add(leader);
        applyDetails(details);
    }

    public String getGroupName() {
        return groupName;
    }

    public String getDescription() {
        return description;
    }

    public Course getCourse() {
        return course;
    }

    public StudyGroupLeader getLeader() {
        return leader;
    }

    public Set<Student> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public Set<StudyGoal> getStudyGoals() {
        return Collections.unmodifiableSet(studyGoals);
    }

    public StudyMode getStudyMode() {
        return studyMode;
    }

    public List<TimeSlot> getAvailability() {
        return Collections.unmodifiableList(availability);
    }

    public int getMaxSize() {
        return maxSize;
    }

    public GroupStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isOpen() {
        return status == GroupStatus.OPEN;
    }

    public boolean isFull() {
        return members.size() >= maxSize;
    }

    public int getMemberCount() {
        return members.size();
    }

    public boolean isLedBy(UUID studentId) {
        return leader.hasId(studentId);
    }

    public boolean hasMember(UUID studentId) {
        return members.stream().anyMatch(member -> member.hasId(studentId));
    }

    public List<MembershipRequest> getPendingRequests() {
        return membershipRequests.stream().filter(MembershipRequest::isPending).toList();
    }

    public boolean hasPendingRequestFrom(UUID studentId) {
        return getPendingRequests().stream().anyMatch(request -> request.getStudent().hasId(studentId));
    }

    public Optional<MembershipRequest> findPendingRequest(UUID requestId) {
        return getPendingRequests().stream().filter(request -> requestId.equals(request.getId())).findFirst();
    }

    /** A student asks to join. Anyone may do this, so it is public. */
    public MembershipRequest requestToJoin(Student student, String message) {
        requireOpen();
        if (hasMember(student.getId())) {
            throw new BusinessRuleException("You are already in this group.");
        }
        if (isFull()) {
            throw new BusinessRuleException("This group is full.");
        }
        if (hasPendingRequestFrom(student.getId())) {
            throw new BusinessRuleException("You already have a pending request for this group.");
        }
        MembershipRequest request = new MembershipRequest(student, this, message);
        membershipRequests.add(request);
        return request;
    }

    void applyDetails(GroupDetails details) {
        requireOpen();
        TimeSlot.requireValidAvailability(details.availability());
        if (details.maxSize() < members.size()) {
            throw new BusinessRuleException(
                    "Maximum size cannot be lower than the current " + members.size() + " members.");
        }
        this.groupName = details.groupName();
        this.description = details.description();
        this.studyMode = details.studyMode();
        this.maxSize = details.maxSize();
        this.studyGoals.clear();
        this.studyGoals.addAll(details.studyGoals());
        this.availability.clear();
        this.availability.addAll(details.availability());
    }

    /** Closes the group for good. Pending join requests are rejected. */
    void close() {
        requireOpen();
        getPendingRequests().forEach(MembershipRequest::reject);
        status = GroupStatus.CLOSED;
    }

    void acceptRequest(MembershipRequest request) {
        requireOpen();
        requireOwnRequest(request);
        if (isFull()) {
            throw new BusinessRuleException("The group is full. Remove a member or raise the maximum size first.");
        }
        request.accept();
        members.add(request.getStudent());
    }

    void rejectRequest(MembershipRequest request) {
        requireOpen();
        requireOwnRequest(request);
        request.reject();
    }

    void removeMember(Student student) {
        requireOpen();
        if (isLedBy(student.getId())) {
            throw new BusinessRuleException("The group leader cannot be removed.");
        }
        if (!members.removeIf(member -> member.isSameStudent(student))) {
            throw new BusinessRuleException("This student is not a member of the group.");
        }
    }

    private void requireOpen() {
        if (!isOpen()) {
            throw new BusinessRuleException("This group is closed.");
        }
    }

    private void requireOwnRequest(MembershipRequest request) {
        if (request.getStudyGroup() != this) {
            throw new IllegalArgumentException("The request belongs to a different group.");
        }
    }
}
