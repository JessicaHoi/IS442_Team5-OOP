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

    public MembershipService(StudyGroupService groupService, StudentService studentService, StudyGroupMapper mapper,
            ApplicationEventPublisher events) {
        this.groupService = groupService;
        this.studentService = studentService;
        this.mapper = mapper;
        this.events = events;
    }

    public StudyGroupResponse requestToJoin(UUID studentId, UUID groupId, String message) {
        StudyGroup group = groupService.requireGroup(groupId);
        Student student = studentService.requireStudent(studentId);
        if (!group.getLeader().isActive() || !student.isActive()) {
            throw new BusinessRuleException("The student and group leader must have active accounts to request membership.");
        }
        group.requestToJoin(student, message);
        return mapper.toSummary(group, studentId);
    }

    /** Pending requests, oldest first. Leader only. */
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

    public StudyGroupResponse rejectRequest(UUID studentId, UUID groupId, UUID requestId) {
        StudyGroup group = groupService.requireGroup(groupId);
        StudyGroupLeader leader = groupService.requireLeader(studentId);
        leader.rejectMember(requirePendingRequest(group, requestId));
        return mapper.toDetail(group, studentId);
    }

    public void removeMember(UUID studentId, UUID groupId, UUID memberId) {
        StudyGroup group = groupService.requireGroupForUpdate(groupId);
        StudyGroupLeader leader = groupService.requireLeader(studentId);
        Student member = studentService.requireStudent(memberId);
        leader.removeMember(group, member);
        events.publishEvent(new MemberLeft(groupId, memberId));
    }

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
