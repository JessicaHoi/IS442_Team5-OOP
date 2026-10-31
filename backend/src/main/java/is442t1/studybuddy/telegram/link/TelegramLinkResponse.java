package is442t1.studybuddy.telegram.link;

import java.time.Instant;

/** A one-time link the student opens in Telegram. */
public record TelegramLinkResponse(String url, Instant expiresAt) {
}
