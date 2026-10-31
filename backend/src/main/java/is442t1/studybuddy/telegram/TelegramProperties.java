package is442t1.studybuddy.telegram;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings under {@code studybuddy.telegram} in application.yaml. An enabled bot with a bad token or no
 * username stops the application from starting.
 *
 * @param enabled      whether the Telegram features are on; off by default
 * @param botToken     the token from BotFather; a secret, so it never appears in {@link #toString()}
 * @param botUsername  the bot's username, without the @
 * @param polling      whether this instance reads updates; only one instance may poll per token
 * @param pollTimeout  how long one {@code getUpdates} call waits for an update
 * @param linkTokenTtl how long a link to connect a Telegram account or set up a chat stays valid
 * @param inviteTtl    how long a student's personal invite to a group chat stays valid
 */
@ConfigurationProperties(prefix = "studybuddy.telegram")
public record TelegramProperties(boolean enabled, String botToken, String botUsername, boolean polling,
        Duration pollTimeout, Duration linkTokenTtl, Duration inviteTtl) {

    /** What students see if they ask for something Telegram-related while it's off. */
    public static final String OFF_MESSAGE = "Telegram isn't set up on this StudyBuddy server.";

    private static final String TOKEN_FORMAT = "\\d+:[A-Za-z0-9_-]+";

    public TelegramProperties {
        if (enabled && (botToken == null || !botToken.matches(TOKEN_FORMAT))) {
            throw new IllegalArgumentException("TELEGRAM_BOT_TOKEN is missing or malformed.");
        }
        if (enabled && (botUsername == null || botUsername.isBlank())) {
            throw new IllegalArgumentException("TELEGRAM_BOT_USERNAME is missing.");
        }
    }

    /** The bot's Telegram id, the digits before the colon in its token. Only call this when enabled. */
    public long botId() {
        return Long.parseLong(botToken.substring(0, botToken.indexOf(':')));
    }

    @Override
    public String toString() {
        return "TelegramProperties[enabled=" + enabled + ", botUsername=" + botUsername + ", polling=" + polling + "]";
    }
}
