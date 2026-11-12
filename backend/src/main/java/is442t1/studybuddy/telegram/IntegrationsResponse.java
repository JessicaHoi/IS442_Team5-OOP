package is442t1.studybuddy.telegram;

/**
 * Which optional integrations this server has on, so the front end can hide what isn't available.
 *
 * @param telegram whether Telegram is enabled and, when enabled, the configured bot username
 */
public record IntegrationsResponse(Telegram telegram) {

    /**
     * Telegram integration availability.
     *
     * @param enabled whether this server has Telegram features enabled
     * @param botUsername the configured bot username, or null when Telegram is off
     */
    public record Telegram(boolean enabled, String botUsername) {
    }

    static IntegrationsResponse of(TelegramProperties properties) {
        return new IntegrationsResponse(new Telegram(properties.enabled(), properties.enabled() ? properties.botUsername() : null));
    }
}
