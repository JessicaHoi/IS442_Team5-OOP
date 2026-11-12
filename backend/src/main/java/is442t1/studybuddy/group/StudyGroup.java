package is442t1.studybuddy.group;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

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
import is442t1.studybuddy.common.exception.ResourceNotFoundException;
import is442t1.studybuddy.common.exception.ValidationException;
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

    private static final Pattern CHAT_LINK = Pattern.compile("https://t\\.me/[A-Za-z0-9_+/?=&-]{1,240}");

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

    /** A Telegram chat the leader pasted in. Only members see it. */
    @Column(length = 255)
    private String chatLink;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupStatus status = GroupStatus.OPEN;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "studyGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MembershipRequest> membershipRequests = new ArrayList<>();

    @OneToMany(mappedBy = "studyGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupMemberAvailability> memberAvailabilities = new ArrayList<>();

    /**
     * Finds a member's group-specific availability override.
     *
     * @param studentId the member's identifier
     * @return the override, or empty when the member uses profile availability
     */
    public Optional<GroupMemberAvailability> findMemberAvailability(UUID studentId) {
        return memberAvailabilities.stream().filter(item -> item.belongsTo(studentId)).findFirst();
    }

    /**
     * Gets a member's group override, falling back to their availability for this course.
     *
     * @param studentId the member's identifier
     * @return the override slots or the member's profile slots for this course
     * @throws ResourceNotFoundException if the student is not a member
     */
    public List<TimeSlot> memberAvailability(UUID studentId) {
        Student member = members.stream().filter(item -> item.hasId(studentId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Group member not found."));
        return findMemberAvailability(studentId).map(GroupMemberAvailability::getSlots)
                .orElseGet(() -> member.availabilityFor(course.getCourseCode()));
    }

    void saveMemberAvailability(UUID studentId, List<TimeSlot> slots) {
        requireOpen();
        Student member = members.stream().filter(item -> item.hasId(studentId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Group member not found."));
        findMemberAvailability(studentId).ifPresentOrElse(item -> item.replace(slots),
                () -> memberAvailabilities.add(new GroupMemberAvailability(this, member, slots)));
    }

    void resetMemberAvailability(UUID studentId) {
        requireOpen();
        memberAvailabilities.removeIf(item -> item.belongsTo(studentId));
    }

    protected StudyGroup() {
        // Required by JPA.
    }

    StudyGroup(StudyGroupLeader leader, Course course, GroupDetails details) {
        this.leader = leader;
        this.course = course;
        this.members.add(leader);
        applyDetails(details);
    }

    /** Gets the group's name.
     * @return the group's name
     */
    public String getGroupName() {
        return groupName;
    }

    /** Gets the group's description.
     * @return the group's description
     */
    public String getDescription() {
        return description;
    }

    /** Gets the course this group is for.
     * @return the associated course
     */
    public Course getCourse() {
        return course;
    }

    /** Gets the group's current leader.
     * @return the current leader
     */
    public StudyGroupLeader getLeader() {
        return leader;
    }

    /** Gets an unmodifiable view of the group's members.
     * @return the group's members
     */
    public Set<Student> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    /** Gets an unmodifiable view of the group's study goals.
     * @return the group's study goals
     */
    public Set<StudyGoal> getStudyGoals() {
        return Collections.unmodifiableSet(studyGoals);
    }

    /** Gets the group's meeting mode.
     * @return the meeting mode
     */
    public StudyMode getStudyMode() {
        return studyMode;
    }

    /** Gets an unmodifiable view of the group's proposed availability.
     * @return the proposed time slots
     */
    public List<TimeSlot> getAvailability() {
        return Collections.unmodifiableList(availability);
    }

    /** Gets the maximum number of members allowed in the group.
     * @return the maximum member count
     */
    public int getMaxSize() {
        return maxSize;
    }

    /** Gets the Telegram chat link, or {@code null} if none has been set.
     * @return the chat link, if configured
     */
    public String getChatLink() {
        return chatLink;
    }

    /** Gets the group's status.
     * @return whether the group is open or closed
     */
    public GroupStatus getStatus() {
        return status;
    }

    /** Gets the creation time.
     * @return when the group was created
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Checks whether the group is open to membership and management changes.
     * @return {@code true} if the group is open
     */
    public boolean isOpen() {
        return status == GroupStatus.OPEN;
    }

    /** Checks whether the group has reached its maximum size.
     * @return {@code true} if the current member count is at least the maximum
     */
    public boolean isFull() {
        return members.size() >= maxSize;
    }

    /** Counts the group's current members, including the leader.
     * @return the current member count
     */
    public int getMemberCount() {
        return members.size();
    }

    /**
     * Checks whether a student is the group's current leader.
     *
     * @param studentId the student's identifier
     * @return {@code true} if that student leads this group
     */
    public boolean isLedBy(UUID studentId) {
        return leader.hasId(studentId);
    }

    /**
     * Checks whether a student belongs to this group.
     *
     * @param studentId the student's identifier
     * @return {@code true} if the student is a member
     */
    public boolean hasMember(UUID studentId) {
        return members.stream().anyMatch(member -> member.hasId(studentId));
    }

    /** Gets this group's join requests that have not yet been answered.
     * @return the pending join requests
     */
    public List<MembershipRequest> getPendingRequests() {
        return membershipRequests.stream().filter(MembershipRequest::isPending).toList();
    }

    /**
     * Checks for an unanswered join request from a student.
     *
     * @param studentId the student's identifier
     * @return {@code true} if the student has a pending request
     */
    public boolean hasPendingRequestFrom(UUID studentId) {
        return getPendingRequests().stream().anyMatch(request -> request.getStudent().hasId(studentId));
    }

    /**
     * Finds an unanswered join request by its identifier.
     *
     * @param requestId the request identifier
     * @return the matching pending request, or empty if none exists
     */
    public Optional<MembershipRequest> findPendingRequest(UUID requestId) {
        return getPendingRequests().stream().filter(request -> requestId.equals(request.getId())).findFirst();
    }

    /**
     * Records a student's request to join this open group.
     *
     * @param student the student requesting membership
     * @param message the optional message attached to the request
     * @return the newly created pending request
     * @throws BusinessRuleException if the group is closed or full, or the student is already a member
     *         or already has a pending request
     */
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

    /**
     * Removes a member who chooses to leave the group.
     *
     * @param student the member leaving
     * @throws BusinessRuleException if the group is closed, the student is not a member,
     *         or the student is the leader
     */
    public void leave(Student student) {
        requireOpen();
        if (isLedBy(student.getId())) {
            throw new BusinessRuleException("The group leader cannot leave. Transfer leadership or close the group first.");
        }
        if (!dropMember(student)) {
            throw new BusinessRuleException("You are not a member of this group.");
        }
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
        if (!dropMember(student)) {
            throw new BusinessRuleException("This student is not a member of the group.");
        }
    }

    private boolean dropMember(Student student) {
        boolean removed = members.removeIf(member -> member.isSameStudent(student));
        if (removed) {
            memberAvailabilities.removeIf(item -> item.belongsTo(student.getId()));
        }
        return removed;
    }

    void setChatLink(String url) {
        requireOpen();
        String link = url == null ? "" : url.trim();
        if (!CHAT_LINK.matcher(link).matches()) {
            throw new ValidationException("Enter a Telegram link that starts with https://t.me/.");
        }
        chatLink = link;
    }

    void clearChatLink() {
        requireOpen();
        chatLink = null;
    }

    private void requireOpen() {
        if (!isOpen()) {
            throw new BusinessRuleException("This group is closed.");
        }
    }

    /**
     * Makes an existing active member the group's leader.
     *
     * @param successor the member who will lead the group
     * @throws BusinessRuleException if the successor is inactive, not a member, or already the leader
     */
    public void transferLeadershipTo(StudyGroupLeader successor) {
        if (!successor.isActive() || !hasMember(successor.getId()) || isLedBy(successor.getId())) {
            throw new BusinessRuleException("Choose another active member as the new group leader.");
        }
        leader = successor;
    }

    /**
     * Removes a deleted student's membership, requests, and group availability.
     *
     * @param studentId the deleted student's identifier
     * @throws BusinessRuleException if the deleted student is still the group leader
     */
    public void removeDeletedAccount(UUID studentId) {
        if (isLedBy(studentId)) {
            throw new BusinessRuleException("Transfer group leadership before deleting this account.");
        }
        members.removeIf(member -> member.hasId(studentId));
        membershipRequests.removeIf(request -> request.getStudent().hasId(studentId));
        memberAvailabilities.removeIf(item -> item.belongsTo(studentId));
    }

    /** A request for another group does not exist as far as this group is concerned (404, not 500). */
    private void requireOwnRequest(MembershipRequest request) {
        if (request.getStudyGroup() != this) {
            throw new ResourceNotFoundException("Join request not found.");
        }
    }
}
