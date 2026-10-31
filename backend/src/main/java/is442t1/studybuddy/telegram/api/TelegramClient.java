package is442t1.studybuddy.telegram.api;

import java.time.Duration;
import java.util.List;

/** The Telegram Bot API methods StudyBuddy uses. Every method throws {@link TelegramApiException} on failure. */
public interface TelegramClient {

    BotUser getMe();

    /** Polling and a webhook can't share a token, so this clears any webhook left behind. */
    void deleteWebhook();

    /** Waits up to {@code timeout} for updates. An {@code offset} confirms every earlier update. */
    List<Update> getUpdates(long offset, Duration timeout, List<String> allowedUpdates);

    void sendMessage(long chatId, String text);
}
