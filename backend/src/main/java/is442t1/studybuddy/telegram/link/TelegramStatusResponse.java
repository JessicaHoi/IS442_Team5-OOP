package is442t1.studybuddy.telegram.link;

import java.time.Instant;

/**
 * Whether the signed-in student has connected Telegram. Only ever shown to that student.
 *
 * @param linked whether a Telegram account is connected
 * @param username connected Telegram username, or null when unavailable
 * @param firstName connected Telegram first name, or null when unavailable
 * @param canMessage whether the bot may send the account a private message
 * @param linkedAt when the Telegram account was connected, or null when not linked
 */
public record TelegramStatusResponse(boolean linked, String username, String firstName, boolean canMessage, Instant linkedAt) {

    static TelegramStatusResponse notLinked() {
        return new TelegramStatusResponse(false, null, null, false, null);
    }

    static TelegramStatusResponse of(TelegramAccount account) {
        return new TelegramStatusResponse(true, account.getUsername(), account.getFirstName(), account.canMessage(),
                account.getLinkedAt());
    }
}
