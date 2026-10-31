package is442t1.studybuddy.telegram.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpTelegramClientTest {

    private static final String TOKEN = "123456:SECRET";
    private static final String BASE = "https://api.telegram.org/bot" + TOKEN;

    private MockRestServiceServer server;
    private HttpTelegramClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpTelegramClient(builder, TOKEN);
    }

    private void replyWith(String method, HttpStatus status, String body) {
        server.expect(requestTo(BASE + "/" + method))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(status).contentType(MediaType.APPLICATION_JSON).body(body));
    }

    private TelegramApiException failureOfSendMessage() {
        return catchThrowableOfType(TelegramApiException.class, () -> client.sendMessage(1L, "hi"));
    }

    @Test
    void readsUpdatesAndIgnoresFieldsItDoesNotKnow() {
        server.expect(requestTo(BASE + "/getUpdates"))
                .andExpect(content().json("""
                        {"offset":7,"timeout":50,"allowed_updates":["message"]}
                        """))
                .andRespond(withSuccess("""
                        {"ok":true,"result":[{"update_id":7,"message":{"message_id":2,"date":1,
                         "from":{"id":42,"is_bot":false,"first_name":"Aisha","username":"aisha_r","language_code":"en"},
                         "chat":{"id":42,"type":"private","first_name":"Aisha"},"text":"/start a_abc"}},
                         {"update_id":8,"my_chat_member":{"chat":{"id":-1,"type":"supergroup"}}}]}
                        """, MediaType.APPLICATION_JSON));

        List<Update> updates = client.getUpdates(7, Duration.ofSeconds(50), List.of("message"));

        assertThat(updates).hasSize(2);
        Update.Message message = updates.getFirst().message();
        assertThat(updates.getFirst().updateId()).isEqualTo(7);
        assertThat(message.text()).isEqualTo("/start a_abc");
        assertThat(message.chat().isPrivate()).isTrue();
        assertThat(message.from().isBot()).isFalse();
        assertThat(message.from().firstName()).isEqualTo("Aisha");
        assertThat(message.from().username()).isEqualTo("aisha_r");
        assertThat(updates.getLast().message()).isNull();
        server.verify();
    }

    @Test
    void readsTheBotsOwnDetails() {
        replyWith("getMe", HttpStatus.OK, """
                {"ok":true,"result":{"id":123456,"is_bot":true,"first_name":"Bot","username":"study_bot"}}
                """);

        assertThat(client.getMe()).isEqualTo(new BotUser(123456, "study_bot"));
    }

    @Test
    void sendsAMessageAsSnakeCaseJson() {
        server.expect(requestTo(BASE + "/sendMessage"))
                .andExpect(content().json("""
                        {"chat_id":42,"text":"Connected ✓"}
                        """))
                .andRespond(withSuccess("{\"ok\":true,\"result\":{\"message_id\":1}}", MediaType.APPLICATION_JSON));

        client.sendMessage(42, "Connected ✓");

        server.verify();
    }

    @Test
    void sendsSetupButtonsTheWayTelegramExpects() {
        server.expect(requestTo(BASE + "/sendMessage"))
                .andExpect(content().json("""
                        {"chat_id":42,"text":"Pick","reply_markup":{"is_persistent":true,"resize_keyboard":true,"one_time_keyboard":false,
                         "keyboard":[[{"text":"Existing","request_chat":{"request_id":8,"chat_is_channel":false,"bot_is_member":true}}]]}}
                        """))
                .andExpect(content().string(not(containsString("administrator_rights"))))
                .andRespond(withSuccess("{\"ok\":true,\"result\":{\"message_id\":55}}", MediaType.APPLICATION_JSON));

        long messageId = client.sendMessage(42, "Pick", new ReplyMarkup.Keyboard(List.of(List.of(new ReplyMarkup.ChatRequestButton("Existing",
                new ReplyMarkup.ChatRequest(8, false, true, null, null)))), true, true, false));

        assertThat(messageId).isEqualTo(55);
        server.verify();
    }

    @Test
    void readsAChatSharedAndAJoinRequestFromRealShapes() {
        server.expect(requestTo(BASE + "/getUpdates"))
                .andRespond(withSuccess("""
                        {"ok":true,"result":[
                         {"update_id":1,"message":{"message_id":6,"from":{"id":42,"is_bot":false,"first_name":"A"},
                          "chat":{"id":42,"type":"private"},"date":1,"chat_shared":{"chat_id":-100,"title":"Bolt","request_id":8}}},
                         {"update_id":2,"message":{"message_id":7,"from":{"id":42,"is_bot":false,"first_name":"A"},
                          "chat":{"id":-5,"type":"group"},"date":1,"migrate_to_chat_id":-100}},
                         {"update_id":3,"my_chat_member":{"chat":{"id":-100,"type":"supergroup"},"from":{"id":42,"is_bot":false,"first_name":"A"},
                          "date":1,"old_chat_member":{"user":{"id":9,"is_bot":true,"first_name":"B"},"status":"member"},
                          "new_chat_member":{"user":{"id":9,"is_bot":true,"first_name":"B"},"status":"administrator","can_invite_users":true,"can_restrict_members":true}}},
                         {"update_id":4,"chat_join_request":{"chat":{"id":-100,"type":"supergroup"},"from":{"id":43,"is_bot":false,"first_name":"C"},
                          "user_chat_id":43,"date":1,"invite_link":{"invite_link":"https://t.me/+x","name":"inv_1","creates_join_request":true}}}]}
                        """, MediaType.APPLICATION_JSON));

        List<Update> updates = client.getUpdates(0, Duration.ofSeconds(1), List.of("message"));

        assertThat(updates.get(0).message().chatShared()).isEqualTo(new Update.ChatShared(-100, 8));
        assertThat(updates.get(1).message().migrateToChatId()).isEqualTo(-100L);
        ChatMember bot = updates.get(2).myChatMember().newChatMember();
        assertThat(bot.isAdministrator()).isTrue();
        assertThat(bot.canInviteUsers()).isTrue();
        assertThat(bot.canPinMessages()).isFalse();
        assertThat(updates.get(3).chatJoinRequest().userChatId()).isEqualTo(43);
        assertThat(updates.get(3).chatJoinRequest().inviteLink().name()).isEqualTo("inv_1");
    }

    @Test
    void reportsTelegramsErrorCodeAndText() {
        replyWith("sendMessage", HttpStatus.BAD_REQUEST, """
                {"ok":false,"error_code":400,"description":"Bad Request: can't remove chat owner"}
                """);

        TelegramApiException failure = failureOfSendMessage();

        assertThat(failure.code()).isEqualTo(400);
        assertThat(failure).hasMessageContaining("can't remove chat owner");
    }

    @Test
    void reportsAConflictFromAnotherPoller() {
        replyWith("sendMessage", HttpStatus.CONFLICT, """
                {"ok":false,"error_code":409,"description":"Conflict: terminated by other getUpdates request"}
                """);

        assertThat(failureOfSendMessage().code()).isEqualTo(409);
    }

    @Test
    void reportsHowLongToWaitWhenRateLimited() {
        replyWith("sendMessage", HttpStatus.TOO_MANY_REQUESTS, """
                {"ok":false,"error_code":429,"description":"Too Many Requests","parameters":{"retry_after":17}}
                """);

        assertThat(failureOfSendMessage().retryAfterSeconds()).isEqualTo(17);
    }

    @Test
    void reportsTheNewIdWhenAGroupWasUpgraded() {
        replyWith("sendMessage", HttpStatus.BAD_REQUEST, """
                {"ok":false,"error_code":400,"description":"Bad Request: group chat was upgraded to a supergroup chat",
                 "parameters":{"migrate_to_chat_id":-1004434864572}}
                """);

        assertThat(failureOfSendMessage().migrateToChatId()).isEqualTo(-1004434864572L);
    }

    @Test
    void reportsAReplyThatIsNotJson() {
        server.expect(requestTo(BASE + "/sendMessage"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY).contentType(MediaType.TEXT_HTML).body("<html>Bad Gateway</html>"));

        assertThat(failureOfSendMessage().code()).isEqualTo(TelegramApiException.NO_REPLY);
    }

    @Test
    void aNetworkFailureNeverCarriesTheToken() {
        server.expect(requestTo(BASE + "/sendMessage"))
                .andRespond(withException(new IOException("I/O error on POST request for \"" + BASE + "/sendMessage\"")));

        TelegramApiException failure = failureOfSendMessage();

        assertThat(failure.code()).isEqualTo(TelegramApiException.NO_REPLY);
        assertThat(failure.getMessage()).doesNotContain("SECRET");
        assertThat(failure.getCause()).isNull();
    }

    @Test
    void aFailedCallThrowsRatherThanReturningNothing() {
        replyWith("getMe", HttpStatus.UNAUTHORIZED, """
                {"ok":false,"error_code":401,"description":"Unauthorized"}
                """);

        assertThatThrownBy(() -> client.getMe()).isInstanceOf(TelegramApiException.class);
    }
}
