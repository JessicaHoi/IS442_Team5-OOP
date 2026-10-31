package is442t1.studybuddy.telegram.chat;

import java.util.List;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;

import org.springframework.stereotype.Component;

import is442t1.studybuddy.telegram.api.MessageEntity;
import is442t1.studybuddy.telegram.api.ReplyMarkup;
import is442t1.studybuddy.telegram.api.TelegramApiException;
import is442t1.studybuddy.telegram.api.TelegramClient;
import is442t1.studybuddy.telegram.chat.ChatStore.ChatRef;

/** Sends to a group's chat, following the chat if Telegram has upgraded it to a supergroup with a new id. */
@Component
class ChatSender {

    private final ChatStore store;
    private final TelegramClient client;

    ChatSender(ChatStore store, TelegramClient client) {
        this.store = store;
        this.client = client;
    }

    long send(ChatRef chat, String text) {
        return onChat(chat, chatId -> client.sendMessage(chatId, text));
    }

    long send(ChatRef chat, String text, ReplyMarkup markup) {
        return onChat(chat, chatId -> client.sendMessage(chatId, text, markup));
    }

    long send(ChatRef chat, String text, List<MessageEntity> entities) {
        return entities.isEmpty() ? send(chat, text) : onChat(chat, chatId -> client.sendMessage(chatId, text, entities));
    }

    long reply(ChatRef chat, long messageId, String text, List<MessageEntity> entities) {
        return onChat(chat, chatId -> client.sendReply(chatId, messageId, text, entities));
    }

    /** Runs a call on the chat, using its latest saved id. */
    <T> T onChat(ChatRef chat, LongFunction<T> call) {
        return onChatId(store.findByGroup(chat.groupId()).map(ChatRef::chatId).orElse(chat.chatId()), call);
    }

    void runOnChat(ChatRef chat, LongConsumer call) {
        onChat(chat, chatId -> {
            call.accept(chatId);
            return null;
        });
    }

    /** If the group has since been upgraded, Telegram says so: save the new id and try once more. */
    <T> T onChatId(long chatId, LongFunction<T> call) {
        try {
            return call.apply(chatId);
        } catch (TelegramApiException exception) {
            if (exception.migrateToChatId() == null) {
                throw exception;
            }
            store.migrate(chatId, exception.migrateToChatId());
            return call.apply(exception.migrateToChatId());
        }
    }
}
