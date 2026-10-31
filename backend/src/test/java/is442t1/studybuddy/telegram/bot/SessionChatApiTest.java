package is442t1.studybuddy.telegram.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import is442t1.studybuddy.ApiTestSupport;
import is442t1.studybuddy.telegram.api.ChatInfo;
import is442t1.studybuddy.telegram.api.ChatInviteLink;
import is442t1.studybuddy.telegram.api.ChatMember;
import is442t1.studybuddy.telegram.api.MessageEntity;
import is442t1.studybuddy.telegram.api.ReplyMarkup;
import is442t1.studybuddy.telegram.api.TelegramApiException;
import is442t1.studybuddy.telegram.api.TelegramClient;
import is442t1.studybuddy.telegram.api.Update;
import is442t1.studybuddy.telegram.chat.SessionCardRefresher;
import is442t1.studybuddy.telegram.chat.SessionChatService;

/** Telegram on, with a fake client: session messages, answering with buttons, and reminders. */
@TestPropertySource(properties = {
        "studybuddy.telegram.enabled=true",
        "studybuddy.telegram.bot-token=123456:TEST",
        "studybuddy.telegram.bot-username=study_test_bot",
        "studybuddy.telegram.polling=false",
        "studybuddy.telegram.card-refresh=1h"
})
class SessionChatApiTest extends ApiTestSupport {

    private static final long BOT_ID = 123456;
    private static final long TIMEOUT_MS = 3000;
    private static final AtomicLong NEXT_ID = new AtomicLong(9000);
    private static final String BOT_LINK = "https://t.me/study_test_bot?start=";
    private static final String UNKNOWN_PERSON = "StudyBuddy doesn't know this Telegram account. Join the chat from your study group's page, or answer there.";
    private static final String STARTING_SOON = "Starting soon";
    private static final long CARD = 77;

    @MockitoBean private TelegramClient client;
    @Autowired private TelegramUpdateRouter router;
    @Autowired private SessionChatService sessionChat;
    @Autowired private SessionCardRefresher refresher;
    @Autowired private JdbcTemplate jdbc;

    private String leaderToken;
    private String memberToken;
    private String groupId;
    private long leaderTelegramId;
    private long chatId;

