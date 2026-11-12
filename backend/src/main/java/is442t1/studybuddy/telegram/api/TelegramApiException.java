package is442t1.studybuddy.telegram.api;

/**
 * A failed Bot API call. Deliberately has no cause: HTTP client exceptions quote the request URL, which contains
 * the bot token, and a cause would carry that into the logs.
 *
 * @see #code()
 */
public class TelegramApiException extends RuntimeException {

    /** Error-code sentinel used when Telegram did not return a usable response. */
    public static final int NO_REPLY = 0;

    /** Telegram's error code, or {@link #NO_REPLY} if no usable reply arrived. */
    private final int code;
    /** Telegram's requested wait before retrying, or null when unspecified. */
    private final Integer retryAfterSeconds;
    /** Replacement chat identifier for a group migration, or null when no migration occurred. */
    private final Long migrateToChatId;

    /**
     * Creates a Telegram API failure without retaining a cause that could expose the bot token.
     *
     * @param code Telegram's error code, or {@link #NO_REPLY} if no usable reply arrived
     * @param description a safe-to-log summary of the failure
     * @param retryAfterSeconds how long Telegram asked the caller to wait, or null
     * @param migrateToChatId the replacement chat identifier when Telegram reports a migration, or null
     */
    public TelegramApiException(int code, String description, Integer retryAfterSeconds, Long migrateToChatId) {
        super(description);
        this.code = code;
        this.retryAfterSeconds = retryAfterSeconds;
        this.migrateToChatId = migrateToChatId;
    }

    /**
     * Returns Telegram's error code, or {@link #NO_REPLY} when no usable response arrived.
     *
     * @return Telegram error code or the no-reply sentinel
     */
    public int code() {
        return code;
    }

    /**
     * Returns how long Telegram asked us to wait, or null when unspecified.
     *
     * @return retry delay in seconds, or null
     */
    public Integer retryAfterSeconds() {
        return retryAfterSeconds;
    }

    /**
     * Returns the chat's new identifier when a group was upgraded to a supergroup, or null.
     *
     * @return replacement chat identifier, or null
     */
    public Long migrateToChatId() {
        return migrateToChatId;
    }
}
