package is442t1.studybuddy.telegram.api;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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

import tools.jackson.core.JacksonException;
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
    public void sendMessage(long chatId, String text) {
        call("sendMessage", Map.of("chat_id", chatId, "text", text));
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
