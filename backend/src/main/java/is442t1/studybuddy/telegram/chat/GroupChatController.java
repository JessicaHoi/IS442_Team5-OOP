package is442t1.studybuddy.telegram.chat;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import is442t1.studybuddy.auth.AuthenticatedUser;
import is442t1.studybuddy.auth.CurrentUser;
import is442t1.studybuddy.auth.RequireRole;
import is442t1.studybuddy.model.enums.Role;
import is442t1.studybuddy.telegram.link.TelegramLinkResponse;

/** A study group's Telegram chat: setup for the leader, invites for members. */
@RestController
@RequestMapping("/api/groups/{groupId}/chat")
@RequireRole(Role.STUDENT)
public class GroupChatController {

    private final GroupChatService chatService;

    public GroupChatController(GroupChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Reads managed chat state: {@code GET /api/groups/{groupId}/chat}, members only.
     * Returns HTTP 200; leaders also see missing rights and member chat states.
     * Disabled Telegram returns NOT_LINKED; a non-member is 403.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param groupId study group UUID from the path
     * @return response described above, with the documented HTTP status and payload
     */
    @GetMapping
    public GroupChatResponse view(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId) {
        return chatService.view(caller.id(), groupId);
    }

    /**
     * Creates a setup URL: {@code POST /api/groups/{groupId}/chat/setup}, leader only.
     * Returns HTTP 200 with URL and expiry; disabled Telegram, a closed group or an existing
     *     non-detached chat is 409.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param groupId study group UUID from the path
     * @return response described above, with the documented HTTP status and payload
     */
    @PostMapping("/setup")
    public TelegramLinkResponse startSetup(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId) {
        return chatService.startSetup(caller.id(), groupId);
    }

    /**
     * Rechecks the bot's chat permissions: {@code POST /api/groups/{groupId} /chat/verify}, leader
     *     only.
     * Returns HTTP 200 with chat state; Telegram failures are 503 and disabled Telegram is 409.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param groupId study group UUID from the path
     * @return response described above, with the documented HTTP status and payload
     */
    @PostMapping("/verify")
    public GroupChatResponse verify(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId) {
        return chatService.verify(caller.id(), groupId);
    }

    /**
     * Creates or renews a personal chat invite: {@code POST /api/groups/{groupId} /chat/invite},
     *     active members only.
     * Optional body {@code {"renew":true}} replaces an invite or removes the person who used it.
     * Returns HTTP 200 with the caller's chat state and invite, when applicable.
     * A closed/unusable chat or conflicting Telegram identity is 409; Telegram failures are 503.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param groupId study group UUID from the path
     * @param request JSON body described above
     * @return response described above, with the documented HTTP status and payload
     */
    @PostMapping("/invite")
    public GroupChatResponse invite(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @RequestBody(required = false) ChatInviteRequest request) {
        return chatService.invite(caller.id(), groupId, request != null && request.renew());
    }

    /**
     * Unlinks the bot-managed chat: {@code DELETE /api/groups/{groupId}/chat}, leader only.
     * Attempts to revoke invites and leave Telegram while preserving the Telegram chat itself.
     * Returns HTTP 204; a non-leader is 403 and disabled Telegram is 409.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param groupId study group UUID from the path
     */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void detach(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId) {
        chatService.detach(caller.id(), groupId);
    }
}
