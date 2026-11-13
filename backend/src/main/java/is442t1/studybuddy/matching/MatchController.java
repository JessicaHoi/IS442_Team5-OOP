package is442t1.studybuddy.matching;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import is442t1.studybuddy.auth.AuthenticatedUser;
import is442t1.studybuddy.auth.CurrentUser;
import is442t1.studybuddy.auth.RequireRole;
import is442t1.studybuddy.model.enums.GroupPreference;
import is442t1.studybuddy.model.enums.StudyGoal;
import is442t1.studybuddy.model.enums.StudyMode;
import is442t1.studybuddy.model.enums.Role;

/**
 * Student-only matching searches with optional course, goal, day, meeting-mode and group-format
 *     filters.
 */
@RestController
@RequestMapping("/api/matches")
@RequireRole(Role.STUDENT)
public class MatchController {

    private final MatchingService matchingService;

    public MatchController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    /**
     * Ranks candidate students: {@code GET /api/matches}, student only.
     * Optional filters are course, goal, day (MON through SUN), studyMode and groupFormat.
     * Returns HTTP 200 ordered by the saved ranking strategy; inactive students and the caller are
     *     excluded.
     * Contact visibility follows accepted connections. Invalid filter values are 400.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param course optional course-code filter
     * @param goal optional study-goal filter
     * @param day optional three-letter day filter
     * @param studyMode optional meeting-mode filter
     * @param groupFormat optional group-format filter
     * @return response described above, with the documented HTTP status and payload
     */
    @GetMapping
    public List<MatchResult> search(
            @CurrentUser AuthenticatedUser caller,
            @RequestParam(required = false) String course,
            @RequestParam(required = false) StudyGoal goal,
            @RequestParam(required = false) String day,
            @RequestParam(required = false) StudyMode studyMode,
            @RequestParam(required = false) GroupPreference groupFormat) {
        return matchingService.search(caller.id(), course, goal, day, studyMode, groupFormat);
    }
}
