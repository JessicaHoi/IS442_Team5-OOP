package is442t1.studybuddy.group;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import is442t1.studybuddy.auth.AuthenticatedUser;
import is442t1.studybuddy.auth.CurrentUser;
import is442t1.studybuddy.auth.RequireRole;
import is442t1.studybuddy.model.enums.Role;

/**
 * Reads and edits the authenticated student's availability for one group.
 */
@RestController
@RequestMapping("/api/groups/{groupId}/availability/me")
@RequireRole(Role.STUDENT)
public class GroupAvailabilityController {
    private final GroupAvailabilityService service;

    /**
     * Creates the controller.
     *
     * @param service group availability operations
     */
    public GroupAvailabilityController(GroupAvailabilityService service) { this.service = service; }

    /**
     * Gets the caller's effective availability for a group.
     *
     * @param caller the authenticated student
     * @param groupId the group's identifier
     * @return the group override, or profile availability if no override exists
     */
    @GetMapping
    public GroupAvailabilityResponse get(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId) {
        return service.get(groupId, caller.id());
    }

    /**
     * Saves the caller's group-specific availability using the supplied revision.
     *
     * @param caller the authenticated student
     * @param groupId the group's identifier
     * @param request the replacement slots and revision being replaced
     * @return the saved availability and its new revision
     */
    @PutMapping
    public GroupAvailabilityResponse save(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @Valid @RequestBody GroupAvailabilityRequest request) {
        return service.save(groupId, caller.id(), request);
    }

    /**
     * Removes the caller's group-specific override and returns to profile availability.
     *
     * @param caller the authenticated student
     * @param groupId the group's identifier
     * @param revision the current override revision, or {@code null} if there is no override
     * @return the effective availability after the reset
     */
    @DeleteMapping
    public GroupAvailabilityResponse reset(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @RequestParam UUID revision) {
        return service.reset(groupId, caller.id(), revision);
    }
}
