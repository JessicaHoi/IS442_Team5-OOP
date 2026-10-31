package is442t1.studybuddy.telegram.chat;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import is442t1.studybuddy.common.exception.BusinessRuleException;
import is442t1.studybuddy.common.exception.ExternalServiceException;
import is442t1.studybuddy.telegram.TelegramProperties;
import is442t1.studybuddy.telegram.api.ChatInviteLink;
import is442t1.studybuddy.telegram.api.ChatMember;
import is442t1.studybuddy.telegram.api.ReplyMarkup;
import is442t1.studybuddy.telegram.api.TelegramApiException;
import is442t1.studybuddy.telegram.api.TelegramClient;
import is442t1.studybuddy.telegram.chat.ChatStore.ChatRef;
import is442t1.studybuddy.telegram.chat.ChatStore.InviteState;
import is442t1.studybuddy.telegram.chat.ChatStore.Removal;
import is442t1.studybuddy.telegram.link.TelegramLinkResponse;

/**
 * Keeps study groups and their Telegram chats in step. Deliberately not transactional: it alternates short
 * {@link ChatStore} transactions with Telegram calls.
 */
@Service
public class GroupChatService {

    static final String INVITE_NAME_PREFIX = "inv_";
    static final String WELCOME_DM = "You're in! Say hi to your study group.";
    private static final Logger LOG = LoggerFactory.getLogger(GroupChatService.class);
    private static final int TITLE_LIMIT = 128;
    private static final int DESCRIPTION_LIMIT = 255;
    private static final String TELEGRAM_UNAVAILABLE = "Telegram couldn't do that right now. Please try again.";

    private final ChatStore store;
    private final TelegramClient client;
    private final TelegramProperties properties;
    private final SecureRandom random = new SecureRandom();

    public GroupChatService(ChatStore store, TelegramClient client, TelegramProperties properties) {
        this.store = store;
        this.client = client;
        this.properties = properties;
    }

    public GroupChatResponse view(UUID viewerId, UUID groupId) {
        return store.view(viewerId, groupId);
    }

    public TelegramLinkResponse startSetup(UUID leaderId, UUID groupId) {
        requireEnabled();
        return store.startSetup(leaderId, groupId);
    }

    /** Re-checks the bot's rights, for a leader who has just fixed them. */
    public GroupChatResponse verify(UUID leaderId, UUID groupId) {
        requireEnabled();
        ChatRef chat = store.requireLedChat(leaderId, groupId);
        try {
            refreshRights(chat);
        } catch (TelegramApiException exception) {
            throw unavailable(exception);
        }
        return store.view(leaderId, groupId);
    }

    /** Reads the bot's rights and, the first time they're enough, finishes setting up the chat. */
    void refreshRights(ChatRef chat) {
        ChatMember bot = onChat(chat, chatId -> client.getChatMember(chatId, properties.botId()));
        long current = store.findByGroup(chat.groupId()).map(ChatRef::chatId).orElse(chat.chatId());
        store.updateRights(current, ChatRights.of(bot)).ifPresent(this::finishSetup);
    }

    /** Sets the chat up as far as the bot's rights allow. Nothing here is essential, so failures only get logged. */
    void finishSetup(ChatRef chat) {
        attempt("set member permissions", () -> runOnChat(chat, chatId -> client.setMemberPermissions(chatId, true)));
        if (!chat.lacks("CHANGE_INFO")) {
            applyTitleAndDescription(chat);
        }
        attempt("post the welcome message", () -> {
            String text = "This chat is linked to the study group " + chat.groupName()
                    + " on StudyBuddy. The StudyBuddy bot can see messages here but doesn't store them.";
            long messageId = send(chat, text);
            if (!chat.lacks("PIN_MESSAGES")) {
                runOnChat(chat, chatId -> client.pinMessage(chatId, messageId));
            }
        });
    }

    private void applyTitleAndDescription(ChatRef chat) {
        attempt("rename the chat", () -> runOnChat(chat, chatId -> client.setTitle(chatId, truncate(chat.groupName() + " · " + chat.courseCode(), TITLE_LIMIT))));
        if (chat.description() != null && !chat.description().isBlank()) {
            attempt("set the chat description", () -> runOnChat(chat, chatId -> client.setDescription(chatId, truncate(chat.description(), DESCRIPTION_LIMIT))));
        }
    }

