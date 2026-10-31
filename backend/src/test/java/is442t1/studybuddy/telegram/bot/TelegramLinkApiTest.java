package is442t1.studybuddy.telegram.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import is442t1.studybuddy.ApiTestSupport;
import is442t1.studybuddy.telegram.api.TelegramClient;
import is442t1.studybuddy.telegram.api.Update;

/** Telegram on, with a fake client: drives the real router and database through updates. */
@TestPropertySource(properties = {
        "studybuddy.telegram.enabled=true",
        "studybuddy.telegram.bot-token=123456:TEST",
        "studybuddy.telegram.bot-username=study_test_bot",
        "studybuddy.telegram.polling=false"
})
class TelegramLinkApiTest extends ApiTestSupport {

    private static final AtomicLong NEXT_TELEGRAM_ID = new AtomicLong(1000);
    private static final String LINK_PREFIX = "https://t.me/study_test_bot?start=";

    @MockitoBean private TelegramClient client;
    @Autowired private TelegramUpdateRouter router;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ApplicationContext context;

    private String requestLink(String token) throws Exception {
        String body = mockMvc.perform(as(token, post("/api/students/me/telegram/link")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresAt").exists())
                .andReturn().getResponse().getContentAsString();
        String url = read(body, "$.url");
        assertThat(url).startsWith(LINK_PREFIX);
        return url.substring(LINK_PREFIX.length());
    }

    private void pressStart(long telegramUserId, String payload) {
        router.route(new Update(1, new Update.Message(2, new Update.Sender(telegramUserId, false, "Aisha", "aisha_r"),
                new Update.Chat(telegramUserId, "private"), "/start " + payload)));
    }

    private void expectLinked(String token, boolean linked) throws Exception {
        mockMvc.perform(as(token, get("/api/students/me/telegram")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linked").value(linked));
    }

    @Test
    void thePollerStaysOffWhenPollingIsOff() {
        assertThat(context.getBeansOfType(TelegramUpdatePoller.class)).isEmpty();
    }

    @Test
    void reportsTheBotUsername() throws Exception {
        mockMvc.perform(as(login(createStudent("Bot Reader")), get("/api/integrations")))
                .andExpect(jsonPath("$.telegram.enabled").value(true))
                .andExpect(jsonPath("$.telegram.botUsername").value("study_test_bot"));
    }

    @Test
    void pressingStartOnALinkConnectsTheStudent() throws Exception {
        String token = login(createStudent("Link Student"));
        long telegramId = NEXT_TELEGRAM_ID.incrementAndGet();
        expectLinked(token, false);

        pressStart(telegramId, requestLink(token));

        verify(client).sendMessage(telegramId, TelegramUpdateRouter.CONNECTED);
        mockMvc.perform(as(token, get("/api/students/me/telegram")))
                .andExpect(jsonPath("$.linked").value(true))
                .andExpect(jsonPath("$.username").value("aisha_r"))
                .andExpect(jsonPath("$.firstName").value("Aisha"))
                .andExpect(jsonPath("$.canMessage").value(true))
                .andExpect(jsonPath("$.linkedAt").exists());
    }

    @Test
    void theTokenItselfIsNotStored() throws Exception {
        String email = createStudent("Hash Student");
        String payload = requestLink(login(email));
        String stored = jdbc.queryForObject("select token_hash from telegram_link_token where student_id = ?", String.class, idOf(email));

        assertThat(payload).startsWith("a_").hasSizeLessThanOrEqualTo(64);
        assertThat(stored).isNotEqualTo(payload.substring(2)).hasSize(64);
    }

    @Test
    void aLinkWorksOnlyOnce() throws Exception {
        String token = login(createStudent("Once Student"));
        long telegramId = NEXT_TELEGRAM_ID.incrementAndGet();
        long stranger = NEXT_TELEGRAM_ID.incrementAndGet();
        String payload = requestLink(token);

        pressStart(telegramId, payload);
        pressStart(stranger, payload);

        verify(client).sendMessage(stranger, TelegramUpdateRouter.LINK_NOT_VALID);
        assertThat(jdbc.queryForObject("select count(*) from telegram_account where telegram_user_id = ?", Long.class, stranger)).isZero();
    }

    @Test
    void anExpiredLinkIsRefused() throws Exception {
        String email = createStudent("Expired Student");
        String token = login(email);
        long telegramId = NEXT_TELEGRAM_ID.incrementAndGet();
        String payload = requestLink(token);
        jdbc.update("update telegram_link_token set expires_at = timestamp '2020-01-01 00:00:00' where student_id = ?", idOf(email));

        pressStart(telegramId, payload);

        verify(client).sendMessage(telegramId, TelegramUpdateRouter.LINK_NOT_VALID);
        expectLinked(token, false);
    }

    @Test
    void aNewLinkReplacesTheEarlierOne() throws Exception {
        String token = login(createStudent("Replaced Student"));
        long telegramId = NEXT_TELEGRAM_ID.incrementAndGet();
        String first = requestLink(token);
        requestLink(token);

        pressStart(telegramId, first);

        verify(client).sendMessage(telegramId, TelegramUpdateRouter.LINK_NOT_VALID);
        expectLinked(token, false);
    }

    @Test
    void anUnknownLinkIsRefused() {
        long telegramId = NEXT_TELEGRAM_ID.incrementAndGet();

        pressStart(telegramId, "a_not-a-real-token");

        verify(client).sendMessage(telegramId, TelegramUpdateRouter.LINK_NOT_VALID);
    }

    @Test
    void startWithoutALinkGetsAWelcome() {
        long telegramId = NEXT_TELEGRAM_ID.incrementAndGet();

        pressStart(telegramId, "");

        verify(client).sendMessage(telegramId, TelegramUpdateRouter.WELCOME);
    }

    @Test
    void aLinkPostedInAGroupDoesNothing() throws Exception {
        String token = login(createStudent("Group Poster"));
        String payload = requestLink(token);

        router.route(new Update(1, new Update.Message(2, new Update.Sender(NEXT_TELEGRAM_ID.incrementAndGet(), false, "Eve", "eve"),
                new Update.Chat(-100L, "supergroup"), "/start " + payload)));

        verify(client, never()).sendMessage(anyLong(), anyString());
        expectLinked(token, false);
    }

    @Test
    void aSuspendedStudentsLinkIsRefused() throws Exception {
        String email = createStudent("Suspended Student");
        long telegramId = NEXT_TELEGRAM_ID.incrementAndGet();
        String payload = requestLink(login(email));
        jdbc.update("update users set status = 'SUSPENDED' where id = ?", idOf(email));

        pressStart(telegramId, payload);

        verify(client).sendMessage(telegramId, TelegramUpdateRouter.LINK_NOT_VALID);
        assertThat(jdbc.queryForObject("select count(*) from telegram_account where student_id = ?", Long.class, idOf(email))).isZero();
    }

    @Test
    void connectingAgainMovesTheStudentToTheNewTelegramAccount() throws Exception {
        String email = createStudent("Moving Student");
        String token = login(email);
        long oldId = NEXT_TELEGRAM_ID.incrementAndGet();
        long newId = NEXT_TELEGRAM_ID.incrementAndGet();

        pressStart(oldId, requestLink(token));
        pressStart(newId, requestLink(token));

        assertThat(jdbc.queryForObject("select telegram_user_id from telegram_account where student_id = ?", Long.class, idOf(email))).isEqualTo(newId);
    }

    @Test
    void theLatestStudentToLinkATelegramAccountKeepsIt() throws Exception {
        String firstToken = login(createStudent("First Owner"));
        String secondToken = login(createStudent("Second Owner"));
        long telegramId = NEXT_TELEGRAM_ID.incrementAndGet();

        pressStart(telegramId, requestLink(firstToken));
        pressStart(telegramId, requestLink(secondToken));

        expectLinked(firstToken, false);
        expectLinked(secondToken, true);
    }

    @Test
    void aStudentCanDisconnect() throws Exception {
        String token = login(createStudent("Leaving Student"));
        pressStart(NEXT_TELEGRAM_ID.incrementAndGet(), requestLink(token));

        mockMvc.perform(as(token, delete("/api/students/me/telegram"))).andExpect(status().isNoContent());

        expectLinked(token, false);
    }

    @Test
    void anotherBotsConnectionDoesNotShow() throws Exception {
        String email = createStudent("Other Bot Student");
        String token = login(email);
        pressStart(NEXT_TELEGRAM_ID.incrementAndGet(), requestLink(token));
        jdbc.update("update telegram_account set bot_id = 999 where student_id = ?", idOf(email));

        expectLinked(token, false);
    }

    @Test
    void deletingAStudentRemovesTheirConnectionAndTokens() throws Exception {
        String email = createStudent("Deleted Student");
        UUID id = idOf(email);
        String token = login(email);
        pressStart(NEXT_TELEGRAM_ID.incrementAndGet(), requestLink(token));
        requestLink(token);

        mockMvc.perform(as(login(createAdministrator()), delete("/api/admin/users/" + id))).andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("select count(*) from telegram_account where student_id = ?", Long.class, id)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from telegram_link_token where student_id = ?", Long.class, id)).isZero();
    }
}
