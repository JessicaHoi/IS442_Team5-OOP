package is442t1.studybuddy.telegram.api;

/**
 * The chat details returned by Telegram's {@code getChat} method.
 *
 * @param id the Telegram chat identifier
 * @param type the Telegram chat type, such as {@code group} or {@code supergroup}
 * @param title the chat title, when Telegram provides one
 */
public record ChatInfo(long id, String type, String title) {
}
