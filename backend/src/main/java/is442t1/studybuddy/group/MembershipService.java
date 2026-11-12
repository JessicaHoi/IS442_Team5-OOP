package is442t1.studybuddy.group;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import is442t1.studybuddy.common.exception.ResourceNotFoundException;
import is442t1.studybuddy.common.exception.BusinessRuleException;
import is442t1.studybuddy.common.exception.PermissionDeniedException;
import is442t1.studybuddy.group.event.MemberJoined;
import is442t1.studybuddy.group.event.MemberLeft;
import is442t1.studybuddy.student.Student;
import is442t1.studybuddy.student.StudentService;

/** Join requests and membership of study groups. */
@Service
@Transactional
public class MembershipService {

    private final StudyGroupService groupService;
    private final StudentService studentService;
    private final StudyGroupMapper mapper;
    private final ApplicationEventPublisher events;

    /**
     * Creates the membership service.
     *
     * @param groupService group lookup and leader authorization
     * @param studentService student lookup
     * @param mapper group response mapper
     * @param events publisher for membership events
     */
    public MembershipService(StudyGroupService groupService, StudentService studentService, StudyGroupMapper mapper,
            ApplicationEventPublisher events) {
        this.groupService = groupService;
        this.studentService = studentService;
        this.mapper = mapper;
        this.events = events;
    }

    /**
     * Submits a request for a student to join a group.
     *
     * @param studentId the requesting student's identifier
     * @param groupId the group's identifier
     * @param message the optional request message
     * @return the group summary from the requesting student's perspective
     * @throws ResourceNotFoundException if the group or student does not exist
     * @throws BusinessRuleException if either account is inactive or the group disallows the request
     */
    public StudyGroupResponse requestToJoin(UUID studentId, UUID groupId, String message) {
        StudyGroup group = groupService.requireGroup(groupId);
        Student student = studentService.requireStudent(studentId);
        if (!group.getLeader().isActive() || !student.isActive()) {
            throw new BusinessRuleException("The student and group leader must have active accounts to request membership.");
        }
        group.requestToJoin(student, message);
        return mapper.toSummary(group, studentId);
    }

    /**
     * Lists a group's pending join requests in creation order.
     * Any group member can view the list.
     *
     * @param studentId the caller's identifier
     * @param groupId the group's identifier
     * @return the pending requests, oldest first
     * @throws ResourceNotFoundException if the group does not exist
     * @throws PermissionDeniedException if the caller is not a group member
     */
    @Transactional(readOnly = true)
    public List<JoinRequestResponse> listPendingRequests(UUID studentId, UUID groupId) {
        StudyGroup group = groupService.requireGroup(groupId);
        if (!group.hasMember(studentId)) {
            throw new PermissionDeniedException("Only group members can view join requests.");
        }
        return group.getPendingRequests().stream()
                .sorted(Comparator.comparing(MembershipRequest::getRequestDate))
                .map(JoinRequestResponse::from)
                .toList();
    }

    /**
     * Accepts a pending request and adds its active requester as a member.
     *
     * @param studentId the caller's identifier
     * @param groupId the group's identifier
     * @param requestId the pending request's identifier
     * @return the updated group detail response
     * @throws ResourceNotFoundException if the group or pending request does not exist
     * @throws PermissionDeniedException if the caller is not the group's leader
     * @throws BusinessRuleException if the leader or requester is inactive, or the group cannot accept
     *         another member
     */
    public StudyGroupResponse acceptRequest(UUID studentId, UUID groupId, UUID requestId) {
        StudyGroup group = groupService.requireGroup(groupId);
        StudyGroupLeader leader = groupService.requireLeader(studentId);
        if (!leader.leads(group)) {
            throw new PermissionDeniedException(StudyGroupLeader.NOT_LEADER);
        }
        MembershipRequest request = requirePendingRequest(group, requestId);
        if (!leader.isActive() || !request.getStudent().isActive()) {
            throw new BusinessRuleException("The student and group leader must have active accounts to accept membership.");
        }
        leader.acceptMember(request);
        events.publishEvent(new MemberJoined(groupId, request.getStudent().getId()));
        return mapper.toDetail(group, studentId);
    }

    /**
     * Rejects a pending request to join a group.
     *
     * @param studentId the caller's identifier
     * @param groupId the group's identifier
     * @param requestId the pending request's identifier
     * @return the group detail response after rejection
     * @throws ResourceNotFoundException if the group or pending request does not exist
     * @throws PermissionDeniedException if the caller is not the group's leader
     */
    public StudyGroupResponse rejectRequest(UUID studentId, UUID groupId, UUID requestId) {
        StudyGroup group = groupService.requireGroup(groupId);
        StudyGroupLeader leader = groupService.requireLeader(studentId);
        leader.rejectMember(requirePendingRequest(group, requestId));
        return mapper.toDetail(group, studentId);
    }

    /**
     * Removes a student from a group and publishes a {@link MemberLeft} event.
     *
     * @param studentId the caller's identifier, who must lead the group
     * @param groupId the group's identifier
     * @param memberId the member to remove
     * @throws ResourceNotFoundException if the group or student does not exist
     * @throws PermissionDeniedException if the caller is not the group's leader
     */
    public void removeMember(UUID studentId, UUID groupId, UUID memberId) {
        StudyGroup group = groupService.requireGroupForUpdate(groupId);
        StudyGroupLeader leader = groupService.requireLeader(studentId);
        Student member = studentService.requireStudent(memberId);
        leader.removeMember(group, member);
        events.publishEvent(new MemberLeft(groupId, memberId));
    }

    /**
     * Removes the caller from a group and publishes a {@link MemberLeft} event.
     *
     * @param studentId the departing student's identifier
     * @param groupId the group's identifier
     * @throws ResourceNotFoundException if the group or student does not exist
     * @throws BusinessRuleException if the group is closed, the caller is not a member,
     *         or the caller is the leader
     */
    public void leaveGroup(UUID studentId, UUID groupId) {
        StudyGroup group = groupService.requireGroupForUpdate(groupId);
        group.leave(studentService.requireStudent(studentId));
        events.publishEvent(new MemberLeft(groupId, studentId));
    }

    private static MembershipRequest requirePendingRequest(StudyGroup group, UUID requestId) {
        return group.findPendingRequest(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Join request not found."));
    }
}
