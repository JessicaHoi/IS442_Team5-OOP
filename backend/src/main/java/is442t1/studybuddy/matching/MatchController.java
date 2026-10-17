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

@RestController
@RequestMapping("/api/matches")
@RequireRole(Role.STUDENT)
public class MatchController {

    private final MatchingService matchingService;

    public MatchController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

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
