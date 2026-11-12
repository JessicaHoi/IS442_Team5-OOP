package is442t1.studybuddy.group;

import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import is442t1.studybuddy.common.exception.BusinessRuleException;
import is442t1.studybuddy.common.exception.PermissionDeniedException;
import is442t1.studybuddy.common.exception.ResourceNotFoundException;
import is442t1.studybuddy.common.web.TimeSlotDto;
import is442t1.studybuddy.model.UserRepository;

/**
 * Manages each member's group-specific availability override.
 */
@Service
@Transactional(readOnly = true)
public class GroupAvailabilityService {
    private final StudyGroupRepository groups;
    private final UserRepository users;

    /**
     * Creates the service.
     *
     * @param groups repository for study groups
     * @param users account lookup and locking operations
     */
    public GroupAvailabilityService(StudyGroupRepository groups, UserRepository users) {
        this.groups = groups;
        this.users = users;
    }

    /**
     * Gets the member's effective availability for a group.
     *
     * @param groupId the group's identifier
     * @param studentId the member's identifier
     * @return the group override if present, otherwise the profile availability for the course
     * @throws ResourceNotFoundException if the group does not exist
     * @throws PermissionDeniedException if the student is not a group member
     */
    public GroupAvailabilityResponse get(UUID groupId, UUID studentId) {
        StudyGroup group = groups.findById(groupId).orElseThrow(() -> new ResourceNotFoundException("Group not found."));
        requireMember(group, studentId);
        return response(group, studentId);
    }

    /**
     * Replaces the member's group-specific availability if the supplied revision is current.
     *
     * @param groupId the group's identifier
     * @param studentId the member's identifier
     * @param request the replacement slots and revision being replaced
     * @return the effective availability and current revision after saving
     * @throws ResourceNotFoundException if the student or group does not exist
     * @throws PermissionDeniedException if the account is inactive or the student is not a member
     * @throws BusinessRuleException if the group is closed or the revision is stale
     */
    @Transactional
    public GroupAvailabilityResponse save(UUID groupId, UUID studentId, GroupAvailabilityRequest request) {
        StudyGroup group = editableGroup(groupId, studentId, request.revision());
        group.saveMemberAvailability(studentId, TimeSlotDto.toTimeSlots(request.availability()));
        groups.flush();
        return response(group, studentId);
    }

    /**
     * Removes the member's group-specific override if the supplied revision is current.
     *
     * @param groupId the group's identifier
     * @param studentId the member's identifier
     * @param revision the current override revision, or {@code null} if there is no override
     * @return the effective availability after reverting to profile slots
     * @throws ResourceNotFoundException if the student or group does not exist
     * @throws PermissionDeniedException if the account is inactive or the student is not a member
     * @throws BusinessRuleException if the group is closed or the revision is stale
     */
    @Transactional
    public GroupAvailabilityResponse reset(UUID groupId, UUID studentId, UUID revision) {
        StudyGroup group = editableGroup(groupId, studentId, revision);
        group.resetMemberAvailability(studentId);
        groups.flush();
        return response(group, studentId);
    }

    private StudyGroup editableGroup(UUID groupId, UUID studentId, UUID revision) {
        users.lockAccount(studentId).orElseThrow(() -> new ResourceNotFoundException("Student not found."));
        if (!users.findById(studentId).orElseThrow().isActive()) {
            throw new PermissionDeniedException("An active account is required.");
        }
        StudyGroup group = groups.findForUpdate(groupId).orElseThrow(() -> new ResourceNotFoundException("Group not found."));
        requireMember(group, studentId);
        if (!group.isOpen()) throw new BusinessRuleException("This group is closed. Availability cannot be changed.");
        UUID current = group.findMemberAvailability(studentId).map(GroupMemberAvailability::getRevision).orElse(null);
        if (!Objects.equals(current, revision)) {
            throw new BusinessRuleException("Your availability changed in another session. Reload it before saving again.");
        }
        return group;
    }

    private static void requireMember(StudyGroup group, UUID studentId) {
        if (!group.hasMember(studentId)) throw new PermissionDeniedException("Only group members can access group availability.");
    }

    private static GroupAvailabilityResponse response(StudyGroup group, UUID studentId) {
        var override = group.findMemberAvailability(studentId);
        return new GroupAvailabilityResponse(override.isPresent() ? "GROUP" : "PROFILE",
                override.map(GroupMemberAvailability::getRevision).orElse(null),
                TimeSlotDto.fromTimeSlots(group.memberAvailability(studentId)));
    }
}
