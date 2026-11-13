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
 * Reads group-specific availability or profile fallback and serialises override writes with account
 *     and group locks. Writes require active membership, an open group and the latest override
 *     revision.
 */
@Service
@Transactional(readOnly = true)
public class GroupAvailabilityService {
    private final StudyGroupRepository groups;
    private final UserRepository users;

    public GroupAvailabilityService(StudyGroupRepository groups, UserRepository users) {
        this.groups = groups;
        this.users = users;
    }

    /** Reads the member's override, or their preference availability for the group's course if none exists. */
    public GroupAvailabilityResponse get(UUID groupId, UUID studentId) {
        StudyGroup group = groups.findById(groupId).orElseThrow(() -> new ResourceNotFoundException("Group not found."));
        requireMember(group, studentId);
        return response(group, studentId);
    }

    /**
     * Saves an override using optimistic revision comparison under a group lock.
     * A first save requires a null revision; subsequent saves require the latest UUID.
     * Empty availability is an explicit unavailable override. Each save issues a new revision.
     */
    @Transactional
    public GroupAvailabilityResponse save(UUID groupId, UUID studentId, GroupAvailabilityRequest request) {
        StudyGroup group = editableGroup(groupId, studentId, request.revision());
        group.saveMemberAvailability(studentId, TimeSlotDto.toTimeSlots(request.availability()));
        groups.flush();
        return response(group, studentId);
    }

    /**
     * Removes an override when its revision matches, restoring profile fallback.
     * Returns the effective profile state with a null revision. Closed groups and
     * stale revisions produce a business conflict (HTTP 409 at the web boundary).
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
