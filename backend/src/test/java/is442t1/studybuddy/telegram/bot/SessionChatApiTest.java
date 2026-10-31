package is442t1.studybuddy.telegram.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
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
import is442t1.studybuddy.telegram.chat.SessionChatService;

/** Telegram on, with a fake client: session messages, answering with buttons, and reminders. */
@TestPropertySource(properties = {
        "studybuddy.telegram.enabled=true",
        "studybuddy.telegram.bot-token=123456:TEST",
        "studybuddy.telegram.bot-username=study_test_bot",
        "studybuddy.telegram.polling=false"
})
class SessionChatApiTest extends ApiTestSupport {

    private static final long BOT_ID = 123456;
    private static final long TIMEOUT_MS = 3000;
    private static final AtomicLong NEXT_ID = new AtomicLong(9000);
    private static final String BOT_LINK = "https://t.me/study_test_bot?start=";
    private static final String UNKNOWN_PERSON = "StudyBuddy doesn't know this Telegram account. Join the chat from your study group's page, or answer there.";
    private static final String STARTING_SOON = "Starting soon";

    @MockitoBean private TelegramClient client;
    @Autowired private TelegramUpdateRouter router;
    @Autowired private SessionChatService sessionChat;
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

    private void verifyReminders(int count) {
        verify(client, times(count)).sendMessage(eq(chatId), contains(STARTING_SOON), any(ReplyMarkup.class));
    }

    @Test
    void schedulingPostsTheSessionWithAnswerButtons() throws Exception {
        linkChat();

        String sessionId = schedule(inHours(24));

        ArgumentCaptor<ReplyMarkup> markup = ArgumentCaptor.forClass(ReplyMarkup.class);
        verify(client, timeout(TIMEOUT_MS)).sendMessage(eq(chatId), contains("New study session: Mock exam"), markup.capture());
        ReplyMarkup.ActionButtons buttons = (ReplyMarkup.ActionButtons) markup.getValue();
        assertThat(buttons.inlineKeyboard().getFirst()).extracting(ReplyMarkup.ActionButtons.Action::callbackData)
                .containsExactly("rsvp:" + sessionId + ":G", "rsvp:" + sessionId + ":N");
    }

    @Test
    void changingAndCancellingArePosted() throws Exception {
        linkChat();
        String sessionId = schedule(inHours(24));

        reschedule(sessionId, inHours(30));
        verify(client, timeout(TIMEOUT_MS)).sendMessage(eq(chatId), contains("updated: Mock exam"), any(ReplyMarkup.ActionButtons.class));

        mockMvc.perform(as(leaderToken, post(path("/sessions/" + sessionId + "/cancel")))).andExpect(status().isOk());
        verify(client, timeout(TIMEOUT_MS)).sendMessage(eq(chatId), contains("cancelled: Mock exam"), any(ReplyMarkup.RemoveKeyboard.class));
    }

    @Test
    void aGroupWithoutAChatStillSchedulesAndPostsNothing() throws Exception {
        schedule(inHours(24));

        verify(client, after(300).never()).sendMessage(anyLong(), contains("New study session"), any(ReplyMarkup.class));
    }

