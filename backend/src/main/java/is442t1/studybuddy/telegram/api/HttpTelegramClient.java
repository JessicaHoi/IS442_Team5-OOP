package is442t1.studybuddy.telegram.api;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import is442t1.studybuddy.telegram.TelegramProperties;

/** Calls the Bot API over HTTPS. The request URL contains the token, so it is never logged or put in an exception. */
@Component
@ConditionalOnProperty(prefix = "studybuddy.telegram", name = "enabled", havingValue = "true")
class HttpTelegramClient implements TelegramClient {

    private static final String API_URL = "https://api.telegram.org/bot";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    /** Longer than the 50 second poll, so a quiet long poll isn't reported as a failure. */
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);

    private final RestClient restClient;
    private final JsonMapper mapper = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            // Telegram leaves out what doesn't apply, such as an ordinary member's rights: missing means false.
            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .changeDefaultPropertyInclusion(inclusion -> inclusion.withValueInclusion(JsonInclude.Include.NON_NULL))
            .build();

    @Autowired
    HttpTelegramClient(TelegramProperties properties) {
        this(RestClient.builder().requestFactory(requestFactory()), properties.botToken());
    }

    HttpTelegramClient(RestClient.Builder builder, String botToken) {
        this.restClient = builder.baseUrl(API_URL + botToken).build();
    }

    private static JdkClientHttpRequestFactory requestFactory() {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }

    @Override
    public BotUser getMe() {
        return read(call("getMe", Map.of()), mapper.constructType(BotUser.class));
    }

    @Override
    public void deleteWebhook() {
        call("deleteWebhook", Map.of());
    }

    @Override
    public List<Update> getUpdates(long offset, Duration timeout, List<String> allowedUpdates) {
        JsonNode result = call("getUpdates",
                Map.of("offset", offset, "timeout", timeout.toSeconds(), "allowed_updates", allowedUpdates));
        return read(result, mapper.getTypeFactory().constructCollectionType(List.class, Update.class));
    }

    @Override
    public long sendMessage(long chatId, String text) {
        return call("sendMessage", Map.of("chat_id", chatId, "text", text)).path("message_id").asLong();
    }

    @Override
    public long sendMessage(long chatId, String text, ReplyMarkup markup) {
        return call("sendMessage", Map.of("chat_id", chatId, "text", text, "reply_markup", markup)).path("message_id").asLong();
    }

    @Override
    public long sendMessage(long chatId, String text, List<MessageEntity> entities) {
        return call("sendMessage", Map.of("chat_id", chatId, "text", text, "entities", entities)).path("message_id").asLong();
    }

    @Override
    public long sendReply(long chatId, long replyToMessageId, String text, List<MessageEntity> entities) {
        Map<String, Object> body = new HashMap<>(Map.of("chat_id", chatId, "text", text,
                "reply_parameters", Map.of("message_id", replyToMessageId, "allow_sending_without_reply", true)));
        if (!entities.isEmpty()) {
            body.put("entities", entities);
        }
        return call("sendMessage", body).path("message_id").asLong();
    }

    @Override
    public void editMessage(long chatId, long messageId, String text) {
        call("editMessageText", Map.of("chat_id", chatId, "message_id", messageId, "text", text));
    }

    @Override
    public void editMessage(long chatId, long messageId, String text, ReplyMarkup markup) {
        call("editMessageText", Map.of("chat_id", chatId, "message_id", messageId, "text", text, "reply_markup", markup));
    }

    @Override
    public ChatInfo getChat(long chatId) {
        return read(call("getChat", Map.of("chat_id", chatId)), mapper.constructType(ChatInfo.class));
    }

    @Override
    public ChatMember getChatMember(long chatId, long userId) {
        return read(call("getChatMember", Map.of("chat_id", chatId, "user_id", userId)), mapper.constructType(ChatMember.class));
    }

    @Override
    public ChatInviteLink createJoinRequestLink(long chatId, String name, Instant expiresAt) {
        JsonNode result = call("createChatInviteLink", Map.of("chat_id", chatId, "name", name,
                "creates_join_request", true, "expire_date", expiresAt.getEpochSecond()));
        return read(result, mapper.constructType(ChatInviteLink.class));
    }

    @Override
    public void revokeInviteLink(long chatId, String inviteLink) {
        call("revokeChatInviteLink", Map.of("chat_id", chatId, "invite_link", inviteLink));
    }

    @Override
    public void approveJoinRequest(long chatId, long userId) {
        call("approveChatJoinRequest", Map.of("chat_id", chatId, "user_id", userId));
    }

    @Override
    public void declineJoinRequest(long chatId, long userId) {
        call("declineChatJoinRequest", Map.of("chat_id", chatId, "user_id", userId));
    }

    @Override
    public void banMember(long chatId, long userId) {
        call("banChatMember", Map.of("chat_id", chatId, "user_id", userId));
    }

    @Override
    public void unbanMember(long chatId, long userId) {
        call("unbanChatMember", Map.of("chat_id", chatId, "user_id", userId, "only_if_banned", true));
    }

    @Override
    public void setTitle(long chatId, String title) {
        call("setChatTitle", Map.of("chat_id", chatId, "title", title));
    }

    @Override
    public void setDescription(long chatId, String description) {
        call("setChatDescription", Map.of("chat_id", chatId, "description", description));
    }

    @Override
    public void pinMessage(long chatId, long messageId) {
        call("pinChatMessage", Map.of("chat_id", chatId, "message_id", messageId, "disable_notification", true));
    }

    @Override
    public void unpinMessage(long chatId, long messageId) {
        call("unpinChatMessage", Map.of("chat_id", chatId, "message_id", messageId));
    }

    @Override
    public void setMemberPermissions(long chatId, boolean canSendMessages) {
        call("setChatPermissions", Map.of("chat_id", chatId, "use_independent_chat_permissions", true, "permissions", Map.ofEntries(
                Map.entry("can_send_messages", canSendMessages),
                Map.entry("can_send_audios", canSendMessages),
                Map.entry("can_send_documents", canSendMessages),
                Map.entry("can_send_photos", canSendMessages),
                Map.entry("can_send_videos", canSendMessages),
                Map.entry("can_send_video_notes", canSendMessages),
                Map.entry("can_send_voice_notes", canSendMessages),
                Map.entry("can_send_polls", canSendMessages),
                Map.entry("can_send_other_messages", canSendMessages),
                Map.entry("can_add_web_page_previews", canSendMessages),
                Map.entry("can_change_info", false),
                Map.entry("can_invite_users", false),
                Map.entry("can_pin_messages", false))));
    }

    @Override
    public void leaveChat(long chatId) {
        call("leaveChat", Map.of("chat_id", chatId));
    }

    @Override
    public void answerButton(String callbackQueryId, String text, boolean alert) {
        call("answerCallbackQuery", Map.of("callback_query_id", callbackQueryId, "text", text, "show_alert", alert));
    }

    /** Returns the reply's {@code result}. Telegram's error replies have 4xx statuses, so the body is read whatever the status. */
    private JsonNode call(String method, Map<String, Object> parameters) {
        String body;
        try {
            body = restClient.post()
                    .uri("/" + method)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(mapper.writeValueAsString(parameters))
                    .exchange((request, response) -> StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8));
        } catch (RestClientException exception) {
            throw noReply(method + " failed: " + exception.getClass().getSimpleName());
        }
        JsonNode reply;
        try {
            reply = mapper.readTree(body);
        } catch (JacksonException exception) {
            throw noReply(method + " returned something other than JSON");
        }
        if (reply.path("ok").asBoolean(false)) {
            return reply.path("result");
        }
        throw toException(method, reply);
    }

    private <T> T read(JsonNode result, JavaType type) {
        try {
            return mapper.treeToValue(result, type);
        } catch (JacksonException exception) {
            throw noReply("Telegram's reply had an unexpected shape");
        }
    }

    private static TelegramApiException toException(String method, JsonNode reply) {
        JsonNode parameters = reply.path("parameters");
        Integer retryAfter = parameters.hasNonNull("retry_after") ? parameters.get("retry_after").asInt() : null;
        Long migrateTo = parameters.hasNonNull("migrate_to_chat_id") ? parameters.get("migrate_to_chat_id").asLong() : null;
        String description = method + " failed: " + reply.path("description").asString("no description");
        return new TelegramApiException(reply.path("error_code").asInt(TelegramApiException.NO_REPLY), description,
                retryAfter, migrateTo);
    }

    private static TelegramApiException noReply(String description) {
        return new TelegramApiException(TelegramApiException.NO_REPLY, description, null, null);
    }
}
