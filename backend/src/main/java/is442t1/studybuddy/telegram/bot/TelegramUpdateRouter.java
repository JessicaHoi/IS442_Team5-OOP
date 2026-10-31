package is442t1.studybuddy.telegram.bot;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import is442t1.studybuddy.telegram.api.TelegramClient;
import is442t1.studybuddy.telegram.api.Update;
import is442t1.studybuddy.telegram.link.TelegramLinkService;

/**
 * Acts on updates. Holds no transaction: the database work finishes before the reply goes out, so an
 * HTTP call never keeps a database connection.
 */
@Component
@ConditionalOnProperty(prefix = "studybuddy.telegram", name = "enabled", havingValue = "true")
public class TelegramUpdateRouter {

    private static final String ACCOUNT_LINK_PREFIX = "a_";
    static final String WELCOME = "Hi! I'm the StudyBuddy bot. To connect your account, open your StudyBuddy profile and choose Connect Telegram.";
    static final String CONNECTED = "Connected ✓ You can go back to StudyBuddy.";
    static final String LINK_NOT_VALID = "This link has expired or was already used. Open your StudyBuddy profile and choose Connect Telegram again.";

    private final TelegramClient client;
    private final TelegramLinkService linkService;

    TelegramUpdateRouter(TelegramClient client, TelegramLinkService linkService) {
        this.client = client;
        this.linkService = linkService;
    }

    public void route(Update update) {
        switch (UpdateClassifier.classify(update)) {
            case BotEvent.StartCommand start -> client.sendMessage(start.chatId(), replyTo(start));
            case BotEvent.Ignored ignored -> {
            }
        }
    }

    private String replyTo(BotEvent.StartCommand start) {
        if (!start.payload().startsWith(ACCOUNT_LINK_PREFIX)) {
            return WELCOME;
        }
        String token = start.payload().substring(ACCOUNT_LINK_PREFIX.length());
        boolean linked = linkService.completeLink(token, start.telegramUserId(), start.username(), start.firstName());
        return linked ? CONNECTED : LINK_NOT_VALID;
    }
}
