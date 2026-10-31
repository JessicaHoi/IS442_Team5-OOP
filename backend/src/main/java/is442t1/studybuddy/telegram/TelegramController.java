package is442t1.studybuddy.telegram;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import is442t1.studybuddy.auth.AuthenticatedUser;
import is442t1.studybuddy.auth.CurrentUser;
import is442t1.studybuddy.auth.RequireRole;
import is442t1.studybuddy.model.enums.Role;
import is442t1.studybuddy.telegram.link.TelegramLinkResponse;
import is442t1.studybuddy.telegram.link.TelegramLinkService;
import is442t1.studybuddy.telegram.link.TelegramStatusResponse;

/** Whether Telegram is available, and a student's connection to it. */
@RestController
@RequestMapping("/api")
public class TelegramController {

    private final TelegramLinkService linkService;
    private final TelegramProperties properties;

    public TelegramController(TelegramLinkService linkService, TelegramProperties properties) {
        this.linkService = linkService;
        this.properties = properties;
    }

    @GetMapping("/integrations")
    public IntegrationsResponse integrations() {
        return IntegrationsResponse.of(properties);
    }

    @GetMapping("/students/me/telegram")
    @RequireRole(Role.STUDENT)
    public TelegramStatusResponse status(@CurrentUser AuthenticatedUser caller) {
        return linkService.status(caller.id());
    }

    @PostMapping("/students/me/telegram/link")
    @RequireRole(Role.STUDENT)
    public TelegramLinkResponse createLink(@CurrentUser AuthenticatedUser caller) {
        return linkService.createLink(caller.id());
    }

    @DeleteMapping("/students/me/telegram")
    @RequireRole(Role.STUDENT)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlink(@CurrentUser AuthenticatedUser caller) {
        linkService.unlink(caller.id());
    }
}
