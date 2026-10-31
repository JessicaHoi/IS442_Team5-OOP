package is442t1.studybuddy.telegram.bot;

import is442t1.studybuddy.telegram.api.Update;

/** Sorts raw updates into {@link BotEvent}s. A link token only counts in a private chat, so it can't be used from a group. */
final class UpdateClassifier {

    private static final String START = "/start";

    private UpdateClassifier() {
    }

    static BotEvent classify(Update update) {
        if (update.chatJoinRequest() != null) {
            return new BotEvent.JoinRequested(update.chatJoinRequest());
        }
        if (update.callbackQuery() != null) {
            Update.CallbackQuery press = update.callbackQuery();
            return new BotEvent.ButtonPressed(press.id(), press.from().id(), press.data());
        }
        if (update.myChatMember() != null) {
            return new BotEvent.BotChanged(update.myChatMember().chat().id(), update.myChatMember().newChatMember());
        }
        if (update.chatMember() != null) {
            Update.ChatMemberUpdated change = update.chatMember();
            return new BotEvent.MemberChanged(change.chat().id(), change.newChatMember().user().id(), change.newChatMember().isInChat());
        }
        Update.Message message = update.message();
        if (message != null && message.migrateToChatId() != null) {
            return new BotEvent.ChatMigrated(message.chat().id(), message.migrateToChatId());
        }
        if (message != null && message.migrateFromChatId() != null) {
            return new BotEvent.ChatMigrated(message.migrateFromChatId(), message.chat().id());
        }
        if (message != null && message.videoChatStarted() != null) {
            return new BotEvent.VideoChatStarted(message.chat().id(), message.from() == null ? null : message.from().id());
        }
        if (message == null || message.from() == null || message.from().isBot() || message.chat() == null
                || !message.chat().isPrivate()) {
            return new BotEvent.Ignored();
        }
        if (message.chatShared() != null) {
            return new BotEvent.ChatChosen(message.chat().id(), message.from().id(), message.from().firstName(),
                    message.chatShared().requestId(), message.chatShared().chatId());
        }
        if (message.text() == null) {
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
