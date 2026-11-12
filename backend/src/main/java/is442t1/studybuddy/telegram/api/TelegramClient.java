package is442t1.studybuddy.telegram.api;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * The Telegram Bot API methods StudyBuddy uses. Every method throws {@link TelegramApiException} on failure.
 */
public interface TelegramClient {

    /**
     * Returns the bot's Telegram identity.
     *
     * @return the Telegram bot identity
     */
    BotUser getMe();

    /** Polling and a webhook can't share a token, so this clears any webhook left behind. */
    void deleteWebhook();

    /**
     * Waits up to {@code timeout} for updates. An {@code offset} confirms every earlier update.
     *
     * @param offset the first update identifier to return; preceding updates are confirmed
     * @param timeout maximum long-poll wait
     * @param allowedUpdates update kinds requested from Telegram
     * @return the received updates, in Telegram's order
     */
    List<Update> getUpdates(long offset, Duration timeout, List<String> allowedUpdates);

    /**
     * Sends a text message.
     *
     * @param chatId destination chat
     * @param text message text
     * @return the new message identifier
     */
    long sendMessage(long chatId, String text);

    /**
     * Sends a text message with keyboard markup.
     *
     * @param chatId destination chat
     * @param text message text
     * @param markup keyboard markup
     * @return the new message identifier
     */
    long sendMessage(long chatId, String text, ReplyMarkup markup);

    /**
     * Sends text with message entities, such as Telegram mentions.
     *
     * @param chatId destination chat
     * @param text message text
     * @param entities formatting or mention entities in the text
     * @return the new message identifier
     */
    long sendMessage(long chatId, String text, List<MessageEntity> entities);

    /**
     * Replies to a message, allowing delivery even if the replied-to message was deleted.
     *
     * @param chatId destination chat
     * @param replyToMessageId message being replied to
     * @param text reply text
     * @param entities formatting or mention entities in the text
     * @return the new message identifier
     */
    long sendReply(long chatId, long replyToMessageId, String text, List<MessageEntity> entities);

    /**
     * Changes a message's text and removes its buttons. Editing never notifies anyone.
     *
     * @param chatId chat containing the message
     * @param messageId message to edit
     * @param text replacement text
     */
    void editMessage(long chatId, long messageId, String text);

    /**
     * Changes a message's text and keyboard. Editing never notifies anyone.
     *
     * @param chatId chat containing the message
     * @param messageId message to edit
     * @param text replacement text
     * @param markup replacement keyboard
     */
    void editMessage(long chatId, long messageId, String text, ReplyMarkup markup);

    /**
     * Returns Telegram's details for a chat.
     *
     * @param chatId chat to look up
     * @return the chat details
     */
    ChatInfo getChat(long chatId);

    /**
     * Returns a user's membership and available rights in a chat.
     *
     * @param chatId chat to inspect
     * @param userId user to inspect
     * @return the member status and permissions
     */
    ChatMember getChatMember(long chatId, long userId);

    /**
     * Creates an expiring personal invite link that requires bot approval for a join request.
     *
     * @param chatId destination chat
     * @param name link label
     * @param expiresAt link expiration time
     * @return the created invite link
     */
    ChatInviteLink createJoinRequestLink(long chatId, String name, Instant expiresAt);

    /**
     * Revokes an invite link.
     *
     * @param chatId chat that owns the link
     * @param inviteLink invite URL to revoke
     */
    void revokeInviteLink(long chatId, String inviteLink);

    /**
     * Approves a pending request to join a chat.
     *
     * @param chatId chat requested
     * @param userId user whose request is approved
     */
    void approveJoinRequest(long chatId, long userId);

    /**
     * Declines a pending request to join a chat.
     *
     * @param chatId chat requested
     * @param userId user whose request is declined
     */
    void declineJoinRequest(long chatId, long userId);

    /**
     * Bans a member from a chat.
     *
     * @param chatId affected chat
     * @param userId member to ban
     */
    void banMember(long chatId, long userId);

    /**
     * Removes a member's ban, allowing them to be invited again. Only supported for supergroups.
     *
     * @param chatId affected supergroup
     * @param userId user to unban
     */
    void unbanMember(long chatId, long userId);

    /**
     * Changes the chat title.
     *
     * @param chatId affected chat
     * @param title replacement title
     */
    void setTitle(long chatId, String title);

    /**
     * Changes the chat description.
     *
     * @param chatId affected chat
     * @param description replacement description
     */
    void setDescription(long chatId, String description);

    /**
     * Pins a message without notifying chat members.
     *
     * @param chatId chat containing the message
     * @param messageId message to pin
     */
    void pinMessage(long chatId, long messageId);

    /**
     * Unpins a message.
     *
     * @param chatId chat containing the message
     * @param messageId message to unpin
     */
    void unpinMessage(long chatId, long messageId);

    /**
     * Sets whether ordinary members may send messages and other content. Admins aren't affected.
     *
     * @param chatId affected chat
     * @param canSendMessages whether ordinary members may send content
     */
    void setMemberPermissions(long chatId, boolean canSendMessages);

    /**
     * Makes the bot leave a chat.
     *
     * @param chatId chat to leave
     */
    void leaveChat(long chatId);

    /**
     * Answers a button press to stop Telegram's loading indicator.
     *
     * @param callbackQueryId Telegram callback-query identifier
     * @param text response shown to the user
     * @param alert whether to show the response in a pop-up alert instead of a brief notice
     */
    void answerButton(String callbackQueryId, String text, boolean alert);
}
