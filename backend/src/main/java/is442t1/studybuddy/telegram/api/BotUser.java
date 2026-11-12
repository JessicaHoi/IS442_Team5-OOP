package is442t1.studybuddy.telegram.api;

/**
 * The bot identity returned by Telegram's {@code getMe} method.
 *
 * @param id the bot's Telegram user identifier
 * @param username the bot's username
 */
public record BotUser(long id, String username) {
}
