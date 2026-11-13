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

    /**
     * Reads optional integration capabilities: {@code GET /api/integrations} , either authenticated
     *     role.
     * Returns HTTP 200 with Telegram enabled state and bot username, null when disabled.
     *
     * @return response described above, with the documented HTTP status and payload
     */
    @GetMapping("/integrations")
    public IntegrationsResponse integrations() {
        return IntegrationsResponse.of(properties);
    }

    /**
     * Reads the caller's Telegram connection: {@code GET /api/students/me/telegram}, student only.
     * Returns HTTP 200; linked is false when Telegram is disabled. No other student's connection is
     *     exposed.
     *
     * @param caller authenticated caller injected from the bearer token
     * @return response described above, with the documented HTTP status and payload
     */
    @GetMapping("/students/me/telegram")
    @RequireRole(Role.STUDENT)
    public TelegramStatusResponse status(@CurrentUser AuthenticatedUser caller) {
        return linkService.status(caller.id());
    }

    /**
     * Creates a personal account-link URL: {@code POST /api/students/me/telegram/link} , student
     *     only.
     * Returns HTTP 200 with URL and expiry, ten minutes by default; replaces earlier personal
     *     links.
     * The URL is single-use. Disabled Telegram produces 409.
     *
     * @param caller authenticated caller injected from the bearer token
     * @return response described above, with the documented HTTP status and payload
     */
    @PostMapping("/students/me/telegram/link")
    @RequireRole(Role.STUDENT)
    public TelegramLinkResponse createLink(@CurrentUser AuthenticatedUser caller) {
        return linkService.createLink(caller.id());
    }

    /**
     * Disconnects the caller's Telegram account: {@code DELETE /api/students/me/telegram} , student
     *     only.
     * Returns HTTP 204 even when there is no connection.
     *
     * @param caller authenticated caller injected from the bearer token
     */
    @DeleteMapping("/students/me/telegram")
    @RequireRole(Role.STUDENT)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlink(@CurrentUser AuthenticatedUser caller) {
        linkService.unlink(caller.id());
    }
}
