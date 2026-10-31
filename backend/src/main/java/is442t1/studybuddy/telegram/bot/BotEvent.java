package is442t1.studybuddy.telegram.bot;

import is442t1.studybuddy.telegram.api.ChatMember;
import is442t1.studybuddy.telegram.api.Update;

/** What an update means to StudyBuddy. */
sealed interface BotEvent {

    /**
     * Someone pressed Start in a private chat with the bot.
     *
     * @param payload the text after {@code /start}, or an empty string
     */
    record StartCommand(long chatId, long telegramUserId, String username, String firstName, String payload)
            implements BotEvent {
    }

    /** The leader chose a chat with a setup button. */
    record ChatChosen(long privateChatId, long telegramUserId, String firstName, int requestId, long chatId) implements BotEvent {
    }

    record JoinRequested(Update.ChatJoinRequest request) implements BotEvent {
    }

    /** The bot itself was added to, changed in or removed from a chat. */
    record BotChanged(long chatId, ChatMember now) implements BotEvent {
    }

    /** Someone else joined or left a chat. */
    record MemberChanged(long chatId, long telegramUserId, boolean inChat) implements BotEvent {
    }

    /** A basic group was upgraded and got a new id. */
    record ChatMigrated(long oldChatId, long newChatId) implements BotEvent {
    }

    /** Someone pressed a button attached to one of the bot's messages. */
    record ButtonPressed(String callbackQueryId, long telegramUserId, String data) implements BotEvent {
    }

    /** @param startedBy null when Telegram doesn't say who started it */
    record VideoChatStarted(long chatId, Long startedBy) implements BotEvent {
    }

    /** Everything else. Group messages end up here, and their text is never logged or stored. */
    record Ignored() implements BotEvent {
    }
}
