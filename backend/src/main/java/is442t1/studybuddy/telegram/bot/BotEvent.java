package is442t1.studybuddy.telegram.bot;

/** What an update means to StudyBuddy. Later phases add a case for each new kind of update. */
sealed interface BotEvent {

    /**
     * Someone pressed Start in a private chat with the bot.
     *
     * @param payload the text after {@code /start}, or an empty string
     */
    record StartCommand(long chatId, long telegramUserId, String username, String firstName, String payload)
            implements BotEvent {
    }

    /** Everything else. Group messages end up here, and their text is never logged or stored. */
    record Ignored() implements BotEvent {
    }
}
