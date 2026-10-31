package is442t1.studybuddy.telegram.api;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Stands in when Telegram is off. Nothing should reach it, because the services check first. */
@Component
@ConditionalOnProperty(prefix = "studybuddy.telegram", name = "enabled", havingValue = "false", matchIfMissing = true)
class DisabledTelegramClient implements TelegramClient {

    private static TelegramApiException off() {
        return new TelegramApiException(TelegramApiException.NO_REPLY, "Telegram is off.", null, null);
    }

    @Override
    public BotUser getMe() {
        throw off();
    }

    @Override
    public void deleteWebhook() {
        throw off();
    }

    @Override
    public List<Update> getUpdates(long offset, Duration timeout, List<String> allowedUpdates) {
        throw off();
    }

    @Override
    public void sendMessage(long chatId, String text) {
        throw off();
    }

    @Override
    public long sendMessage(long chatId, String text, ReplyMarkup markup) {
        throw off();
    }

    @Override
    public ChatInfo getChat(long chatId) {
        throw off();
    }

    @Override
    public ChatMember getChatMember(long chatId, long userId) {
        throw off();
    }

    @Override
    public ChatInviteLink createJoinRequestLink(long chatId, String name, Instant expiresAt) {
        throw off();
    }

    @Override
    public void revokeInviteLink(long chatId, String inviteLink) {
        throw off();
    }

    @Override
    public void approveJoinRequest(long chatId, long userId) {
        throw off();
    }

    @Override
    public void declineJoinRequest(long chatId, long userId) {
        throw off();
    }

    @Override
    public void banMember(long chatId, long userId) {
        throw off();
    }

    @Override
    public void unbanMember(long chatId, long userId) {
        throw off();
    }

    @Override
    public void setTitle(long chatId, String title) {
        throw off();
    }

    @Override
    public void setDescription(long chatId, String description) {
        throw off();
    }

    @Override
    public void pinMessage(long chatId, long messageId) {
        throw off();
    }

    @Override
    public void setMemberPermissions(long chatId, boolean canSendMessages) {
        throw off();
    }

    @Override
    public void leaveChat(long chatId) {
        throw off();
    }
}
