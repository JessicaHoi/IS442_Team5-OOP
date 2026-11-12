package is442t1.studybuddy.group;

import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import is442t1.studybuddy.common.exception.PermissionDeniedException;
import is442t1.studybuddy.common.exception.ResourceNotFoundException;
import is442t1.studybuddy.common.exception.ValidationException;
import is442t1.studybuddy.group.event.LeadershipTransferred;
import is442t1.studybuddy.model.User;
import is442t1.studybuddy.model.UserRepository;
import is442t1.studybuddy.student.Student;

/**
 * Transfers study group leadership between active student members.
 */
@Service
@Transactional
public class GroupLeadershipService {

    private final UserRepository userRepository;
    private final StudyGroupRepository groupRepository;
    private final StudyGroupLeaderRepository leaderRepository;
    private final ApplicationEventPublisher events;

    /**
     * Creates the leadership service.
     *
     * @param userRepository account lookup and locking operations
     * @param groupRepository group lookup and locking operations
     * @param leaderRepository leader promotion and demotion operations
     * @param events publisher for leadership-transfer events
     */
    public GroupLeadershipService(UserRepository userRepository, StudyGroupRepository groupRepository,
            StudyGroupLeaderRepository leaderRepository, ApplicationEventPublisher events) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.leaderRepository = leaderRepository;
        this.events = events;
    }

    /**
     * Transfers a group's leadership to another active student who is already a member.
     *
     * <p>When invoked by an administrator, a non-matching current leader is reported
     * as a missing group, and the current leader need not have an active account.</p>
     *
     * @param leaderId the current leader's identifier
     * @param groupId the group's identifier
     * @param successorId the selected successor's identifier
     * @param byAdministrator whether the transfer is being performed by an administrator
     * @return the group's leadership details after the transfer
     * @throws ResourceNotFoundException if the accounts or group do not exist, or an administrator's
     *         target does not belong to that leader
     * @throws PermissionDeniedException if a non-administrator is not the active current leader
     * @throws ValidationException if the successor is not another active student member
     */
    public LedGroupResponse transfer(UUID leaderId, UUID groupId, UUID successorId, boolean byAdministrator) {
        Stream.of(leaderId, successorId).distinct().sorted().forEach(id -> userRepository.lockAccount(id)
                .orElseThrow(() -> new ResourceNotFoundException("User account not found.")));
        StudyGroup group = groupRepository.findForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Study group not found."));
        if (!group.isLedBy(leaderId)) {
            if (byAdministrator) {
                throw new ResourceNotFoundException("Study group not found for this leader.");
            }
            throw new PermissionDeniedException(StudyGroupLeader.NOT_LEADER);
        }
        User leader = userRepository.findById(leaderId).orElseThrow();
        if (!byAdministrator && !leader.isActive()) {
            throw new PermissionDeniedException("An active group leader account is required.");
        }
        User successor = userRepository.findById(successorId).orElseThrow();
        if (!(successor instanceof Student) || !successor.isActive()
                || successorId.equals(leaderId) || !group.hasMember(successorId)) {
            throw new ValidationException("Choose another active member as the new group leader.");
        }
        if (!(successor instanceof StudyGroupLeader)) {
            leaderRepository.promoteStudent(successorId);
        }
        group = groupRepository.findById(groupId).orElseThrow();
        group.transferLeadershipTo(leaderRepository.findById(successorId).orElseThrow());
        groupRepository.flush();
        leaderRepository.demoteIfNoGroups(leaderId);
        events.publishEvent(new LeadershipTransferred(groupId, successorId));
        return LedGroupResponse.from(groupRepository.findById(groupId).orElseThrow());
    }
}
