package is442t1.studybuddy.scheduling;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import is442t1.studybuddy.auth.AuthenticatedUser;
import is442t1.studybuddy.auth.CurrentUser;
import is442t1.studybuddy.auth.RequireRole;
import is442t1.studybuddy.model.TimeSlot;
import is442t1.studybuddy.model.enums.Role;

@RestController
@RequestMapping("/api")
@RequireRole(Role.STUDENT)
public class SchedulingController {

    private final BestTimeService bestTimeService;

    public SchedulingController(BestTimeService bestTimeService) {
        this.bestTimeService = bestTimeService;
    }

    /** Suggested meeting times for an accepted study-buddy connection. */
    @GetMapping("/connections/{id}/best-times")
    public List<TimeSlot> connectionBestTimes(@CurrentUser AuthenticatedUser caller, @PathVariable UUID id) {
        return bestTimeService.forConnection(id, caller.id());
    }

    /** Suggested windows and the heatmap for a study group the caller belongs to. */
    @GetMapping("/groups/{id}/best-times")
    public GroupBestTimes groupBestTimes(@CurrentUser AuthenticatedUser caller, @PathVariable UUID id) {
        return bestTimeService.forGroup(id, caller.id());
    }
}