    @Test
    void aFailedPostDoesNotUndoTheSession() throws Exception {
        linkChat();
        doThrow(new TelegramApiException(500, "down", null, null)).when(client).sendMessage(eq(chatId), contains("New study session"), any(ReplyMarkup.class));

        schedule(inHours(24));

        verify(client, timeout(TIMEOUT_MS)).sendMessage(eq(chatId), contains("New study session"), any(ReplyMarkup.class));
        mockMvc.perform(as(leaderToken, get(path("/sessions")))).andExpect(jsonPath("$[0].title").value("Mock exam"));
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
        schedule(startsAt);

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
        schedule(startsAt);

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
    void aFailedReminderIsTriedAgainOnceItsClaimExpires() throws Exception {
        linkChat();
        Instant startsAt = inHours(3);
        schedule(startsAt);
        doThrow(new TelegramApiException(500, "down", null, null))
                .doReturn(9L)
                .when(client).sendMessage(eq(chatId), contains(STARTING_SOON), any(ReplyMarkup.class));

        sessionChat.sendDueReminders(startsAt.minus(Duration.ofMinutes(40)));
        sessionChat.sendDueReminders(startsAt.minus(Duration.ofMinutes(35)));
        verifyReminders(1);

        sessionChat.sendDueReminders(startsAt.minus(Duration.ofMinutes(29)));
        verifyReminders(2);

        sessionChat.sendDueReminders(startsAt.minus(Duration.ofMinutes(10)));
        verifyReminders(2);
    }

    @Test
    void reschedulingAfterAReminderAllowsAnotherForTheNewTime() throws Exception {
        linkChat();
        Instant startsAt = inHours(3);
        String sessionId = schedule(startsAt);
        sessionChat.sendDueReminders(startsAt.minus(Duration.ofMinutes(30)));
        verifyReminders(1);

        Instant newStart = inHours(8);
        reschedule(sessionId, newStart);
        sessionChat.sendDueReminders(newStart.minus(Duration.ofMinutes(30)));

        verifyReminders(2);
    }

    @Test
    void reschedulingWhileAReminderIsBeingSentKeepsTheNextOne() throws Exception {
        linkChat();
        Instant startsAt = inHours(3);
        String sessionId = schedule(startsAt);
        Instant newStart = inHours(8);
        doAnswer(call -> {
            reschedule(sessionId, newStart);
            return 9L;
        }).doReturn(10L).when(client).sendMessage(eq(chatId), contains(STARTING_SOON), any(ReplyMarkup.class));

        sessionChat.sendDueReminders(startsAt.minus(Duration.ofMinutes(30)));
        sessionChat.sendDueReminders(newStart.minus(Duration.ofMinutes(30)));

        verifyReminders(2);
    }

    @Test
    void claimingAReminderDoesNotCountAsChangingTheSession() throws Exception {
        linkChat();
        Instant startsAt = inHours(3);
        String sessionId = schedule(startsAt);
        Timestamp before = jdbc.queryForObject("select updated_at from study_session where id = ?", Timestamp.class, UUID.fromString(sessionId));

        sessionChat.sendDueReminders(startsAt.minus(Duration.ofMinutes(30)));

        assertThat(jdbc.queryForObject("select updated_at from study_session where id = ?", Timestamp.class, UUID.fromString(sessionId))).isEqualTo(before);
    }

    @Test
    void anotherBotsChatGetsNoPostsOrReminders() throws Exception {
        linkChat();
        jdbc.update("update group_chat set bot_id = 999 where chat_id = ?", chatId);
        Instant startsAt = inHours(3);
        schedule(startsAt);

        sessionChat.sendDueReminders(startsAt.minus(Duration.ofMinutes(30)));

        verify(client, after(300).never()).sendMessage(eq(chatId), contains("New study session"), any(ReplyMarkup.class));
        verifyReminders(0);
    }

    @Test
    void aChatTheBotWasRemovedFromGetsNoReminder() throws Exception {
        linkChat();
        Instant startsAt = inHours(3);
        schedule(startsAt);
        router.route(new Update(1, null, new Update.ChatMemberUpdated(new Update.Chat(chatId, "supergroup"), person(leaderTelegramId), null,
                new ChatMember(null, "left", false, false, false, false, false), null), null, null));

        sessionChat.sendDueReminders(startsAt.minus(Duration.ofMinutes(30)));

        verifyReminders(0);
    }

    @Test
    void closingTheGroupStopsReminders() throws Exception {
        linkChat();
        Instant startsAt = inHours(3);
        schedule(startsAt);
        mockMvc.perform(as(leaderToken, post(path("/close")))).andExpect(status().isOk());

        sessionChat.sendDueReminders(startsAt.minus(Duration.ofMinutes(30)));

        verifyReminders(0);
        verify(client, never()).sendMessage(eq(chatId), contains("cancelled: Mock exam"), any(ReplyMarkup.class));
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

        verify(client, timeout(TIMEOUT_MS)).sendMessage(eq(chatId), contains("Join: https://meet.example.com/abc"), any(ReplyMarkup.class));
    }

    @Test
    void aVideoChatReminderTagsThoseGoing() throws Exception {
        linkChat();
        Instant startsAt = inHours(3);
        String sessionId = scheduleVideoChat(startsAt);
        long memberTelegramId = joinChatAndAnswerGoing(sessionId);

        sessionChat.sendDueReminders(startsAt.minus(Duration.ofMinutes(30)));

        ArgumentCaptor<List<MessageEntity>> entities = ArgumentCaptor.captor();
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(client).sendMessage(eq(chatId), text.capture(), entities.capture(), any(ReplyMarkup.ActionButtons.class));
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
        verify(client).sendMessage(eq(chatId), contains("video chat for Mock exam has started"), entities.capture(), any(ReplyMarkup.class));
        assertThat(entities.getValue()).extracting(entity -> entity.user().id()).containsExactly(memberTelegramId);
    }

    @Test
    void aCallLongBeforeTheSessionTagsNobody() throws Exception {
        linkChat();
        String sessionId = scheduleVideoChat(inHours(3));
        joinChatAndAnswerGoing(sessionId);

        callStartedBy(leaderTelegramId);

        verify(client, never()).sendMessage(eq(chatId), contains("has started"), any(ReplyMarkup.class));
        verify(client, never()).sendMessage(eq(chatId), contains("has started"), any(List.class), any(ReplyMarkup.class));
    }

    @Test
    void aCallDuringASessionWithAMeetingLinkTagsNobody() throws Exception {
        linkChat();
        schedule(Instant.now().plus(Duration.ofMinutes(20)));

        callStartedBy(leaderTelegramId);

        verify(client, never()).sendMessage(eq(chatId), contains("has started"), any(ReplyMarkup.class));
    }

    @Test
    void someoneNotGoingIsNotTagged() throws Exception {
        linkChat();
        Instant startsAt = inHours(3);
        String sessionId = scheduleVideoChat(startsAt);
        long memberTelegramId = joinChatAndAnswerGoing(sessionId);
        press(memberTelegramId, "rsvp:" + sessionId + ":N");

        sessionChat.sendDueReminders(startsAt.minus(Duration.ofMinutes(30)));

        verify(client, never()).sendMessage(eq(chatId), anyString(), any(List.class), any(ReplyMarkup.class));
        verifyReminders(1);
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