    /** Unlinks the chat: invites stop working and the bot leaves. The chat itself stays with its members. */
    public void detach(UUID leaderId, UUID groupId) {
        requireEnabled();
        ChatRef chat = store.requireLedChat(leaderId, groupId);
        if (chat.status() != GroupChatStatus.DETACHED) {
            store.activeLinks(chat.id()).forEach(link -> attempt("revoke an invite", () -> runOnChat(chat, chatId -> client.revokeInviteLink(chatId, link))));
            attempt("say goodbye", () -> send(chat, "This chat is no longer linked to StudyBuddy. Goodbye!"));
            attempt("leave the chat", () -> runOnChat(chat, client::leaveChat));
        }
        store.delete(chat.id());
    }

    /**
     * The student's way into the chat. {@code renew} replaces their link, or, once someone has joined with it,
     * removes that person: it's the "Not you?" button for an invite that reached the wrong hands.
     */
    public GroupChatResponse invite(UUID studentId, UUID groupId, boolean renew) {
        requireEnabled();
        InviteState state = store.inviteState(studentId, groupId);
        ChatRef chat = state.chat();
        try {
            if (renew) {
                discardCurrent(state, studentId);
            } else if (state.state() == ChatMemberState.JOINED || state.inviteStillValid()) {
                return store.view(studentId, groupId);
            }
            if ((!renew || state.state() != ChatMemberState.JOINED) && state.linkedTelegramUserId() != null
                    && onChat(chat, chatId -> client.getChatMember(chatId, state.linkedTelegramUserId())).isInChat()) {
                store.saveJoined(chat.id(), studentId, state.linkedTelegramUserId(), null);
                return store.view(studentId, groupId);
            }
            issueInvite(chat, studentId);
        } catch (TelegramApiException exception) {
            throw unavailable(exception);
        }
        return store.view(studentId, groupId);
    }

    private void discardCurrent(InviteState state, UUID studentId) {
        ChatRef chat = state.chat();
        if (state.inviteLink() != null) {
            runOnChat(chat, chatId -> client.revokeInviteLink(chatId, state.inviteLink()));
        }
        if (state.state() == ChatMemberState.JOINED && state.telegramUserId() != null) {
            if (state.telegramUserId().equals(state.linkedTelegramUserId())) {
                throw new BusinessRuleException("You're in this chat with your connected Telegram account.");
            }
            kick(chat.chatId(), chat.chatType(), state.telegramUserId());
            store.saveRemoved(chat.id(), studentId);
        }
    }

    private String issueInvite(ChatRef chat, UUID studentId) {
        byte[] bytes = new byte[4];
        random.nextBytes(bytes);
        String name = INVITE_NAME_PREFIX + HexFormat.of().formatHex(bytes);
        Instant expiresAt = Instant.now().plus(properties.inviteTtl());
        ChatInviteLink link = onChat(chat, chatId -> client.createJoinRequestLink(chatId, name, expiresAt));
        store.saveInvite(chat.id(), studentId, link.inviteLink(), name, expiresAt);
        return link.inviteLink();
    }

    /** A new member's row starts afresh. If they connected Telegram, the bot messages them their invite too. */
    public void onMemberJoined(UUID groupId, UUID studentId, Long messageableTelegramUserId) {
        store.resetForNewMember(groupId, studentId)
                .filter(chat -> chat.status() == GroupChatStatus.ACTIVE)
                .ifPresent(chat -> {
                    if (messageableTelegramUserId == null) {
                        return;
                    }
                    String link = issueInvite(chat, studentId);
                    client.sendMessage(messageableTelegramUserId, "You've joined " + chat.groupName() + " on StudyBuddy. Join the group chat here:",
                            ReplyMarkup.LinkButton.of("Join the group chat", link));
                });
    }

    /** Takes someone who is no longer in the study group out of the chat. A refusal is shown to the leader. */
    public void onMemberLeft(UUID groupId, UUID studentId) {
        store.removalFor(groupId, studentId).ifPresent(this::remove);
    }

    /** Runs after the account has gone, from what was read before, so there's no row left to update. */
    public void onAccountDeleted(List<Removal> removals) {
        for (Removal removal : removals) {
            if (removal.inviteLink() != null) {
                attempt("revoke an invite", () -> client.revokeInviteLink(removal.chatId(), removal.inviteLink()));
            }
            if (removal.telegramUserId() != null && removal.state() != ChatMemberState.REMOVED) {
                attempt("remove a deleted account from a chat", () -> kick(removal.chatId(), removal.chatType(), removal.telegramUserId()));
            }
        }
    }

