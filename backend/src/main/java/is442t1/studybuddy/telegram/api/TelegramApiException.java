package is442t1.studybuddy.telegram.api;

/**
 * A failed Bot API call. Deliberately has no cause: HTTP client exceptions quote the request URL, which contains
 * the bot token, and a cause would carry that into the logs.
 *
 * @see #code() Telegram's error code, or {@link #NO_REPLY} if no usable reply arrived
 */
public class TelegramApiException extends RuntimeException {

    public static final int NO_REPLY = 0;

    private final int code;
    private final Integer retryAfterSeconds;
    private final Long migrateToChatId;

    public TelegramApiException(int code, String description, Integer retryAfterSeconds, Long migrateToChatId) {
        super(description);
        this.code = code;
        this.retryAfterSeconds = retryAfterSeconds;
        this.migrateToChatId = migrateToChatId;
    }

    public int code() {
        return code;
    }

    /** How long Telegram asked us to wait, or null. */
    public Integer retryAfterSeconds() {
        return retryAfterSeconds;
    }

    /** The chat's new id when a group was upgraded to a supergroup, or null. */
    public Long migrateToChatId() {
        return migrateToChatId;
    }
}
