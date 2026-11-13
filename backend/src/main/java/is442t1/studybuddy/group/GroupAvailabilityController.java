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
 * Student-only endpoints for a group member to read, save or reset their own availability override.
 *     See GroupAvailabilityRequest for revision and empty-list semantics.
 */
@RestController
@RequestMapping("/api/groups/{groupId}/availability/me")
@RequireRole(Role.STUDENT)
public class GroupAvailabilityController {
    private final GroupAvailabilityService service;

    public GroupAvailabilityController(GroupAvailabilityService service) { this.service = service; }

    /**
     * Reads effective availability: {@code GET /api/groups/{groupId} /availability/me}, members
     *     only.
     * Returns HTTP 200 with source, revision and slots. PROFILE has a null revision; GROUP has a
     *     UUID.
     * Non-members receive 403; a missing group is 404.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param groupId study group UUID from the path
     * @return response described above, with the documented HTTP status and payload
     */
    @GetMapping
    public GroupAvailabilityResponse get(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId) {
        return service.get(groupId, caller.id());
    }

    /**
     * Replaces a group override: {@code PUT /api/groups/{groupId} /availability/me}, active members
     *     only.
     * Example first save:
     *     {@code {"revision":null,"availability":[{"day":"WED","start":"19:00","end":"21:00"} ]}}.
     * Later saves must send the latest returned revision. An empty list means unavailable.
     * Returns HTTP 200 with a new revision; invalid slots are 400, non-members are 403, and closed
     *     groups or stale revisions are 409.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param groupId study group UUID from the path
     * @param request JSON body described above
     * @return response described above, with the documented HTTP status and payload
     */
    @PutMapping
    public GroupAvailabilityResponse save(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @Valid @RequestBody GroupAvailabilityRequest request) {
        return service.save(groupId, caller.id(), request);
    }

    /**
     * Restores profile fallback: {@code DELETE /api/groups/{groupId}
     *     /availability/me?revision=uuid}, active members only.
     * The revision must match the existing override; after reset, source is PROFILE and revision is
     *     null.
     * Returns HTTP 200 with the effective state, not 204. Non-members are 403; closed groups and
     *     stale revisions are 409.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param groupId study group UUID from the path
     * @param revision current availability override revision UUID
     * @return response described above, with the documented HTTP status and payload
     */
    @DeleteMapping
    public GroupAvailabilityResponse reset(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @RequestParam UUID revision) {
        return service.reset(groupId, caller.id(), revision);
    }
}
