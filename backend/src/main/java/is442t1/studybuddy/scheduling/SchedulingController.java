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

/** Student endpoints for suggested meeting times. */
@RestController
@RequestMapping("/api")
@RequireRole(Role.STUDENT)
public class SchedulingController {

    private final BestTimeService bestTimeService;

    /**
     * Creates the scheduling controller.
     *
     * @param bestTimeService the meeting-time service
     */
    public SchedulingController(BestTimeService bestTimeService) {
        this.bestTimeService = bestTimeService;
    }

    /**
     * Returns suggested meeting times for a study-buddy connection.
     *
     * @param caller the authenticated student
     * @param id the connection identifier
     * @return shared meeting windows
     */
    @GetMapping("/connections/{id}/best-times")
    public List<TimeSlot> connectionBestTimes(@CurrentUser AuthenticatedUser caller, @PathVariable UUID id) {
        return bestTimeService.forConnection(id, caller.id());
    }

    /**
     * Returns suggested windows and the heatmap for a group the caller belongs to.
     *
     * @param caller the authenticated student
     * @param id the group identifier
     * @return the group suggestions and heatmap
     */
    @GetMapping("/groups/{id}/best-times")
    public GroupBestTimes groupBestTimes(@CurrentUser AuthenticatedUser caller, @PathVariable UUID id) {
        return bestTimeService.forGroup(id, caller.id());
    }
}