package is442t1.studybuddy.telegram.chat;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import is442t1.studybuddy.telegram.TelegramProperties;
import is442t1.studybuddy.telegram.api.ChatInfo;
import is442t1.studybuddy.telegram.api.ChatInviteLink;
import is442t1.studybuddy.telegram.api.ChatMember;
import is442t1.studybuddy.telegram.api.ReplyMarkup;
import is442t1.studybuddy.telegram.api.TelegramApiException;
import is442t1.studybuddy.telegram.api.TelegramClient;
import is442t1.studybuddy.telegram.api.Update;
import is442t1.studybuddy.telegram.chat.ChatStore.ChatRef;
import is442t1.studybuddy.telegram.chat.ChatStore.JoinOutcome;
import is442t1.studybuddy.telegram.link.TelegramLinkService;

/** What the bot does about chat setup, join requests and changes it sees in a chat. */
@Component
@ConditionalOnProperty(prefix = "studybuddy.telegram", name = "enabled", havingValue = "true")
public class ChatBotHandler {

    static final String SETUP_LINK_NOT_VALID = "This setup link is no longer valid. Open your group in StudyBuddy and start again.";
    static final String CHOOSE_CHAT = "Choose a chat for your study group. Create a new group, and I'll name it for you. Or use a group you already own: I'll rename it after your study group and reset what members are allowed to do. Existing members stay and will see session details, and StudyBuddy only ever removes people it invited.";
    static final String NOT_ADMIN = "You need to be an admin of that group to link it. Try again from StudyBuddy with a group you own.";
    static final String ALREADY_LINKED = "That chat is already linked to another study group. Your setup link still works: tap a button below to choose a different one.";
    static final String LINKED = "Linked ✓ You can go back to StudyBuddy.";
    static final String MAKE_ME_ADMIN = "Linked, but I can't manage the chat yet. In the group, open Administrators, add me as an admin, and allow adding and banning users. StudyBuddy will update by itself.";
    static final String COULD_NOT_JOIN = "I couldn't let you into the chat just now. Open the group in StudyBuddy to get a new invite.";
    static final String COULD_NOT_READ_CHAT = "I couldn't read that group from Telegram. Your setup link still works: tap Use an existing group and choose the same group.";
    private static final Logger LOG = LoggerFactory.getLogger(ChatBotHandler.class);

    /** What the bot needs to know about a chat a leader chose. */
    private record ChosenChat(ChatMember chooser, ChatInfo chat, ChatRights rights) {
    }

    private static final Map<String, Boolean> REQUESTED_RIGHTS = Map.ofEntries(
            Map.entry("is_anonymous", false),
            Map.entry("can_manage_chat", true),
            Map.entry("can_delete_messages", false),
            Map.entry("can_manage_video_chats", false),
            Map.entry("can_restrict_members", true),
            Map.entry("can_promote_members", false),
            Map.entry("can_change_info", true),
            Map.entry("can_invite_users", true),
            Map.entry("can_post_stories", false),
            Map.entry("can_edit_stories", false),
            Map.entry("can_delete_stories", false),
            Map.entry("can_pin_messages", true));

    private final TelegramClient client;
    private final TelegramLinkService linkService;
    private final ChatStore store;
    private final GroupChatService chatService;
    private final TelegramProperties properties;

    ChatBotHandler(TelegramClient client, TelegramLinkService linkService, ChatStore store, GroupChatService chatService,
            TelegramProperties properties) {
        this.client = client;
        this.linkService = linkService;
        this.store = store;
        this.chatService = chatService;
        this.properties = properties;
    }

    /** A leader opened their setup link: show the buttons, which they can do as often as they need. */
    public void openSetup(long privateChatId, long telegramUserId, String token) {
        Optional<Integer> requestId = linkService.openGroupSetup(token, telegramUserId);
        if (requestId.isEmpty()) {
            client.sendMessage(privateChatId, SETUP_LINK_NOT_VALID);
            return;
        }
        client.sendMessage(privateChatId, CHOOSE_CHAT, setupButtons(requestId.get()));
    }

    /** Two ids in one message, as Telegram requires. Only the first asks for rights up front, which hides most existing groups. */
    private static ReplyMarkup.Keyboard setupButtons(int requestId) {
        ReplyMarkup.ChatRequest create = new ReplyMarkup.ChatRequest(requestId, false, true, REQUESTED_RIGHTS, REQUESTED_RIGHTS);
        ReplyMarkup.ChatRequest existing = new ReplyMarkup.ChatRequest(requestId + 1, false, true, null, null);
        return new ReplyMarkup.Keyboard(List.of(
                List.of(new ReplyMarkup.ChatRequestButton("Create a new group", create)),
                List.of(new ReplyMarkup.ChatRequestButton("Use an existing group", existing))), true, true);
    }

