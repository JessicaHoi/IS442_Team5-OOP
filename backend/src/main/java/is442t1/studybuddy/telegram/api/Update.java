package is442t1.studybuddy.telegram.api;

/**
 * The parts of a Telegram update StudyBuddy reads. Fields Telegram adds are ignored.
 *
 * @param message null when the update is of another kind
 */
public record Update(long updateId, Message message, ChatMemberUpdated myChatMember, ChatMemberUpdated chatMember,
        ChatJoinRequest chatJoinRequest, CallbackQuery callbackQuery) {

    public Update(long updateId, Message message) {
        this(updateId, message, null, null, null, null);
    }

    public Update(long updateId, Message message, ChatMemberUpdated myChatMember, ChatMemberUpdated chatMember,
            ChatJoinRequest chatJoinRequest) {
        this(updateId, message, myChatMember, chatMember, chatJoinRequest, null);
    }

    /** @param from null for messages sent on behalf of a channel; {@code text} is null for non-text messages */
    public record Message(long messageId, Sender from, Chat chat, String text, ChatShared chatShared,
            Long migrateToChatId, Long migrateFromChatId, VideoChatStarted videoChatStarted) {

        public Message(long messageId, Sender from, Chat chat, String text) {
            this(messageId, from, chat, text, null, null, null);
        }

        public Message(long messageId, Sender from, Chat chat, String text, ChatShared chatShared,
                Long migrateToChatId, Long migrateFromChatId) {
            this(messageId, from, chat, text, chatShared, migrateToChatId, migrateFromChatId, null);
        }
    }

    /** Telegram's notice that someone started a video chat. It carries no details. */
    public record VideoChatStarted() {
    }

    public record ChatShared(long chatId, int requestId) {
    }

    /** @param inviteLink how they joined, when that applies */
    public record ChatMemberUpdated(Chat chat, Sender from, ChatMember oldChatMember, ChatMember newChatMember,
            ChatInviteLink inviteLink) {
    }

    /** @param userChatId where the bot may message this person for the next few minutes, even if they never pressed Start */
    public record ChatJoinRequest(Chat chat, Sender from, long userChatId, ChatInviteLink inviteLink) {
    }

    /** Someone pressed a button attached to a message. @param data null for buttons StudyBuddy didn't make */
    public record CallbackQuery(String id, Sender from, String data) {
    }

    /** @param username null when the person has none */
    public record Sender(long id, boolean isBot, String firstName, String username) {
    }

    public record Chat(long id, String type) {

        public boolean isPrivate() {
            return "private".equals(type);
        }
    }
}
