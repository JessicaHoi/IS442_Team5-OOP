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

    /**
     * Creates the group-chat endpoints.
     *
     * @param chatService operations for linked group chats
     */
    public GroupChatController(GroupChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Returns the caller's view of a group's Telegram chat.
     *
     * @param caller the authenticated student
     * @param groupId StudyBuddy group identifier
     * @return the chat status and member-specific details
     */
    @GetMapping
    public GroupChatResponse view(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId) {
        return chatService.view(caller.id(), groupId);
    }

    /**
     * Creates a one-time Telegram link for the group leader to choose a chat.
     *
     * @param caller the authenticated student
     * @param groupId StudyBuddy group identifier
     * @return the Telegram setup link and its expiration time
     */
    @PostMapping("/setup")
    public TelegramLinkResponse startSetup(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId) {
        return chatService.startSetup(caller.id(), groupId);
    }

    /**
     * Re-checks the linked bot's Telegram permissions.
     *
     * @param caller the authenticated student; only the group leader may verify setup
     * @param groupId StudyBuddy group identifier
     * @return the updated chat status
     */
    @PostMapping("/verify")
    public GroupChatResponse verify(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId) {
        return chatService.verify(caller.id(), groupId);
    }

    /**
     * Creates or renews the caller's personal invite to the linked chat.
     *
     * @param caller the authenticated student
     * @param groupId StudyBuddy group identifier
     * @param request optional invite renewal request; absent requests do not renew
     * @return the updated chat status and the caller's invite state
     */
    @PostMapping("/invite")
    public GroupChatResponse invite(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @RequestBody(required = false) ChatInviteRequest request) {
        return chatService.invite(caller.id(), groupId, request != null && request.renew());
    }

    /**
     * Unlinks the Telegram chat from the group.
     *
     * @param caller the authenticated student; only the group leader may detach
     * @param groupId StudyBuddy group identifier
     */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void detach(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId) {
        chatService.detach(caller.id(), groupId);
    }
}
