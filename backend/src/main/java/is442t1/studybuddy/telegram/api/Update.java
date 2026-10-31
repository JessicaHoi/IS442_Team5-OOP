package is442t1.studybuddy.telegram.api;

/**
 * The parts of a Telegram update StudyBuddy reads. Fields Telegram adds are ignored.
 *
 * @param message null when the update is of another kind
 */
public record Update(long updateId, Message message, ChatMemberUpdated myChatMember, ChatMemberUpdated chatMember,
        ChatJoinRequest chatJoinRequest) {

    public Update(long updateId, Message message) {
        this(updateId, message, null, null, null);
    }

    /** @param from null for messages sent on behalf of a channel; {@code text} is null for non-text messages */
    public record Message(long messageId, Sender from, Chat chat, String text, ChatShared chatShared,
            Long migrateToChatId, Long migrateFromChatId) {

        public Message(long messageId, Sender from, Chat chat, String text) {
            this(messageId, from, chat, text, null, null, null);
        }
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

    /** @param username null when the person has none */
    public record Sender(long id, boolean isBot, String firstName, String username) {
    }

    public record Chat(long id, String type) {

        public boolean isPrivate() {
            return "private".equals(type);
        }
    }
}
