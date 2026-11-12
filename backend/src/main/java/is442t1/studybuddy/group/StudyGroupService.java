package is442t1.studybuddy.group;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
import is442t1.studybuddy.group.event.GroupRenamed;
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

    /**
     * Creates the service and its persistence, mapping, course, and event collaborators.
     *
     * @param groupRepository repository for study groups
     * @param leaderRepository repository for group leaders
     * @param studentRepository repository for student accounts
     * @param courseService service for resolving courses
     * @param mapper mapper for group responses
     * @param properties configured group-size limits
     * @param events publisher for group lifecycle events
     */
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

    /**
     * Lists open groups, optionally restricted to a course, newest first.
     * A {@code null} or blank course code does not restrict the results.
     *
     * @param viewerId the caller's identifier, used to personalize each response
     * @param courseCode optional course code filter
     * @return the matching group summaries
     */
    @Transactional(readOnly = true)
    public List<StudyGroupResponse> listOpenGroups(UUID viewerId, String courseCode) {
        List<StudyGroup> groups = courseCode == null || courseCode.isBlank()
                ? groupRepository.findByStatusOrderByCreatedAtDesc(GroupStatus.OPEN)
                : groupRepository.findByStatusAndCourse_CourseCodeOrderByCreatedAtDesc(GroupStatus.OPEN, courseCode);
        return groups.stream().map(group -> mapper.toSummary(group, viewerId)).toList();
    }

    /**
     * Lists groups the caller leads or belongs to, regardless of status.
     * Groups led by the caller come first; each section is ordered by name.
     *
     * @param viewerId the caller's identifier
     * @return the caller's group summaries
     */
    @Transactional(readOnly = true)
    public List<StudyGroupResponse> listMyGroups(UUID viewerId) {
        return groupRepository.findByMemberId(viewerId).stream()
                .map(group -> mapper.toSummary(group, viewerId))
                .sorted(Comparator.comparing((StudyGroupResponse group) -> group.myRole() != StudyGroupResponse.Role.LEADER)
                        .thenComparing(StudyGroupResponse::name))
                .toList();
    }

    /**
     * Gets a group's detail representation for the caller.
     *
     * @param viewerId the caller's identifier
     * @param groupId the group's identifier
     * @return the group detail response
     * @throws ResourceNotFoundException if the group does not exist
     */
    @Transactional(readOnly = true)
    public StudyGroupResponse getGroup(UUID viewerId, UUID groupId) {
        return mapper.toDetail(requireGroup(groupId), viewerId);
    }

    /**
     * Creates a group led by the caller, promoting them to leader if this is their first group.
     *
     * @param studentId the creator's identifier
     * @param request the requested group settings
     * @return the created group's detail response
     * @throws ValidationException if no course code is supplied or the maximum size is outside
     *         the configured range
     * @throws ResourceNotFoundException if the course or student does not exist
     */
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

    /**
     * Updates a group's editable settings after checking the caller's leadership.
     *
     * <p>The course cannot be changed. A name or description change publishes a
     * {@link GroupRenamed} event.</p>
     *
     * @param studentId the caller's identifier
     * @param groupId the group's identifier
     * @param request the replacement group settings
     * @return the updated group's detail response
     * @throws ResourceNotFoundException if the group does not exist
     * @throws PermissionDeniedException if the caller is not the group's leader
     * @throws ValidationException if the requested maximum size is outside the configured range
     */
    public StudyGroupResponse updateGroup(UUID studentId, UUID groupId, StudyGroupRequest request) {
        StudyGroup group = requireGroup(groupId);
        StudyGroupLeader leader = requireLeader(studentId);
        if (!leader.leads(group)) {
            throw new PermissionDeniedException(StudyGroupLeader.NOT_LEADER);
        }
        requireAllowedSize(request.maxSize());
        String oldName = group.getGroupName();
        String oldDescription = group.getDescription();
        leader.updateGroup(group, request.toDetails());
        if (!Objects.equals(oldName, group.getGroupName()) || !Objects.equals(oldDescription, group.getDescription())) {
            events.publishEvent(new GroupRenamed(groupId));
        }
        return mapper.toDetail(group, studentId);
    }

    /**
     * Closes a group and publishes a {@link GroupClosed} event.
     *
     * @param studentId the caller's identifier
     * @param groupId the group's identifier
     * @return the closed group's detail response
     * @throws ResourceNotFoundException if the group does not exist
     * @throws PermissionDeniedException if the caller is not the group's leader
     */
    public StudyGroupResponse closeGroup(UUID studentId, UUID groupId) {
        StudyGroup group = requireGroupForUpdate(groupId);
        requireLeader(studentId).closeGroup(group);
        events.publishEvent(new GroupClosed(groupId));
        return mapper.toDetail(group, studentId);
    }

    /**
     * Gets leadership details for a group.
     *
     * @param studentId the caller's identifier
     * @param groupId the group's identifier
     * @return the group's current leader and eligible successors
     * @throws ResourceNotFoundException if the group does not exist
     * @throws PermissionDeniedException if the caller is not the group's leader
     */
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

    /**
     * Sets the group's Telegram chat link.
     *
     * @param studentId the caller's identifier
     * @param groupId the group's identifier
     * @param url the Telegram URL to store
     * @return the updated group's detail response
     * @throws ResourceNotFoundException if the group does not exist
     * @throws PermissionDeniedException if the caller is not the group's leader
     * @throws ValidationException if the URL is not an accepted Telegram link
     */
    public StudyGroupResponse setChatLink(UUID studentId, UUID groupId, String url) {
        StudyGroup group = requireGroupForUpdate(groupId);
        requireLeader(studentId).setChatLink(group, url);
        return mapper.toDetail(group, studentId);
    }

    /**
     * Clears the group's Telegram chat link.
     *
     * @param studentId the caller's identifier
     * @param groupId the group's identifier
     * @throws ResourceNotFoundException if the group does not exist
     * @throws PermissionDeniedException if the caller is not the group's leader
     */
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
