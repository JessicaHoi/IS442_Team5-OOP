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

/** Endpoint for searching ranked study buddy matches. */
@RestController
@RequestMapping("/api/matches")
@RequireRole(Role.STUDENT)
public class MatchController {

    private final MatchingService matchingService;

    /**
     * Creates the matching controller.
     *
     * @param matchingService the matching service
     */
    public MatchController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    /**
     * Searches for matches with optional filters.
     *
     * @param caller the authenticated student
     * @param course optional course code
     * @param goal optional study goal
     * @param day optional day filter
     * @param studyMode optional meeting mode
     * @param groupFormat optional group format
     * @return ranked matching students
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
