package is442t1.studybuddy.telegram.api;

/**
 * Marks part of a message's text as meaning something to Telegram.
 *
 * @param type Telegram entity type, such as {@code text_mention}
 * @param offset where it starts, in UTF-16 code units, which is how Java counts a String
 * @param length its length, in the same units
 * @param user mentioned Telegram account for a text mention, or null for other entity types
 */
public record MessageEntity(String type, int offset, int length, Update.Sender user) {

    /**
     * Creates a mention entity for a Telegram user, which also works without a username.
     *
     * @param offset start of the name in UTF-16 code units
     * @param length length of the name in UTF-16 code units
     * @param telegramUserId mentioned Telegram user identifier
     * @param name displayed name to mention
     * @return the Telegram text-mention entity
     */
    public static MessageEntity mention(int offset, int length, long telegramUserId, String name) {
        return new MessageEntity("text_mention", offset, length, new Update.Sender(telegramUserId, false, name, null));
    }
}
