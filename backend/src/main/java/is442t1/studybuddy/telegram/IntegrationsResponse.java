package is442t1.studybuddy.telegram;

/** Which optional integrations this server has on, so the front end can hide what isn't available. */
public record IntegrationsResponse(Telegram telegram) {

    /**
     * Client-visible capability fields for the optional Telegram integration, without bot credentials.
     *
     * @param botUsername null when Telegram is off
     */
    public record Telegram(boolean enabled, String botUsername) {
    }

    static IntegrationsResponse of(TelegramProperties properties) {
        return new IntegrationsResponse(new Telegram(properties.enabled(), properties.enabled() ? properties.botUsername() : null));
    }
}
