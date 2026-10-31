package is442t1.studybuddy.telegram.bot;

import is442t1.studybuddy.telegram.api.Update;

/** Sorts raw updates into {@link BotEvent}s. A link token only counts in a private chat, so it can't be used from a group. */
final class UpdateClassifier {

    private static final String START = "/start";

    private UpdateClassifier() {
    }

    static BotEvent classify(Update update) {
        Update.Message message = update.message();
        if (message == null || message.from() == null || message.from().isBot() || message.chat() == null
                || !message.chat().isPrivate() || message.text() == null) {
            return new BotEvent.Ignored();
        }
        String text = message.text();
        if (!text.equals(START) && !text.startsWith(START + " ")) {
            return new BotEvent.Ignored();
        }
        Update.Sender sender = message.from();
        return new BotEvent.StartCommand(message.chat().id(), sender.id(), sender.username(), sender.firstName(),
                text.substring(START.length()).trim());
    }
}