    /** The leader picked a chat. Only the person who opened the link, answering its own buttons, gets this far. */
    public void chatChosen(long privateChatId, long telegramUserId, String firstName, int requestId, long chatId) {
        Optional<ChosenChat> read = readChosenChat(chatId, telegramUserId);
        if (read.isEmpty()) {
            client.sendMessage(privateChatId, COULD_NOT_READ_CHAT);
            return;
        }
        ChosenChat chosen = read.get();
        if (!chosen.chooser().isOwnerOrAdministrator()) {
            client.sendMessage(privateChatId, NOT_ADMIN, new ReplyMarkup.RemoveKeyboard(true));
            return;
        }
        Optional<UUID> groupId = linkService.groupInSetup(telegramUserId, requestId);
        if (groupId.isEmpty()) {
            client.sendMessage(privateChatId, SETUP_LINK_NOT_VALID, new ReplyMarkup.RemoveKeyboard(true));
            return;
        }
        if (store.linkedToAnotherGroup(chosen.chat().id(), groupId.get())) {
            client.sendMessage(privateChatId, ALREADY_LINKED);
            return;
        }
        ChatRef saved = store.saveLinkedChat(groupId.get(), chosen.chat().id(), chosen.chat().type(), chosen.chat().title(),
                telegramUserId, firstName, chosen.rights());
        linkService.useUpGroupSetup(telegramUserId, requestId);
        if (saved.status() == GroupChatStatus.ACTIVE) {
            chatService.finishSetup(saved);
            client.sendMessage(privateChatId, LINKED, new ReplyMarkup.RemoveKeyboard(true));
        } else {
            client.sendMessage(privateChatId, MAKE_ME_ADMIN, new ReplyMarkup.RemoveKeyboard(true));
        }
    }

    /**
     * Reads the chosen chat, before the setup link is used up, so a failure leaves the leader able to try again.
     * Telegram upgrades a new group as it creates it and can send the old id, so the upgrade is followed here.
     */
    private Optional<ChosenChat> readChosenChat(long chatId, long telegramUserId) {
        try {
            return Optional.of(readChat(chatId, telegramUserId));
        } catch (TelegramApiException exception) {
            if (exception.migrateToChatId() == null) {
                LOG.warn("Could not read a chosen chat: {}", exception.getMessage());
                return Optional.empty();
            }
            return readUpgradedChat(exception.migrateToChatId(), telegramUserId);
        }
    }

    private Optional<ChosenChat> readUpgradedChat(long chatId, long telegramUserId) {
        try {
            return Optional.of(readChat(chatId, telegramUserId));
        } catch (TelegramApiException exception) {
            LOG.warn("Could not read an upgraded chat: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    private ChosenChat readChat(long chatId, long telegramUserId) {
        return new ChosenChat(client.getChatMember(chatId, telegramUserId), client.getChat(chatId),
                ChatRights.of(client.getChatMember(chatId, properties.botId())));
    }

    /** Someone used an invite link. */
    public void joinRequested(Update.ChatJoinRequest request) {
        long chatId = request.chat().id();
        long userId = request.from().id();
        ChatInviteLink link = request.inviteLink();
        JoinOutcome outcome = store.decideJoin(chatId, link == null ? null : link.name(), link == null ? null : link.inviteLink(), userId);
        switch (outcome.decision()) {
            case JoinDecision.Ignore ignored -> {
            }
            case JoinDecision.Decline decline -> {
                client.declineJoinRequest(chatId, userId);
                messageQuietly(request.userChatId(), decline.reason());
            }
            case JoinDecision.Approve approve -> {
                // Saved first: if this fails nobody has been let in, whereas the other order leaves someone in the chat with no record of them.
                store.saveJoined(outcome.chat().id(), outcome.studentId(), userId, displayName(request.from()));
                try {
                    approve(chatId, userId);
                } catch (TelegramApiException exception) {
                    store.saveJoinFailed(outcome.chat().id(), outcome.studentId());
                    messageQuietly(request.userChatId(), COULD_NOT_JOIN);
                    throw exception;
                }
                if (outcome.link() != null) {
                    revokeQuietly(chatId, outcome.link());
                }
                messageQuietly(request.userChatId(), GroupChatService.WELCOME_DM);
            }
        }
    }

    /** If they've already been let in, the request has done its job. */
    private void approve(long chatId, long userId) {
        try {
            client.approveJoinRequest(chatId, userId);
        } catch (TelegramApiException exception) {
            if (exception.getMessage() == null || !exception.getMessage().contains("USER_ALREADY_PARTICIPANT")) {
                throw exception;
            }
        }
    }

    /** The bot itself was added, promoted or removed. */
    public void botChanged(long chatId, ChatMember now) {
        if ("left".equals(now.status()) || "kicked".equals(now.status())) {
            store.markDetached(chatId);
            return;
        }
        store.updateRights(chatId, ChatRights.of(now)).ifPresent(chatService::finishSetup);
    }

    public void memberChanged(long chatId, long telegramUserId, boolean inChat) {
        store.followChatMember(chatId, telegramUserId, inChat);
    }

    public void chatMigrated(long oldChatId, long newChatId) {
        store.migrate(oldChatId, newChatId);
    }

    private static String displayName(Update.Sender sender) {
        return sender.username() != null ? "@" + sender.username() : sender.firstName();
    }

    private void messageQuietly(long chatId, String text) {
        try {
            client.sendMessage(chatId, text);
        } catch (TelegramApiException exception) {
            LOG.warn("Could not send a private message: {}", exception.getMessage());
        }
    }

    private void revokeQuietly(long chatId, String link) {
        try {
            client.revokeInviteLink(chatId, link);
        } catch (TelegramApiException exception) {
            LOG.warn("Could not revoke a used invite: {}", exception.getMessage());
        }
    }
}
