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

    @GetMapping
    public GroupChatResponse view(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId) {
        return chatService.view(caller.id(), groupId);
    }

    @PostMapping("/setup")
    public TelegramLinkResponse startSetup(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId) {
        return chatService.startSetup(caller.id(), groupId);
    }

    @PostMapping("/verify")
    public GroupChatResponse verify(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId) {
        return chatService.verify(caller.id(), groupId);
    }

    @PostMapping("/invite")
    public GroupChatResponse invite(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @RequestBody(required = false) ChatInviteRequest request) {
        return chatService.invite(caller.id(), groupId, request != null && request.renew());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void detach(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId) {
        chatService.detach(caller.id(), groupId);
    }
}
