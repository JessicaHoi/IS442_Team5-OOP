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

/**
 * Student-only meeting suggestions for buddy-connection participants and group members. These
 *     endpoints suggest weekly windows rather than creating study sessions.
 */
@RestController
@RequestMapping("/api")
@RequireRole(Role.STUDENT)
public class SchedulingController {

    private final BestTimeService bestTimeService;

    public SchedulingController(BestTimeService bestTimeService) {
        this.bestTimeService = bestTimeService;
    }

    /**
     * Suggests buddy meeting windows: {@code GET /api/connections/{id} /best-times}, participants
     *     only.
     * Use a connection ID, not an invitation ID. Returns HTTP 200 with up to three distinct
     *     intervals
     * of at least 30 minutes, longest first, across courses where both students have preferences.
     * A missing connection or a caller outside the pair receives 404.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param id resource UUID from the path, with its meaning described above
     * @return response described above, with the documented HTTP status and payload
     */
    @GetMapping("/connections/{id}/best-times")
    public List<TimeSlot> connectionBestTimes(@CurrentUser AuthenticatedUser caller, @PathVariable UUID id) {
        return bestTimeService.forConnection(id, caller.id());
    }

    /**
     * Suggests group meeting windows: {@code GET /api/groups/{id}/best-times}, members only.
     * Returns HTTP 200 with up to three suggestions, member count and 30-minute heatmap cells from
     *     07:00 to 23:00.
     * Uses per-group overrides with profile fallback; adjacent blocks merge only when the available
     *     members match.
     * A missing group or a non-member receives 404.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param id resource UUID from the path, with its meaning described above
     * @return response described above, with the documented HTTP status and payload
     */
    @GetMapping("/groups/{id}/best-times")
    public GroupBestTimes groupBestTimes(@CurrentUser AuthenticatedUser caller, @PathVariable UUID id) {
        return bestTimeService.forGroup(id, caller.id());
    }
}