    private void remove(Removal removal) {
        if (removal.inviteLink() != null) {
            attempt("revoke an invite", () -> client.revokeInviteLink(removal.chatId(), removal.inviteLink()));
        }
        if (removal.telegramUserId() == null || removal.state() == ChatMemberState.REMOVED || removal.state() == ChatMemberState.LEFT) {
            store.saveRemoved(removal.groupChatId(), removal.studentId());
            return;
        }
        try {
            kick(removal.chatId(), removal.chatType(), removal.telegramUserId());
            store.saveRemoved(removal.groupChatId(), removal.studentId());
        } catch (TelegramApiException exception) {
            LOG.warn("Could not remove someone from a chat: {}", exception.getMessage());
            store.saveRemovalFailed(removal.groupChatId(), removal.studentId());
        }
    }

    /** Banning is how a bot removes someone. In a supergroup the unban lets them be invited back later. */
    private void kick(long chatId, String chatType, long telegramUserId) {
        client.banMember(chatId, telegramUserId);
        if ("supergroup".equals(chatType)) {
            client.unbanMember(chatId, telegramUserId);
        }
    }

    /** Makes the chat read-only and stops its invites. Members keep the history. */
    public void onGroupClosed(UUID groupId) {
        store.findByGroup(groupId).filter(chat -> chat.status() != GroupChatStatus.DETACHED).ifPresent(chat -> {
            store.closeChat(chat.id()).forEach(link -> attempt("revoke an invite", () -> runOnChat(chat, chatId -> client.revokeInviteLink(chatId, link))));
            attempt("post the closing message", () -> send(chat, "This study group has closed, so the chat is now read-only."));
            attempt("make the chat read-only", () -> runOnChat(chat, chatId -> client.setMemberPermissions(chatId, false)));
        });
    }

    public void onGroupRenamed(UUID groupId) {
        store.findByGroup(groupId).filter(chat -> chat.status() == GroupChatStatus.ACTIVE && !chat.lacks("CHANGE_INFO"))
                .ifPresent(this::applyTitleAndDescription);
    }

    public void onLeadershipTransferred(UUID groupId, String newLeaderName) {
        store.findByGroup(groupId).filter(chat -> chat.status() == GroupChatStatus.ACTIVE).ifPresent(chat ->
                attempt("post the leadership notice", () -> send(chat, newLeaderName + " is now the leader of this study group.")));
    }

    long send(ChatRef chat, String text) {
        return onChat(chat, chatId -> client.sendMessage(chatId, text, new ReplyMarkup.RemoveKeyboard(true)));
    }

    /** Runs a call on the chat. If the group has since been upgraded, Telegram says so: save the new id and try once more. */
    private <T> T onChat(ChatRef chat, LongFunction<T> call) {
        long chatId = store.findByGroup(chat.groupId()).map(ChatRef::chatId).orElse(chat.chatId());
        try {
            return call.apply(chatId);
        } catch (TelegramApiException exception) {
            if (exception.migrateToChatId() == null) {
                throw exception;
            }
            store.migrate(chatId, exception.migrateToChatId());
            return call.apply(exception.migrateToChatId());
        }
    }

    private void runOnChat(ChatRef chat, LongConsumer call) {
        onChat(chat, chatId -> {
            call.accept(chatId);
            return null;
        });
    }

    private static void attempt(String what, Runnable action) {
        try {
            action.run();
        } catch (TelegramApiException exception) {
            LOG.warn("Could not {}: {}", what, exception.getMessage());
        }
    }

    private void requireEnabled() {
        if (!properties.enabled()) {
            throw new BusinessRuleException(TelegramProperties.OFF_MESSAGE);
        }
    }

    private static ExternalServiceException unavailable(TelegramApiException exception) {
        LOG.warn("A Telegram call failed: {}", exception.getMessage());
        return new ExternalServiceException(TELEGRAM_UNAVAILABLE);
    }

    private static String truncate(String text, int limit) {
        return text.length() <= limit ? text : text.substring(0, limit - 1) + "…";
    }
}
