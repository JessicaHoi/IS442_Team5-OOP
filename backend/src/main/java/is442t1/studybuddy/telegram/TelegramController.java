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

    /**
     * Creates the Telegram integration endpoints.
     *
     * @param linkService operations for student Telegram links
     * @param properties Telegram bot configuration
     */
    public TelegramController(TelegramLinkService linkService, TelegramProperties properties) {
        this.linkService = linkService;
        this.properties = properties;
    }

    /**
     * Reports whether optional Telegram integration is enabled.
     *
     * @return integration availability and configured Telegram bot username
     */
    @GetMapping("/integrations")
    public IntegrationsResponse integrations() {
        return IntegrationsResponse.of(properties);
    }

    /**
     * Returns the Telegram connection for the signed-in student.
     *
     * @param caller the authenticated student
     * @return the student's Telegram connection status
     */
    @GetMapping("/students/me/telegram")
    @RequireRole(Role.STUDENT)
    public TelegramStatusResponse status(@CurrentUser AuthenticatedUser caller) {
        return linkService.status(caller.id());
    }

    /**
     * Creates a one-time Telegram link for the signed-in student.
     *
     * @param caller the authenticated student
     * @return the Telegram deep link and its expiration time
     */
    @PostMapping("/students/me/telegram/link")
    @RequireRole(Role.STUDENT)
    public TelegramLinkResponse createLink(@CurrentUser AuthenticatedUser caller) {
        return linkService.createLink(caller.id());
    }

    /**
     * Removes the signed-in student's Telegram connection.
     *
     * @param caller the authenticated student
     */
    @DeleteMapping("/students/me/telegram")
    @RequireRole(Role.STUDENT)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlink(@CurrentUser AuthenticatedUser caller) {
        linkService.unlink(caller.id());
    }
}
