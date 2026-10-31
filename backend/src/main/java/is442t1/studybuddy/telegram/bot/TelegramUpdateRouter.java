package is442t1.studybuddy.telegram.bot;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import is442t1.studybuddy.telegram.api.TelegramClient;
import is442t1.studybuddy.telegram.api.Update;
import is442t1.studybuddy.telegram.chat.ChatBotHandler;
import is442t1.studybuddy.telegram.link.TelegramLinkService;

/**
 * Acts on updates. Holds no transaction: the database work finishes before the reply goes out, so an
 * HTTP call never keeps a database connection.
 */
@Component
@ConditionalOnProperty(prefix = "studybuddy.telegram", name = "enabled", havingValue = "true")
public class TelegramUpdateRouter {

    private static final String ACCOUNT_LINK_PREFIX = "a_";
    private static final String GROUP_SETUP_PREFIX = "g_";
    static final String WELCOME = "Hi! I'm the StudyBuddy bot. To connect your account, open your StudyBuddy profile and choose Connect Telegram.";
    static final String CONNECTED = "Connected ✓ You can go back to StudyBuddy.";
    static final String LINK_NOT_VALID = "This link has expired or was already used. Open your StudyBuddy profile and choose Connect Telegram again.";

    private final TelegramClient client;
    private final TelegramLinkService linkService;
    private final ChatBotHandler chatHandler;

    TelegramUpdateRouter(TelegramClient client, TelegramLinkService linkService, ChatBotHandler chatHandler) {
        this.client = client;
        this.linkService = linkService;
        this.chatHandler = chatHandler;
    }

    public void route(Update update) {
        switch (UpdateClassifier.classify(update)) {
            case BotEvent.StartCommand start when start.payload().startsWith(GROUP_SETUP_PREFIX) ->
                    chatHandler.openSetup(start.chatId(), start.telegramUserId(), start.payload().substring(GROUP_SETUP_PREFIX.length()));
            case BotEvent.StartCommand start -> client.sendMessage(start.chatId(), replyTo(start));
            case BotEvent.ChatChosen chosen -> chatHandler.chatChosen(chosen.privateChatId(), chosen.telegramUserId(), chosen.firstName(), chosen.requestId(), chosen.chatId());
            case BotEvent.JoinRequested joining -> chatHandler.joinRequested(joining.request());
            case BotEvent.BotChanged changed -> chatHandler.botChanged(changed.chatId(), changed.now());
            case BotEvent.MemberChanged changed -> chatHandler.memberChanged(changed.chatId(), changed.telegramUserId(), changed.inChat());
            case BotEvent.ChatMigrated migrated -> chatHandler.chatMigrated(migrated.oldChatId(), migrated.newChatId());
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
