package is442t1.studybuddy.telegram.api;

/**
 * The parts of a Telegram update StudyBuddy reads. Fields Telegram adds are ignored.
 *
 * @param updateId Telegram's monotonically increasing update identifier
 * @param message a message update, or null when the update is another kind
 * @param myChatMember a change to the bot's own chat membership, or null
 * @param chatMember a member's status change, or null
 * @param chatJoinRequest a request to join a chat, or null
 * @param callbackQuery a press on one of the bot's inline buttons, or null
 */
public record Update(long updateId, Message message, ChatMemberUpdated myChatMember, ChatMemberUpdated chatMember,
        ChatJoinRequest chatJoinRequest, CallbackQuery callbackQuery) {

    /**
     * Creates an update containing only an optional message.
     *
     * @param updateId Telegram's monotonically increasing update identifier
     * @param message the message, or null
     */
    public Update(long updateId, Message message) {
        this(updateId, message, null, null, null, null);
    }

    /**
     * Creates an update without a callback query.
     *
     * @param updateId Telegram's monotonically increasing update identifier
     * @param message a message update, or null
     * @param myChatMember a change to the bot's own chat membership, or null
     * @param chatMember a member's status change, or null
     * @param chatJoinRequest a request to join a chat, or null
     */
    public Update(long updateId, Message message, ChatMemberUpdated myChatMember, ChatMemberUpdated chatMember,
            ChatJoinRequest chatJoinRequest) {
        this(updateId, message, myChatMember, chatMember, chatJoinRequest, null);
    }

    /**
     * A message received in a chat.
     *
     * @param messageId the Telegram message identifier
     * @param from the sender, or null for messages sent on behalf of a channel
     * @param chat the chat where the message was sent
     * @param text the message text, or null for non-text messages
     * @param chatShared a chat selected with a chat-request button, or null
     * @param migrateToChatId the new chat identifier if this group migrated, or null
     * @param migrateFromChatId the previous chat identifier if this chat was upgraded, or null
     * @param videoChatStarted a notice that a video chat started, or null
     */
    public record Message(long messageId, Sender from, Chat chat, String text, ChatShared chatShared,
            Long migrateToChatId, Long migrateFromChatId, VideoChatStarted videoChatStarted) {

        /**
         * Creates a basic message without chat-selection, migration, or video-chat details.
         *
         * @param messageId the Telegram message identifier
         * @param from the sender, or null for messages sent on behalf of a channel
         * @param chat the chat where the message was sent
         * @param text the message text, or null for non-text messages
         */
        public Message(long messageId, Sender from, Chat chat, String text) {
            this(messageId, from, chat, text, null, null, null);
        }

        /**
         * Creates a message without video-chat details.
         *
         * @param messageId the Telegram message identifier
         * @param from the sender, or null for messages sent on behalf of a channel
         * @param chat the chat where the message was sent
         * @param text the message text, or null for non-text messages
         * @param chatShared a chat selected with a chat-request button, or null
         * @param migrateToChatId the new chat identifier if this group migrated, or null
         * @param migrateFromChatId the previous chat identifier if this chat was upgraded, or null
         */
        public Message(long messageId, Sender from, Chat chat, String text, ChatShared chatShared,
                Long migrateToChatId, Long migrateFromChatId) {
            this(messageId, from, chat, text, chatShared, migrateToChatId, migrateFromChatId, null);
        }
    }

    /** Telegram's notice that someone started a video chat. It carries no details. */
    public record VideoChatStarted() {
    }

    /**
     * A chat selected by a user through a chat-request button.
     *
     * @param chatId the selected Telegram chat identifier
     * @param requestId the identifier supplied with the button
     */
    public record ChatShared(long chatId, int requestId) {
    }

    /**
     * A change to a member's status in a chat.
     *
     * @param chat the affected chat
     * @param from the account that made the change
     * @param oldChatMember the member's previous status
     * @param newChatMember the member's new status
     * @param inviteLink the invite link used, when Telegram provides it
     */
    public record ChatMemberUpdated(Chat chat, Sender from, ChatMember oldChatMember, ChatMember newChatMember,
            ChatInviteLink inviteLink) {
    }

    /**
     * A request from a user to join a chat.
     *
     * @param chat the chat they want to join
     * @param from the user asking to join
     * @param userChatId the private chat where the bot may message them for the next few minutes, even if they never pressed Start
     * @param inviteLink the invite link used, when provided
     */
    public record ChatJoinRequest(Chat chat, Sender from, long userChatId, ChatInviteLink inviteLink) {
    }

    /**
     * Someone pressed an inline button attached to a message.
     *
     * @param id Telegram's callback-query identifier
     * @param from the user who pressed the button
     * @param data the button payload, or null for buttons StudyBuddy didn't make
     */
    public record CallbackQuery(String id, Sender from, String data) {
    }

    /**
     * A Telegram account that sent or is referenced by an update.
     *
     * @param id the Telegram user identifier
     * @param isBot whether the account is a bot
     * @param firstName the account's first name
     * @param username the username, or null when the account has none
     */
    public record Sender(long id, boolean isBot, String firstName, String username) {
    }

    /**
     * A Telegram chat.
     *
     * @param id the Telegram chat identifier
     * @param type Telegram's chat type, such as {@code private}, {@code group}, or {@code supergroup}
     */
    public record Chat(long id, String type) {

        /**
         * Returns whether Telegram identifies this as a private chat.
         *
         * @return true when the chat type is {@code private}
         */
        public boolean isPrivate() {
            return "private".equals(type);
        }
    }
}
