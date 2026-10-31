package is442t1.studybuddy.telegram.api;

/**
 * The parts of a Telegram update StudyBuddy reads. Fields Telegram adds are ignored.
 *
 * @param message null when the update is of another kind
 */
public record Update(long updateId, Message message) {

    /** @param from null for messages sent on behalf of a channel; {@code text} is null for non-text messages */
    public record Message(long messageId, Sender from, Chat chat, String text) {
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
