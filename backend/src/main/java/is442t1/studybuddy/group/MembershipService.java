package is442t1.studybuddy.group;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import is442t1.studybuddy.common.exception.ResourceNotFoundException;
import is442t1.studybuddy.student.Student;
import is442t1.studybuddy.student.StudentService;

/** Join requests and membership of study groups. */
@Service
@Transactional
public class MembershipService {

    private final StudyGroupService groupService;
    private final StudentService studentService;
    private final StudyGroupMapper mapper;

    public MembershipService(StudyGroupService groupService, StudentService studentService, StudyGroupMapper mapper) {
        this.groupService = groupService;
        this.studentService = studentService;
        this.mapper = mapper;
    }

    public StudyGroupResponse requestToJoin(UUID studentId, UUID groupId, String message) {
        StudyGroup group = groupService.requireGroup(groupId);
        group.requestToJoin(studentService.requireStudent(studentId), message);
        return mapper.toSummary(group, studentId);
    }

    /** Pending requests, oldest first. Leader only. */
    @Transactional(readOnly = true)
    public List<JoinRequestResponse> listPendingRequests(UUID studentId, UUID groupId) {
        StudyGroup group = groupService.requireGroup(groupId);
        return groupService.requireLeader(studentId).viewJoinRequests(group).stream()
                .sorted(Comparator.comparing(MembershipRequest::getRequestDate))
                .map(JoinRequestResponse::from)
                .toList();
    }

    public StudyGroupResponse acceptRequest(UUID studentId, UUID groupId, UUID requestId) {
        StudyGroup group = groupService.requireGroup(groupId);
        StudyGroupLeader leader = groupService.requireLeader(studentId);
        leader.acceptMember(requirePendingRequest(group, requestId));
        return mapper.toDetail(group, studentId);
    }

    public StudyGroupResponse rejectRequest(UUID studentId, UUID groupId, UUID requestId) {
        StudyGroup group = groupService.requireGroup(groupId);
        StudyGroupLeader leader = groupService.requireLeader(studentId);
        leader.rejectMember(requirePendingRequest(group, requestId));
        return mapper.toDetail(group, studentId);
    }

    public void removeMember(UUID studentId, UUID groupId, UUID memberId) {
        StudyGroup group = groupService.requireGroup(groupId);
        StudyGroupLeader leader = groupService.requireLeader(studentId);
        Student member = studentService.requireStudent(memberId);
        leader.removeMember(group, member);
    }

    private static MembershipRequest requirePendingRequest(StudyGroup group, UUID requestId) {
        return group.findPendingRequest(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Join request not found."));
    }
}
