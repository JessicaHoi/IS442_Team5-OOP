package is442t1.studybuddy.telegram.api;

/**
 * Marks part of a message's text as meaning something to Telegram.
 *
 * @param offset where it starts, in UTF-16 code units, which is how Java counts a String
 * @param length its length, in the same units
 */
public record MessageEntity(String type, int offset, int length, Update.Sender user) {

    /** Tags someone by their Telegram id, so it also works for people with no username. */
    public static MessageEntity mention(int offset, int length, long telegramUserId, String name) {
        return new MessageEntity("text_mention", offset, length, new Update.Sender(telegramUserId, false, name, null));
    }
}
