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

@RestController
@RequestMapping("/api/groups/{groupId}/availability/me")
@RequireRole(Role.STUDENT)
public class GroupAvailabilityController {
    private final GroupAvailabilityService service;

    public GroupAvailabilityController(GroupAvailabilityService service) { this.service = service; }

    @GetMapping
    public GroupAvailabilityResponse get(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId) {
        return service.get(groupId, caller.id());
    }

    @PutMapping
    public GroupAvailabilityResponse save(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @Valid @RequestBody GroupAvailabilityRequest request) {
        return service.save(groupId, caller.id(), request);
    }

    @DeleteMapping
    public GroupAvailabilityResponse reset(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @RequestParam UUID revision) {
        return service.reset(groupId, caller.id(), revision);
    }
}
