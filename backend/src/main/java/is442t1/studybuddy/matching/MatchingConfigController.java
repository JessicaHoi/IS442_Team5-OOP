package is442t1.studybuddy.matching;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import is442t1.studybuddy.auth.RequireRole;
import is442t1.studybuddy.model.enums.Role;

/** Lets the system administrator view and change how students are matched. */
@RestController
@RequestMapping("/api/admin/matching-config")
@RequireRole(Role.ADMIN)
public class MatchingConfigController {

    private final MatchingConfigService configService;

    public MatchingConfigController(MatchingConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    public MatchingConfigDto get() {
        return configService.getConfig();
    }

    @PutMapping
    public MatchingConfigDto update(@Valid @RequestBody MatchingConfigDto request) {
        return configService.updateConfig(request);
    }
}
