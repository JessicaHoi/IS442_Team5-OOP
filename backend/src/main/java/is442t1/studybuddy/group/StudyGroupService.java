package is442t1.studybuddy.group;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import is442t1.studybuddy.common.exception.PermissionDeniedException;
import is442t1.studybuddy.common.exception.ResourceNotFoundException;
import is442t1.studybuddy.common.exception.ValidationException;
import is442t1.studybuddy.course.Course;
import is442t1.studybuddy.course.CourseService;
import is442t1.studybuddy.group.event.GroupClosed;
import is442t1.studybuddy.model.enums.GroupStatus;
import is442t1.studybuddy.student.StudentRepository;

/** Creating, browsing, updating and closing study groups. */
@Service
@Transactional
public class StudyGroupService {

    private final StudyGroupRepository groupRepository;
    private final StudyGroupLeaderRepository leaderRepository;
    private final StudentRepository studentRepository;
    private final CourseService courseService;
    private final StudyGroupMapper mapper;
    private final GroupProperties properties;
    private final ApplicationEventPublisher events;

    public StudyGroupService(StudyGroupRepository groupRepository, StudyGroupLeaderRepository leaderRepository,
            StudentRepository studentRepository, CourseService courseService, StudyGroupMapper mapper,
            GroupProperties properties, ApplicationEventPublisher events) {
        this.groupRepository = groupRepository;
        this.leaderRepository = leaderRepository;
        this.studentRepository = studentRepository;
        this.courseService = courseService;
        this.mapper = mapper;
        this.properties = properties;
        this.events = events;
    }

    /** Open groups, optionally for one course, newest first. */
    @Transactional(readOnly = true)
    public List<StudyGroupResponse> listOpenGroups(UUID viewerId, String courseCode) {
        List<StudyGroup> groups = courseCode == null || courseCode.isBlank()
                ? groupRepository.findByStatusOrderByCreatedAtDesc(GroupStatus.OPEN)
                : groupRepository.findByStatusAndCourse_CourseCodeOrderByCreatedAtDesc(GroupStatus.OPEN, courseCode);
        return groups.stream().map(group -> mapper.toSummary(group, viewerId)).toList();
    }

    /** Groups the caller leads or belongs to, in any status. Led groups come first. */
    @Transactional(readOnly = true)
    public List<StudyGroupResponse> listMyGroups(UUID viewerId) {
        return groupRepository.findByMemberId(viewerId).stream()
                .map(group -> mapper.toSummary(group, viewerId))
                .sorted(Comparator.comparing((StudyGroupResponse group) -> group.myRole() != StudyGroupResponse.Role.LEADER)
                        .thenComparing(StudyGroupResponse::name))
                .toList();
    }

    @Transactional(readOnly = true)
    public StudyGroupResponse getGroup(UUID viewerId, UUID groupId) {
        return mapper.toDetail(requireGroup(groupId), viewerId);
    }

    /** Creates a group led by the caller, promoting them to leader if this is their first group. */
    public StudyGroupResponse createGroup(UUID studentId, StudyGroupRequest request) {
        if (request.courseCode() == null || request.courseCode().isBlank()) {
            throw new ValidationException("Choose a course.");
        }
        requireAllowedSize(request.maxSize());
        Course course = courseService.requireCourse(request.courseCode());
        StudyGroupLeader leader = findOrPromoteLeader(studentId);
        StudyGroup group = groupRepository.save(leader.createGroup(course, request.toDetails()));
        return mapper.toDetail(group, studentId);
    }

    /** Checks leadership before the new values, so a non-leader gets 403 whatever they send. */
    public StudyGroupResponse updateGroup(UUID studentId, UUID groupId, StudyGroupRequest request) {
        StudyGroup group = requireGroup(groupId);
        StudyGroupLeader leader = requireLeader(studentId);
        if (!leader.leads(group)) {
            throw new PermissionDeniedException(StudyGroupLeader.NOT_LEADER);
        }
        requireAllowedSize(request.maxSize());
        leader.updateGroup(group, request.toDetails());
        return mapper.toDetail(group, studentId);
    }

    public StudyGroupResponse closeGroup(UUID studentId, UUID groupId) {
        StudyGroup group = requireGroupForUpdate(groupId);
        requireLeader(studentId).closeGroup(group);
        events.publishEvent(new GroupClosed(groupId));
        return mapper.toDetail(group, studentId);
    }

    @Transactional(readOnly = true)
    public LedGroupResponse getLeadership(UUID studentId, UUID groupId) {
        StudyGroup group = requireGroup(groupId);
        if (!group.isLedBy(studentId)) {
            throw new PermissionDeniedException(StudyGroupLeader.NOT_LEADER);
        }
        return LedGroupResponse.from(group);
    }

    StudyGroup requireGroupForUpdate(UUID groupId) {
        return groupRepository.findForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Study group not found."));
    }

    public StudyGroupResponse setChatLink(UUID studentId, UUID groupId, String url) {
        StudyGroup group = requireGroupForUpdate(groupId);
        requireLeader(studentId).setChatLink(group, url);
        return mapper.toDetail(group, studentId);
    }

    public void clearChatLink(UUID studentId, UUID groupId) {
        StudyGroup group = requireGroupForUpdate(groupId);
        requireLeader(studentId).clearChatLink(group);
    }

    StudyGroup requireGroup(UUID groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Study group not found."));
    }

    /** The caller as a leader. A student who has never created a group cannot manage one. */
    StudyGroupLeader requireLeader(UUID studentId) {
        return leaderRepository.findById(studentId)
                .orElseThrow(() -> new PermissionDeniedException(StudyGroupLeader.NOT_LEADER));
    }

    private StudyGroupLeader findOrPromoteLeader(UUID studentId) {
        return leaderRepository.findById(studentId).orElseGet(() -> {
            if (!studentRepository.existsById(studentId)) {
                throw new ResourceNotFoundException("Student not found.");
            }
            leaderRepository.promoteStudent(studentId);
            return leaderRepository.findById(studentId)
                    .orElseThrow(() -> new IllegalStateException("Promotion to group leader failed."));
        });
    }

    private void requireAllowedSize(int maxSize) {
        if (maxSize < properties.minSize() || maxSize > properties.maxSize()) {
            throw new ValidationException("Maximum size must be between " + properties.minSize()
                    + " and " + properties.maxSize() + ".");
        }
    }
}
