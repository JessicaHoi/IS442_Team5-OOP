package is442t1.studybuddy.telegram.api;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** The Telegram Bot API methods StudyBuddy uses. Every method throws {@link TelegramApiException} on failure. */
public interface TelegramClient {

    BotUser getMe();

    /** Polling and a webhook can't share a token, so this clears any webhook left behind. */
    void deleteWebhook();

    /** Waits up to {@code timeout} for updates. An {@code offset} confirms every earlier update. */
    List<Update> getUpdates(long offset, Duration timeout, List<String> allowedUpdates);

    void sendMessage(long chatId, String text);

    /** Returns the new message's id, so it can be pinned. */
    long sendMessage(long chatId, String text, ReplyMarkup markup);

    ChatInfo getChat(long chatId);

    ChatMember getChatMember(long chatId, long userId);

    /** A personal link: joining through it asks the bot's approval first. */
    ChatInviteLink createJoinRequestLink(long chatId, String name, Instant expiresAt);

    void revokeInviteLink(long chatId, String inviteLink);

    void approveJoinRequest(long chatId, long userId);

    void declineJoinRequest(long chatId, long userId);

    void banMember(long chatId, long userId);

    /** Lets a removed person come back later if they're invited again. Supergroups only. */
    void unbanMember(long chatId, long userId);

    void setTitle(long chatId, String title);

    void setDescription(long chatId, String description);

    void pinMessage(long chatId, long messageId);

    /** Sets what ordinary members may do. Admins aren't affected. */
    void setMemberPermissions(long chatId, boolean canSendMessages);

    void leaveChat(long chatId);
}