    @BeforeEach
    void createGroupWithAMember() throws Exception {
        reset(client);
        leaderTelegramId = NEXT_ID.incrementAndGet();
        chatId = -NEXT_ID.incrementAndGet();
        leaderToken = login(createStudent("Session Leader", "IS442"));
        memberToken = login(createStudent("Session Member", "IS442"));
        groupId = read(mockMvc.perform(as(leaderToken, post("/api/groups")).content("""
                        {"courseCode":"IS442","name":"Exam Crew","description":"Weekly practice",
                         "goals":["EXAM_PREPARATION"],"meetingMode":"ONLINE","maxSize":4,
                         "availability":[{"day":"TUE","start":"19:00","end":"21:00"}]}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
        mockMvc.perform(as(memberToken, post(path("/join-requests"))).content("{}")).andExpect(status().isCreated());
        String pending = read(mockMvc.perform(as(leaderToken, get(path("/join-requests"))))
                .andReturn().getResponse().getContentAsString(), "$[0].id");
        mockMvc.perform(as(leaderToken, post(path("/join-requests/" + pending + "/accept")))).andExpect(status().isOk());
        when(client.getChat(anyLong())).thenAnswer(call -> new ChatInfo(call.getArgument(0), "supergroup", "Any name"));
        when(client.getChatMember(anyLong(), eq(BOT_ID))).thenReturn(new ChatMember(null, "administrator", true, true, true, true, true));
        when(client.getChatMember(anyLong(), eq(leaderTelegramId))).thenReturn(new ChatMember(null, "creator", true, false, false, false, false));
        when(client.createJoinRequestLink(anyLong(), anyString(), any())).thenAnswer(call ->
                new ChatInviteLink("https://t.me/+" + call.getArgument(1), call.getArgument(1)));
        when(client.sendMessage(anyLong(), anyString(), any(ReplyMarkup.ActionButtons.class))).thenReturn(CARD);
    }

    private String path(String suffix) {
        return "/api/groups/" + groupId + suffix;
    }

    private static Update.Sender person(long id) {
        return new Update.Sender(id, false, "Lee", null);
    }

    private void linkChat() throws Exception {
        String url = read(mockMvc.perform(as(leaderToken, post(path("/chat/setup"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.url");
        router.route(new Update(1, new Update.Message(2, person(leaderTelegramId), new Update.Chat(leaderTelegramId, "private"),
                "/start " + url.substring(BOT_LINK.length()))));
        ArgumentCaptor<ReplyMarkup> markup = ArgumentCaptor.forClass(ReplyMarkup.class);
        verify(client).sendMessage(eq(leaderTelegramId), anyString(), markup.capture());
        int requestId = ((ReplyMarkup.Keyboard) markup.getValue()).keyboard().getFirst().getFirst().requestChat().requestId();
        router.route(new Update(1, new Update.Message(3, person(leaderTelegramId), new Update.Chat(leaderTelegramId, "private"), null,
                new Update.ChatShared(chatId, requestId), null, null)));
    }

    private static String sessionBody(String title, Instant startsAt) {
        return """
                {"title":"%s","startsAt":"%s","endsAt":"%s","meetingMode":"IN_PERSON","location":"SCIS SR 2-1","note":"Bring notes"}"""
                .formatted(title, startsAt, startsAt.plus(Duration.ofHours(2)));
    }

    private String schedule(Instant startsAt) throws Exception {
        return read(mockMvc.perform(as(leaderToken, post(path("/sessions"))).content(sessionBody("Mock exam", startsAt)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    private void reschedule(String sessionId, Instant startsAt) throws Exception {
        mockMvc.perform(as(leaderToken, put(path("/sessions/" + sessionId))).content(sessionBody("Mock exam", startsAt)))
                .andExpect(status().isOk());
    }

    private static Instant inHours(long hours) {
        return Instant.now().plus(Duration.ofHours(hours)).truncatedTo(ChronoUnit.SECONDS);
    }

    private void press(long telegramUserId, String data) {
        router.route(new Update(1, null, null, null, null, new Update.CallbackQuery("press-1", person(telegramUserId), data)));
    }

    private void expectMyRsvp(String token, String expected) throws Exception {
        mockMvc.perform(as(token, get(path("/sessions")))).andExpect(status().isOk()).andExpect(jsonPath("$[0].myRsvp").value(expected));
    }

    /** The refresher runs on a timer, which these tests turn off so that they can say exactly when it happens. */
    private String posted(String sessionId) {
        refresher.flush();
        return sessionId;
    }

    private void verifyReminders(int count) {
        verify(client, times(count)).sendReply(eq(chatId), eq(CARD), contains(STARTING_SOON), any());
    }

    private void verifyCardsPosted(int count) {
        verify(client, times(count)).sendMessage(eq(chatId), anyString(), any(ReplyMarkup.ActionButtons.class));
    }

    /** The text of the latest edit to the card, which must have its buttons. */
    private String editedCard() {
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(client, atLeastOnce()).editMessage(eq(chatId), eq(CARD), text.capture(), any(ReplyMarkup.ActionButtons.class));
        return text.getValue();
    }

    private void setTimes(String sessionId, Instant startsAt, Instant endsAt) {
        jdbc.update("update study_session set starts_at = ?, ends_at = ? where id = ?", Timestamp.from(startsAt), Timestamp.from(endsAt), UUID.fromString(sessionId));
    }

    @Test
    void schedulingPostsACardWithAnswerButtons() throws Exception {
        linkChat();

        String sessionId = posted(schedule(inHours(24)));

        ArgumentCaptor<ReplyMarkup> markup = ArgumentCaptor.forClass(ReplyMarkup.class);
        verify(client).sendMessage(eq(chatId), contains("📅 Mock exam"), markup.capture());
        ReplyMarkup.ActionButtons buttons = (ReplyMarkup.ActionButtons) markup.getValue();
        assertThat(buttons.inlineKeyboard().getFirst()).extracting(ReplyMarkup.ActionButtons.Action::callbackData)
                .containsExactly("rsvp:" + sessionId + ":G", "rsvp:" + sessionId + ":N");
    }

    @Test
    void aPressUpdatesTheSameCardAndPostsNothingNew() throws Exception {
        linkChat();
        String sessionId = posted(schedule(inHours(24)));

        press(leaderTelegramId, "rsvp:" + sessionId + ":G");
        refresher.flush();

        assertThat(editedCard()).contains("✅ Going (1): Session Leader").contains("❓ No answer (1): Session Member");
        verifyCardsPosted(1);
    }

    @Test
    void aBurstOfPressesBecomesOneEdit() throws Exception {
        linkChat();
        String sessionId = posted(schedule(inHours(24)));
        long memberTelegramId = connectTelegram(memberToken);

        press(leaderTelegramId, "rsvp:" + sessionId + ":G");
        press(memberTelegramId, "rsvp:" + sessionId + ":N");
        press(leaderTelegramId, "rsvp:" + sessionId + ":N");
        refresher.flush();

        verify(client, times(1)).editMessage(eq(chatId), eq(CARD), anyString(), any(ReplyMarkup.class));
        assertThat(editedCard()).contains("❌ Can't (2): Session Leader, Session Member");
    }

    @Test
    void answeringInTheAppUpdatesTheCardToo() throws Exception {
        linkChat();
        String sessionId = posted(schedule(inHours(24)));

        mockMvc.perform(as(memberToken, put(path("/sessions/" + sessionId + "/rsvp"))).content("{\"response\":\"GOING\"}")).andExpect(status().isOk());
        refresher.flush();

        assertThat(editedCard()).contains("✅ Going (1): Session Member");
    }

    @Test
    void anEditThatChangesNothingCountsAsDone() throws Exception {
        linkChat();
        String sessionId = posted(schedule(inHours(24)));
        doThrow(new TelegramApiException(400, "editMessageText failed: Bad Request: message is not modified", null, null))
                .when(client).editMessage(eq(chatId), eq(CARD), anyString(), any(ReplyMarkup.class));

        press(leaderTelegramId, "rsvp:" + sessionId + ":G");
        refresher.flush();
        refresher.flush();

        verify(client, times(1)).editMessage(eq(chatId), eq(CARD), anyString(), any(ReplyMarkup.class));
        verifyCardsPosted(1);
    }

    @Test
    void aCardSomeoneDeletedIsPostedAgain() throws Exception {
        linkChat();
        String sessionId = posted(schedule(inHours(24)));
        doThrow(new TelegramApiException(400, "editMessageText failed: Bad Request: message to edit not found", null, null))
                .when(client).editMessage(eq(chatId), eq(CARD), anyString(), any(ReplyMarkup.class));

        press(leaderTelegramId, "rsvp:" + sessionId + ":G");
        refresher.flush();
        refresher.flush();

        verifyCardsPosted(2);
    }

    @Test
    void anEditDuringAnOutageIsTriedAgain() throws Exception {
        linkChat();
        String sessionId = posted(schedule(inHours(24)));
        doThrow(new TelegramApiException(502, "down", null, null)).doNothing()
                .when(client).editMessage(eq(chatId), eq(CARD), anyString(), any(ReplyMarkup.class));

        press(leaderTelegramId, "rsvp:" + sessionId + ":G");
        refresher.flush();
        refresher.flush();

        verify(client, times(2)).editMessage(eq(chatId), eq(CARD), anyString(), any(ReplyMarkup.class));
    }

    @Test
    void changingDetailsEditsTheCardQuietly() throws Exception {
        linkChat();
        Instant startsAt = inHours(24);
        String sessionId = posted(schedule(startsAt));
        press(leaderTelegramId, "rsvp:" + sessionId + ":G");

        mockMvc.perform(as(leaderToken, put(path("/sessions/" + sessionId))).content(sessionBody("Mock exam", startsAt).replace("Bring notes", "Bring a calculator")))
                .andExpect(status().isOk());
        refresher.flush();

        assertThat(editedCard()).contains("Bring a calculator").contains("✅ Going (1): Session Leader");
        verifyCardsPosted(1);
        verify(client, never()).sendReply(eq(chatId), anyLong(), anyString(), any());
    }

    @Test
    void savingWithoutChangesDoesNothing() throws Exception {
        linkChat();
        Instant startsAt = inHours(24);
        String sessionId = posted(schedule(startsAt));

        reschedule(sessionId, startsAt);
        refresher.flush();

        verify(client, never()).editMessage(eq(chatId), anyLong(), anyString(), any(ReplyMarkup.class));
        verify(client, after(300).never()).sendReply(eq(chatId), anyLong(), anyString(), any());
    }

    @Test
    void movingTheStartClearsAnswersAndAsksPeopleAgain() throws Exception {
        linkChat();
        String sessionId = posted(schedule(inHours(24)));
        press(leaderTelegramId, "rsvp:" + sessionId + ":G");
        refresher.flush();

        reschedule(sessionId, inHours(30));
        refresher.flush();

        assertThat(editedCard()).contains("✅ Going (0)").contains("❓ No answer (2)");
        verify(client, timeout(TIMEOUT_MS)).sendReply(eq(chatId), eq(CARD), contains("Please answer again"), any());
        mockMvc.perform(as(leaderToken, get(path("/sessions")))).andExpect(jsonPath("$[0].goingCount").value(0));
    }

    @Test
    void cancellingShowsOnTheCardRemovesItsButtonsAndNotifiesOnce() throws Exception {
        linkChat();
        String sessionId = posted(schedule(inHours(24)));

        mockMvc.perform(as(leaderToken, post(path("/sessions/" + sessionId + "/cancel")))).andExpect(status().isOk());
        refresher.flush();

        verify(client).editMessage(eq(chatId), eq(CARD), contains("❌ Cancelled: Mock exam"));
        verify(client, timeout(TIMEOUT_MS)).sendReply(eq(chatId), eq(CARD), contains("has been cancelled"), any());
        verify(client).unpinMessage(chatId, CARD);
    }

    @Test
    void theSoonestUpcomingSessionIsThePinnedOne() throws Exception {
        linkChat();
        when(client.sendMessage(eq(chatId), anyString(), any(ReplyMarkup.ActionButtons.class))).thenReturn(101L).thenReturn(102L);
        schedule(inHours(48));
        refresher.flush();
        verify(client).pinMessage(chatId, 101);

        schedule(inHours(24));
        refresher.flush();

        verify(client).pinMessage(chatId, 102);
        verify(client).unpinMessage(chatId, 101);
    }

    @Test
    void theButtonsGoWhenASessionStartsAndThePinWhenItEnds() throws Exception {
        linkChat();
        String sessionId = posted(schedule(inHours(24)));

        setTimes(sessionId, Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600));
        sessionChat.sweep(Instant.now());
        refresher.flush();
        verify(client).editMessage(eq(chatId), eq(CARD), contains("🟢 Happening now: Mock exam"));
        verify(client, never()).unpinMessage(chatId, CARD);

        setTimes(sessionId, Instant.now().minusSeconds(7200), Instant.now().minusSeconds(60));
        sessionChat.sweep(Instant.now());
        refresher.flush();
        verify(client).editMessage(eq(chatId), eq(CARD), contains("✔️ Finished: Mock exam"));
        verify(client).unpinMessage(chatId, CARD);

        sessionChat.sweep(Instant.now());
        refresher.flush();
        verify(client, times(2)).editMessage(eq(chatId), eq(CARD), anyString());
    }

    @Test
    void theSweepPostsACardThatFailedToPost() throws Exception {
        linkChat();
        doThrow(new TelegramApiException(400, "refused", null, null)).doReturn(CARD)
                .when(client).sendMessage(eq(chatId), anyString(), any(ReplyMarkup.ActionButtons.class));
        posted(schedule(inHours(24)));
        verifyCardsPosted(1);

        sessionChat.sweep(Instant.now());
        refresher.flush();

        verifyCardsPosted(2);
        mockMvc.perform(as(leaderToken, get(path("/sessions")))).andExpect(jsonPath("$[0].title").value("Mock exam"));
    }

    @Test
    void linkingAChatLatePostsCardsForUpcomingSessions() throws Exception {
        schedule(inHours(24));
        refresher.flush();
        verify(client, never()).sendMessage(eq(chatId), anyString(), any(ReplyMarkup.ActionButtons.class));

        linkChat();
        sessionChat.sweep(Instant.now());
        refresher.flush();

        verifyCardsPosted(1);
    }

    @Test
    void aMemberLeavingUpdatesTheCard() throws Exception {
        linkChat();
        posted(schedule(inHours(24)));

        mockMvc.perform(as(memberToken, post(path("/leave")))).andExpect(status().isNoContent());

        await().atMost(Duration.ofMillis(TIMEOUT_MS)).untilAsserted(() -> {
            refresher.flush();
            verify(client, atLeastOnce()).editMessage(eq(chatId), eq(CARD), argThat(text -> !text.contains("Session Member")), any(ReplyMarkup.class));
        });
    }

    @Test
    void aGroupWithoutAChatStillSchedulesAndPostsNothing() throws Exception {
        String sessionId = posted(schedule(inHours(24)));

        assertThat(jdbc.queryForObject("select count(*) from session_chat_card where study_session_id = ?", Long.class, UUID.fromString(sessionId))).isZero();
    }

    @Test
    void someoneWhoJoinedTheChatCanAnswerWithAButton() throws Exception {
        linkChat();
        String sessionId = schedule(inHours(24));
        long memberTelegramId = NEXT_ID.incrementAndGet();
        String invite = read(mockMvc.perform(as(memberToken, post(path("/chat/invite"))))
                .andReturn().getResponse().getContentAsString(), "$.me.inviteUrl");
        router.route(new Update(1, null, null, null, new Update.ChatJoinRequest(new Update.Chat(chatId, "supergroup"), person(memberTelegramId),
                memberTelegramId, new ChatInviteLink(invite, invite.substring("https://t.me/+".length())))));

        press(memberTelegramId, "rsvp:" + sessionId + ":G");

        verify(client).answerButton("press-1", "Going · 1 going", false);
        expectMyRsvp(memberToken, "GOING");

        press(memberTelegramId, "rsvp:" + sessionId + ":N");

        verify(client).answerButton("press-1", "Can't make it · 0 going", false);
        expectMyRsvp(memberToken, "NOT_GOING");
    }

    @Test
    void theLeaderCanAnswerFromTheChatTheyLinked() throws Exception {
        linkChat();
        String sessionId = schedule(inHours(24));

        press(leaderTelegramId, "rsvp:" + sessionId + ":G");

        expectMyRsvp(leaderToken, "GOING");
    }

    @Test
    void aConnectedAccountCanAnswerWithoutHavingJoinedThroughAnInvite() throws Exception {
        linkChat();
        String sessionId = schedule(inHours(24));
        long memberTelegramId = connectTelegram(memberToken);

        press(memberTelegramId, "rsvp:" + sessionId + ":G");

        expectMyRsvp(memberToken, "GOING");
    }

    private long connectTelegram(String token) throws Exception {
        long telegramId = NEXT_ID.incrementAndGet();
        String link = read(mockMvc.perform(as(token, post("/api/students/me/telegram/link")))
                .andReturn().getResponse().getContentAsString(), "$.url");
        router.route(new Update(1, new Update.Message(2, person(telegramId), new Update.Chat(telegramId, "private"), "/start " + link.substring(BOT_LINK.length()))));
        return telegramId;
    }

    @Test
    void anUnknownPersonGetsAnAlertAndNothingIsSaved() throws Exception {
        linkChat();
        String sessionId = schedule(inHours(24));

        press(NEXT_ID.incrementAndGet(), "rsvp:" + sessionId + ":G");

        verify(client).answerButton("press-1", UNKNOWN_PERSON, true);
        mockMvc.perform(as(leaderToken, get(path("/sessions")))).andExpect(jsonPath("$[0].goingCount").value(0));
    }

    @Test
    void someoneWhoLeftTheStudyGroupCannotAnswer() throws Exception {
        linkChat();
        String sessionId = schedule(inHours(24));
        long memberTelegramId = connectTelegram(memberToken);
        mockMvc.perform(as(memberToken, post(path("/leave")))).andExpect(status().isNoContent());

        press(memberTelegramId, "rsvp:" + sessionId + ":G");

        verify(client).answerButton("press-1", "Only group members can respond.", true);
    }

    @Test
    void aButtonOnASessionThatHasStartedSaysSo() throws Exception {
        linkChat();
        String sessionId = schedule(inHours(24));
        jdbc.update("update study_session set starts_at = ? where id = ?", Timestamp.from(Instant.now().minusSeconds(60)), UUID.fromString(sessionId));

        press(leaderTelegramId, "rsvp:" + sessionId + ":G");

        verify(client).answerButton("press-1", "This session has already started.", true);
    }

    @Test
    void aButtonOnACancelledSessionSaysSo() throws Exception {
        linkChat();
        String sessionId = schedule(inHours(24));
        mockMvc.perform(as(leaderToken, post(path("/sessions/" + sessionId + "/cancel")))).andExpect(status().isOk());

        press(leaderTelegramId, "rsvp:" + sessionId + ":G");

        verify(client).answerButton("press-1", "This session has been cancelled.", true);
    }

    @Test
    void aButtonStudyBuddyDidNotMakeIsAnswered() {
        press(leaderTelegramId, "something-else");

        verify(client).answerButton("press-1", "This button no longer works.", true);
    }

    @Test
    void aReminderGoesOutOnceInsideTheWindow() throws Exception {
        linkChat();
        Instant startsAt = inHours(3);
        posted(schedule(startsAt));

        sessionChat.sendDueReminders(startsAt.minus(Duration.ofMinutes(61)));
        verifyReminders(0);

        sessionChat.sendDueReminders(startsAt.minus(Duration.ofMinutes(30)));
        sessionChat.sendDueReminders(startsAt.minus(Duration.ofMinutes(29)));
        verifyReminders(1);

        sessionChat.sendDueReminders(startsAt.plusSeconds(1));
        verifyReminders(1);
    }

    @Test
    void aSessionScheduledInsideTheWindowGetsNoReminderAsWell() throws Exception {
        linkChat();
        Instant startsAt = Instant.now().plus(Duration.ofMinutes(30));
        posted(schedule(startsAt));

        sessionChat.sendDueReminders(startsAt.minus(Duration.ofMinutes(10)));

        verifyReminders(0);
    }

    @Test
    void aCancelledSessionGetsNoReminder() throws Exception {
        linkChat();
        Instant startsAt = inHours(3);
        String sessionId = schedule(startsAt);
        mockMvc.perform(as(leaderToken, post(path("/sessions/" + sessionId + "/cancel")))).andExpect(status().isOk());

        sessionChat.sendDueReminders(startsAt.minus(Duration.ofMinutes(30)));

        verifyReminders(0);
    }

    @Test
    void aFailedReminderIsTriedAgainOnTheNextRun() throws Exception {
        linkChat();
        Instant startsAt = inHours(3);
        posted(schedule(startsAt));
        doThrow(new TelegramApiException(500, "down", null, null))
                .doReturn(9L)
                .when(client).sendReply(eq(chatId), eq(CARD), contains(STARTING_SOON), any());

        sessionChat.sendDueReminders(startsAt.minus(Duration.ofMinutes(40)));
        verifyReminders(1);

        sessionChat.sendDueReminders(startsAt.minus(Duration.ofMinutes(39)));
        verifyReminders(2);

        sessionChat.sendDueReminders(startsAt.minus(Duration.ofMinutes(38)));
        verifyReminders(2);
    }

    @Test
    void aSessionWithNoCardYetWaitsForOneBeforeItsReminder() throws Exception {
        linkChat();
        Instant startsAt = inHours(3);
        schedule(startsAt);

        sessionChat.sendDueReminders(startsAt.minus(Duration.ofMinutes(30)));
        verifyReminders(0);

        refresher.flush();
        jdbc.update("update session_chat_card set announced_at = ? where group_chat_id = (select id from group_chat where chat_id = ?)",
                Timestamp.from(startsAt.minus(Duration.ofHours(2))), chatId);
        sessionChat.sendDueReminders(startsAt.minus(Duration.ofMinutes(30)));
        verifyReminders(1);
    }

    @Test
    void movingTheStartAfterAReminderAllowsAnotherForTheNewTime() throws Exception {
        linkChat();
        Instant startsAt = inHours(3);
        String sessionId = posted(schedule(startsAt));
        sessionChat.sendDueReminders(startsAt.minus(Duration.ofMinutes(30)));
        verifyReminders(1);

        Instant newStart = inHours(8);
        reschedule(sessionId, newStart);
        verify(client, timeout(TIMEOUT_MS)).sendReply(eq(chatId), eq(CARD), contains("Please answer again"), any());
        sessionChat.sendDueReminders(newStart.minus(Duration.ofMinutes(30)));

        verifyReminders(2);
    }

    @Test
    void aSessionMovedInsideTheWindowGetsNoReminderAsWell() throws Exception {
        linkChat();
        String sessionId = posted(schedule(inHours(24)));
        Instant newStart = Instant.now().plus(Duration.ofMinutes(30));

        reschedule(sessionId, newStart);
        verify(client, timeout(TIMEOUT_MS)).sendReply(eq(chatId), eq(CARD), contains("Please answer again"), any());
        sessionChat.sendDueReminders(newStart.minus(Duration.ofMinutes(10)));

        verifyReminders(0);
    }

    @Test
    void changingOnlyDetailsAfterAReminderDoesNotSendAnother() throws Exception {
        linkChat();
        Instant startsAt = inHours(3);
        String sessionId = posted(schedule(startsAt));
        sessionChat.sendDueReminders(startsAt.minus(Duration.ofMinutes(30)));

        mockMvc.perform(as(leaderToken, put(path("/sessions/" + sessionId))).content(sessionBody("Mock exam", startsAt).replace("Bring notes", "Bring a calculator")))
                .andExpect(status().isOk());
        sessionChat.sendDueReminders(startsAt.minus(Duration.ofMinutes(29)));

        verifyReminders(1);
    }

    @Test
    void anotherBotsChatGetsNoCardsOrReminders() throws Exception {
        linkChat();
        jdbc.update("update group_chat set bot_id = 999 where chat_id = ?", chatId);
        Instant startsAt = inHours(3);
        posted(schedule(startsAt));

        sessionChat.sendDueReminders(startsAt.minus(Duration.ofMinutes(30)));

        verifyCardsPosted(0);
        verifyReminders(0);
    }

    @Test
    void aChatTheBotWasRemovedFromGetsNoReminder() throws Exception {
        linkChat();
        Instant startsAt = inHours(3);
        posted(schedule(startsAt));
        router.route(new Update(1, null, new Update.ChatMemberUpdated(new Update.Chat(chatId, "supergroup"), person(leaderTelegramId), null,
                new ChatMember(null, "left", false, false, false, false, false), null), null, null));

        sessionChat.sendDueReminders(startsAt.minus(Duration.ofMinutes(30)));

        verifyReminders(0);
    }

    @Test
    void closingTheGroupCancelsTheCardsAndStopsReminders() throws Exception {
        linkChat();
        Instant startsAt = inHours(3);
        posted(schedule(startsAt));

        mockMvc.perform(as(leaderToken, post(path("/close")))).andExpect(status().isOk());
        await().atMost(Duration.ofMillis(TIMEOUT_MS)).untilAsserted(() -> {
            refresher.flush();
            verify(client).editMessage(eq(chatId), eq(CARD), contains("❌ Cancelled: Mock exam"));
        });
        sessionChat.sendDueReminders(startsAt.minus(Duration.ofMinutes(30)));

        verifyReminders(0);
    }

    private static String videoChatBody(Instant startsAt) {
        return """
                {"title":"Mock exam","startsAt":"%s","endsAt":"%s","meetingMode":"ONLINE","groupVideoChat":true}"""
                .formatted(startsAt, startsAt.plus(Duration.ofHours(2)));
    }

    private String scheduleVideoChat(Instant startsAt) throws Exception {
        return read(mockMvc.perform(as(leaderToken, post(path("/sessions"))).content(videoChatBody(startsAt)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groupVideoChat").value(true))
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    private long joinChatAndAnswerGoing(String sessionId) throws Exception {
        long memberTelegramId = NEXT_ID.incrementAndGet();
        String invite = read(mockMvc.perform(as(memberToken, post(path("/chat/invite"))))
                .andReturn().getResponse().getContentAsString(), "$.me.inviteUrl");
        router.route(new Update(1, null, null, null, new Update.ChatJoinRequest(new Update.Chat(chatId, "supergroup"), person(memberTelegramId),
                memberTelegramId, new ChatInviteLink(invite, invite.substring("https://t.me/+".length())))));
        press(memberTelegramId, "rsvp:" + sessionId + ":G");
        return memberTelegramId;
    }

    private void callStartedBy(long telegramUserId) {
        router.route(new Update(1, new Update.Message(4, person(telegramUserId), new Update.Chat(chatId, "supergroup"), null, null, null, null,
                new Update.VideoChatStarted())));
    }

    @Test
    void aMeetingLinkIsPostedWithTheSession() throws Exception {
        linkChat();
        Instant startsAt = inHours(24);

        mockMvc.perform(as(leaderToken, post(path("/sessions"))).content("""
                        {"title":"Mock exam","startsAt":"%s","endsAt":"%s","meetingMode":"ONLINE","meetingLink":"https://meet.example.com/abc"}"""
                        .formatted(startsAt, startsAt.plus(Duration.ofHours(1)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.meetingLink").value("https://meet.example.com/abc"));

        refresher.flush();
        verify(client).sendMessage(eq(chatId), contains("Join: https://meet.example.com/abc"), any(ReplyMarkup.class));
    }

    @Test
    void aVideoChatReminderTagsThoseGoing() throws Exception {
        linkChat();
        Instant startsAt = inHours(3);
        String sessionId = posted(scheduleVideoChat(startsAt));
        long memberTelegramId = joinChatAndAnswerGoing(sessionId);

        sessionChat.sendDueReminders(startsAt.minus(Duration.ofMinutes(30)));

        ArgumentCaptor<List<MessageEntity>> entities = ArgumentCaptor.captor();
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(client).sendReply(eq(chatId), eq(CARD), text.capture(), entities.capture());
        assertThat(text.getValue()).contains(STARTING_SOON).contains("Going: Session Member");
        MessageEntity tag = entities.getValue().getFirst();
        assertThat(tag.type()).isEqualTo("text_mention");
        assertThat(tag.user().id()).isEqualTo(memberTelegramId);
        assertThat(text.getValue().substring(tag.offset(), tag.offset() + tag.length())).isEqualTo("Session Member");
    }

    @Test
    void startingTheCallTagsTheOthersGoingButNotWhoStartedIt() throws Exception {
        linkChat();
        String sessionId = scheduleVideoChat(Instant.now().plus(Duration.ofMinutes(20)));
        long memberTelegramId = joinChatAndAnswerGoing(sessionId);
        press(leaderTelegramId, "rsvp:" + sessionId + ":G");

        callStartedBy(leaderTelegramId);

        ArgumentCaptor<List<MessageEntity>> entities = ArgumentCaptor.captor();
        verify(client).sendMessage(eq(chatId), contains("video chat for Mock exam has started"), entities.capture());
        assertThat(entities.getValue()).extracting(entity -> entity.user().id()).containsExactly(memberTelegramId);
    }

    @Test
    void aCallLongBeforeTheSessionTagsNobody() throws Exception {
        linkChat();
        String sessionId = scheduleVideoChat(inHours(3));
        joinChatAndAnswerGoing(sessionId);

        callStartedBy(leaderTelegramId);

        verify(client, never()).sendMessage(eq(chatId), contains("has started"));
        verify(client, never()).sendMessage(eq(chatId), contains("has started"), any(List.class));
    }

    @Test
    void aCallDuringASessionWithAMeetingLinkTagsNobody() throws Exception {
        linkChat();
        schedule(Instant.now().plus(Duration.ofMinutes(20)));

        callStartedBy(leaderTelegramId);

        verify(client, never()).sendMessage(eq(chatId), contains("has started"));
        verify(client, never()).sendMessage(eq(chatId), contains("has started"), any(List.class));
    }

    @Test
    void anUnexpectedErrorStillAnswersThePress() throws Exception {
        linkChat();
        String sessionId = posted(schedule(inHours(24)));
        long memberTelegramId = joinChatAndAnswerGoing(sessionId);
        doThrow(new IllegalStateException("boom")).when(client).answerButton(anyString(), anyString(), eq(false));

        press(memberTelegramId, "rsvp:" + sessionId + ":N");

        verify(client).answerButton("press-1", "That didn't save. Please try again, or answer in StudyBuddy.", true);
    }

    @Test
    void someoneNotGoingIsNotTagged() throws Exception {
        linkChat();
        Instant startsAt = inHours(3);
        String sessionId = posted(scheduleVideoChat(startsAt));
        long memberTelegramId = joinChatAndAnswerGoing(sessionId);
        press(memberTelegramId, "rsvp:" + sessionId + ":N");

        sessionChat.sendDueReminders(startsAt.minus(Duration.ofMinutes(30)));

        ArgumentCaptor<List<MessageEntity>> entities = ArgumentCaptor.captor();
        verify(client).sendReply(eq(chatId), eq(CARD), contains(STARTING_SOON), entities.capture());
        assertThat(entities.getValue()).isEmpty();
    }

    @Test
    void refusesAnUnsafeOrMisplacedMeetingOption() throws Exception {
        Instant startsAt = inHours(24);
        Instant endsAt = startsAt.plus(Duration.ofHours(1));
        for (String extra : new String[] {
                "\"meetingMode\":\"ONLINE\",\"meetingLink\":\"javascript:alert(1)\"",
                "\"meetingMode\":\"ONLINE\",\"meetingLink\":\"meet.example.com\"",
                "\"meetingMode\":\"IN_PERSON\",\"meetingLink\":\"https://meet.example.com\"",
                "\"meetingMode\":\"IN_PERSON\",\"groupVideoChat\":true",
                "\"meetingMode\":\"ONLINE\",\"groupVideoChat\":true,\"meetingLink\":\"https://meet.example.com\""}) {
            mockMvc.perform(as(leaderToken, post(path("/sessions"))).content(
                            "{\"title\":\"Mock\",\"startsAt\":\"%s\",\"endsAt\":\"%s\",%s}".formatted(startsAt, endsAt, extra)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void theCalendarFileSaysHowToJoin() throws Exception {
        Instant startsAt = inHours(24);
        String id = read(mockMvc.perform(as(leaderToken, post(path("/sessions"))).content("""
                        {"title":"Mock","startsAt":"%s","endsAt":"%s","meetingMode":"ONLINE","meetingLink":"https://m.example.com/a"}"""
                        .formatted(startsAt, startsAt.plus(Duration.ofHours(1)))))
                .andReturn().getResponse().getContentAsString(), "$.id");

        String ics = mockMvc.perform(as(leaderToken, get(path("/sessions/" + id + "/calendar.ics"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(ics.replace("\r\n ", "")).contains("Join: https://m.example.com/a");
    }
}
