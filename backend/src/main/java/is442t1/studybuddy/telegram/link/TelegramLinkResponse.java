package is442t1.studybuddy.telegram.link;

import java.time.Instant;

/**
 * A one-time link the student opens in Telegram.
 *
 * @param url Telegram deep link that carries the one-time token
 * @param expiresAt time after which the link can no longer be used
 */
public record TelegramLinkResponse(String url, Instant expiresAt) {
}
