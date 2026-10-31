package is442t1.studybuddy.telegram.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import is442t1.studybuddy.telegram.api.ReplyMarkup;
import is442t1.studybuddy.telegram.api.TelegramApiException;
import is442t1.studybuddy.telegram.api.TelegramClient;
import is442t1.studybuddy.telegram.api.Update;

/** Telegram on, with a fake client: real router, services and database, from setup to removal. */
@TestPropertySource(properties = {
        "studybuddy.telegram.enabled=true",
        "studybuddy.telegram.bot-token=123456:TEST",
        "studybuddy.telegram.bot-username=study_test_bot",
        "studybuddy.telegram.polling=false"
})
class GroupChatApiTest extends ApiTestSupport {

    private static final long BOT_ID = 123456;
    private static final AtomicLong NEXT_ID = new AtomicLong(5000);
    private static final long TIMEOUT_MS = 3000;
    private static final String SETUP_PREFIX = "https://t.me/study_test_bot?start=";
    private static final String COULD_NOT_READ = "I couldn't read that group from Telegram. Your setup link still works: tap Use an existing group and choose the same group.";
    private static final String LINKED = "Linked ✓ You can go back to StudyBuddy.";

    @MockitoBean private TelegramClient client;
    @Autowired private TelegramUpdateRouter router;
    @Autowired private JdbcTemplate jdbc;

    private String leaderToken;
    private String memberToken;
    private String memberEmail;
    private String strangerToken;
    private String groupId;
    private long leaderTelegramId;
    private long chatId;

    private static ChatMember admin() {
        return new ChatMember(null, "administrator", true, true, true, true, true);
    }

    private static ChatMember plainMember() {
        return new ChatMember(null, "member", true, false, false, false, false);
    }

    private static ChatMember owner() {
        return new ChatMember(null, "creator", true, false, false, false, false);
    }

    @BeforeEach
    void createGroupWithAMember() throws Exception {
        reset(client);
        leaderTelegramId = NEXT_ID.incrementAndGet();
        chatId = -NEXT_ID.incrementAndGet();
        leaderToken = login(createStudent("Chat Leader", "IS442"));
        memberEmail = createStudent("Chat Member", "IS442");
        memberToken = login(memberEmail);
        strangerToken = login(createStudent("Chat Stranger", "IS442"));
        groupId = read(mockMvc.perform(as(leaderToken, post("/api/groups")).content("""
                        {"courseCode":"IS442","name":"Exam Crew","description":"Weekly practice",
                         "goals":["EXAM_PREPARATION"],"meetingMode":"ONLINE","maxSize":4,
                         "availability":[{"day":"TUE","start":"19:00","end":"21:00"}]}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
        acceptIntoGroup(memberToken);
        when(client.getChat(anyLong())).thenAnswer(call -> new ChatInfo(call.getArgument(0), "supergroup", "Any name"));
        when(client.getChatMember(anyLong(), eq(BOT_ID))).thenReturn(admin());
        when(client.getChatMember(anyLong(), eq(leaderTelegramId))).thenReturn(owner());
        when(client.createJoinRequestLink(anyLong(), anyString(), any())).thenAnswer(call ->
                new ChatInviteLink("https://t.me/+" + call.getArgument(1), call.getArgument(1)));
    }

    private void acceptIntoGroup(String token) throws Exception {
        mockMvc.perform(as(token, post(path("/join-requests"))).content("{}")).andExpect(status().isCreated());
        String pending = read(mockMvc.perform(as(leaderToken, get(path("/join-requests"))))
                .andReturn().getResponse().getContentAsString(), "$[0].id");
        mockMvc.perform(as(leaderToken, post(path("/join-requests/" + pending + "/accept")))).andExpect(status().isOk());
    }

    private String path(String suffix) {
        return "/api/groups/" + groupId + suffix;
    }

    private String setupPayload() throws Exception {
        String url = read(mockMvc.perform(as(leaderToken, post(path("/chat/setup"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.url");
        assertThat(url).startsWith(SETUP_PREFIX + "g_");
        return url.substring(SETUP_PREFIX.length());
    }

    private static Update message(long userId, String firstName, String text) {
        return new Update(1, new Update.Message(2, new Update.Sender(userId, false, firstName, null), new Update.Chat(userId, "private"), text));
    }

    /** Opens the setup link and returns the id on the first button the bot sent. */
    private int openSetup(long userId, String payload) {
        router.route(message(userId, "Leah", "/start " + payload));
        return sentRequestId(userId);
    }

    private int sentRequestId(long userId) {
        ArgumentCaptor<ReplyMarkup> markup = ArgumentCaptor.forClass(ReplyMarkup.class);
        verify(client, atLeastOnce()).sendMessage(eq(userId), anyString(), markup.capture());
        ReplyMarkup.Keyboard keyboard = (ReplyMarkup.Keyboard) markup.getValue();
        assertThat(keyboard.isPersistent()).isTrue();
        assertThat(keyboard.oneTimeKeyboard()).isFalse();
        return keyboard.keyboard().getFirst().getFirst().requestChat().requestId();
    }

    private void choose(long userId, int requestId, long chosenChat) {
        router.route(new Update(1, new Update.Message(3, new Update.Sender(userId, false, "Leah", null),
                new Update.Chat(userId, "private"), null, new Update.ChatShared(chosenChat, requestId), null, null)));
    }

    private void linkChat() throws Exception {
        choose(leaderTelegramId, openSetup(leaderTelegramId, setupPayload()), chatId);
    }

    private String inviteUrl(String token) throws Exception {
        return read(mockMvc.perform(as(token, post(path("/chat/invite"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.me.inviteUrl");
    }

    private void askToJoin(long userId, String url) {
        String name = url == null ? null : url.substring("https://t.me/+".length());
        router.route(new Update(1, null, null, null, new Update.ChatJoinRequest(new Update.Chat(chatId, "supergroup"),
                new Update.Sender(userId, false, "Mia", "mia_m"), userId, new ChatInviteLink(url, name))));
    }

    private void expectChatStatus(String token, String expected) throws Exception {
        mockMvc.perform(as(token, get(path("/chat")))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value(expected));
    }

    private void expectMyState(String token, String expected) throws Exception {
        mockMvc.perform(as(token, get(path("/chat")))).andExpect(jsonPath("$.me.state").value(expected));
    }

    @Test
    void aGroupStartsWithNoChat() throws Exception {
        expectChatStatus(leaderToken, "NOT_LINKED");
        expectChatStatus(memberToken, "NOT_LINKED");
    }

    @Test
    void onlyMembersCanSeeTheChatAndOnlyTheLeaderCanSetItUp() throws Exception {
        mockMvc.perform(as(strangerToken, get(path("/chat")))).andExpect(status().isForbidden());
        mockMvc.perform(as(strangerToken, post(path("/chat/invite")))).andExpect(status().isForbidden());
        mockMvc.perform(as(memberToken, post(path("/chat/setup")))).andExpect(status().isForbidden());
        mockMvc.perform(as(memberToken, delete(path("/chat")))).andExpect(status().isForbidden());
    }

    @Test
    void linkingAChatSetsItUpAndLocksDownMemberInvites() throws Exception {
        linkChat();

        verify(client).setMemberPermissions(chatId, true);
        verify(client).setTitle(chatId, "Exam Crew · IS442");
        verify(client).setDescription(chatId, "Weekly practice");
        verify(client).pinMessage(eq(chatId), anyLong());
        verify(client).sendMessage(eq(leaderTelegramId), eq(LINKED), any(ReplyMarkup.RemoveKeyboard.class));
        mockMvc.perform(as(leaderToken, get(path("/chat"))))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.me.state").value("JOINED"))
                .andExpect(jsonPath("$.ownedByFormerLeader").value(false))
                .andExpect(jsonPath("$.members.length()").value(2));
        mockMvc.perform(as(memberToken, get(path("/chat"))))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.me.state").value("NOT_INVITED"))
                .andExpect(jsonPath("$.members").doesNotExist())
                .andExpect(jsonPath("$.missingRights").doesNotExist());
    }

    @Test
    void aNewGroupChosenByItsOldIdIsLinkedUnderItsUpgradedId() throws Exception {
        long upgraded = -NEXT_ID.incrementAndGet();
        int requestId = openSetup(leaderTelegramId, setupPayload());
        when(client.getChatMember(chatId, leaderTelegramId))
                .thenThrow(new TelegramApiException(400, "getChatMember failed: Bad Request: group chat was upgraded to a supergroup chat", null, upgraded));
        when(client.getChatMember(upgraded, leaderTelegramId)).thenReturn(owner());

        choose(leaderTelegramId, requestId, chatId);

        expectChatStatus(leaderToken, "ACTIVE");
        verify(client).setMemberPermissions(upgraded, true);
        verify(client).sendMessage(eq(leaderTelegramId), eq(LINKED), any(ReplyMarkup.RemoveKeyboard.class));
        assertThat(jdbc.queryForObject("select count(*) from group_chat where chat_id = ?", Long.class, upgraded)).isEqualTo(1);
    }

    @Test
    void aLastingFailureTellsTheLeaderAndLeavesTheSetupLinkUsable() throws Exception {
        int requestId = openSetup(leaderTelegramId, setupPayload());
        when(client.getChatMember(chatId, leaderTelegramId))
                .thenThrow(new TelegramApiException(400, "getChatMember failed: Bad Request: chat not found", null, null));

        choose(leaderTelegramId, requestId, chatId);

        verify(client).sendMessage(leaderTelegramId, COULD_NOT_READ);
        expectChatStatus(leaderToken, "NOT_LINKED");

        doReturn(owner()).when(client).getChatMember(chatId, leaderTelegramId);
        choose(leaderTelegramId, requestId + 1, chatId);

        expectChatStatus(leaderToken, "ACTIVE");
    }

    @Test
    void theButtonsCanBeResentByOpeningTheLinkAgain() throws Exception {
        String payload = setupPayload();
        int first = openSetup(leaderTelegramId, payload);

        assertThat(openSetup(leaderTelegramId, payload)).isEqualTo(first);
    }

    @Test
    void onlyTheFirstPersonToOpenASetupLinkCanFinishIt() throws Exception {
        String payload = setupPayload();
        int requestId = openSetup(leaderTelegramId, payload);
        long stranger = NEXT_ID.incrementAndGet();
        when(client.getChatMember(anyLong(), eq(stranger))).thenReturn(owner());

        router.route(message(stranger, "Eve", "/start " + payload));
        choose(stranger, requestId, chatId);

        verify(client, never()).setMemberPermissions(anyLong(), anyBoolean());
        expectChatStatus(leaderToken, "NOT_LINKED");
    }

    @Test
    void someoneWhoIsNotAnAdminOfTheChatCannotLinkIt() throws Exception {
        int requestId = openSetup(leaderTelegramId, setupPayload());
        when(client.getChatMember(chatId, leaderTelegramId)).thenReturn(plainMember());

        choose(leaderTelegramId, requestId, chatId);

        expectChatStatus(leaderToken, "NOT_LINKED");
    }

    @Test
    void anExpiredSetupLinkIsRefused() throws Exception {
        String payload = setupPayload();
        jdbc.update("update telegram_link_token set expires_at = timestamp '2020-01-01 00:00:00' where student_id = ? and purpose = 'GROUP_SETUP'", idOf(leaderEmailFromGroup()));

        router.route(message(leaderTelegramId, "Leah", "/start " + payload));

        verify(client).sendMessage(leaderTelegramId, "This setup link has expired or belongs to someone else. Open your group in StudyBuddy and start again.");
    }

    private String leaderEmailFromGroup() {
        return jdbc.queryForObject("select email from users where id = (select leader_id from study_group where id = ?)", String.class,
                UUID.fromString(groupId));
    }

    @Test
    void aChatCannotServeTwoStudyGroups() throws Exception {
        linkChat();
        String otherLeaderToken = login(createStudent("Other Leader", "IS442"));
        String otherGroup = read(mockMvc.perform(as(otherLeaderToken, post("/api/groups")).content("""
                        {"courseCode":"IS442","name":"Other Crew","description":"x",
                         "goals":["EXAM_PREPARATION"],"meetingMode":"ONLINE","maxSize":4,
                         "availability":[{"day":"TUE","start":"19:00","end":"21:00"}]}"""))
                .andReturn().getResponse().getContentAsString(), "$.id");
        String url = read(mockMvc.perform(as(otherLeaderToken, post("/api/groups/" + otherGroup + "/chat/setup")))
                .andReturn().getResponse().getContentAsString(), "$.url");
        long other = NEXT_ID.incrementAndGet();
        when(client.getChatMember(anyLong(), eq(other))).thenReturn(owner());

        choose(other, openSetup(other, url.substring(SETUP_PREFIX.length())), chatId);

        mockMvc.perform(as(otherLeaderToken, get("/api/groups/" + otherGroup + "/chat"))).andExpect(jsonPath("$.status").value("NOT_LINKED"));
    }

    @Test
    void anExistingGroupWaitsForAdminRightsThenFinishesSetup() throws Exception {
        when(client.getChatMember(chatId, BOT_ID)).thenReturn(plainMember());

        choose(leaderTelegramId, openSetup(leaderTelegramId, setupPayload()) + 1, chatId);

        expectChatStatus(leaderToken, "NEEDS_ATTENTION");
        verify(client, never()).setMemberPermissions(anyLong(), anyBoolean());
        mockMvc.perform(as(memberToken, post(path("/chat/invite")))).andExpect(status().isConflict());

        router.route(new Update(1, null, new Update.ChatMemberUpdated(new Update.Chat(chatId, "supergroup"), null, plainMember(), admin(), null), null, null));

        expectChatStatus(leaderToken, "ACTIVE");
        verify(client).setMemberPermissions(chatId, true);
    }

    @Test
    void anUpgradedGroupKeepsWorkingUnderItsNewId() throws Exception {
        linkChat();
        long newId = -NEXT_ID.incrementAndGet();

        router.route(new Update(1, new Update.Message(9, new Update.Sender(leaderTelegramId, false, "Leah", null),
                new Update.Chat(chatId, "group"), null, null, newId, null)));
        inviteUrl(memberToken);

        verify(client).createJoinRequestLink(eq(newId), anyString(), any());
    }

    @Test
    void anUpgradeFoundThroughAnApiErrorIsSavedAndRetried() throws Exception {
        linkChat();
        long newId = -NEXT_ID.incrementAndGet();
        when(client.createJoinRequestLink(eq(chatId), anyString(), any()))
                .thenThrow(new TelegramApiException(400, "upgraded", null, newId));

        assertThat(inviteUrl(memberToken)).isNotNull();

        verify(client).createJoinRequestLink(eq(newId), anyString(), any());
    }

    @Test
    void aMemberGetsOneInviteAndKeepsGettingTheSameOne() throws Exception {
        linkChat();

        String first = inviteUrl(memberToken);

        assertThat(inviteUrl(memberToken)).isEqualTo(first);
        verify(client).createJoinRequestLink(eq(chatId), anyString(), any());
    }

    @Test
    void aMemberIsApprovedThroughTheirInvite() throws Exception {
        linkChat();
        String url = inviteUrl(memberToken);
        long memberTelegramId = NEXT_ID.incrementAndGet();

        askToJoin(memberTelegramId, url);

        verify(client).approveJoinRequest(chatId, memberTelegramId);
        verify(client).revokeInviteLink(chatId, url);
        verify(client).sendMessage(memberTelegramId, "You're in! Say hi to your study group.");
        mockMvc.perform(as(memberToken, get(path("/chat"))))
                .andExpect(jsonPath("$.me.state").value("JOINED"))
                .andExpect(jsonPath("$.me.telegramName").value("@mia_m"))
                .andExpect(jsonPath("$.me.inviteUrl").doesNotExist());
    }

    @Test
    void joiningNeverConnectsTheStudentsTelegramAccount() throws Exception {
        linkChat();

        askToJoin(NEXT_ID.incrementAndGet(), inviteUrl(memberToken));

        assertThat(jdbc.queryForObject("select count(*) from telegram_account where student_id = ?", Long.class, idOf(memberEmail))).isZero();
    }

    @Test
    void aReplacedInviteIsDeclined() throws Exception {
        linkChat();
        String old = inviteUrl(memberToken);
        long asker = NEXT_ID.incrementAndGet();
        mockMvc.perform(as(memberToken, post(path("/chat/invite"))).content("{\"renew\":true}")).andExpect(status().isOk());

        askToJoin(asker, old);

        verify(client).revokeInviteLink(chatId, old);
        verify(client).declineJoinRequest(chatId, asker);
        verify(client, never()).approveJoinRequest(chatId, asker);
    }

    @Test
    void anInviteForwardedToSomeoneElseIsDeclinedForAConnectedStudent() throws Exception {
        linkChat();
        long realAccount = NEXT_ID.incrementAndGet();
        long forwardedTo = NEXT_ID.incrementAndGet();
        when(client.getChatMember(chatId, realAccount)).thenReturn(new ChatMember(null, "left", false, false, false, false, false));
        connectTelegram(memberToken, realAccount);
        String url = inviteUrl(memberToken);

        askToJoin(forwardedTo, url);

        verify(client).declineJoinRequest(chatId, forwardedTo);
    }

    private void connectTelegram(String token, long telegramId) throws Exception {
        String link = read(mockMvc.perform(as(token, post("/api/students/me/telegram/link")))
                .andReturn().getResponse().getContentAsString(), "$.url");
        router.route(message(telegramId, "Mia", "/start " + link.substring(SETUP_PREFIX.length())));
    }

    @Test
    void someoneAlreadyInTheChatAsAnotherStudentIsDeclined() throws Exception {
        linkChat();
        String secondToken = login(createStudent("Second Member", "IS442"));
        acceptIntoGroup(secondToken);
        long sameTelegram = NEXT_ID.incrementAndGet();
        askToJoin(sameTelegram, inviteUrl(memberToken));

        askToJoin(sameTelegram, inviteUrl(secondToken));

        verify(client).declineJoinRequest(chatId, sameTelegram);
    }

    @Test
    void aLinkFromAHumanAdminIsLeftAlone() throws Exception {
        linkChat();
        long asker = NEXT_ID.incrementAndGet();

        askToJoin(asker, "https://t.me/+someone_elses");

        verify(client, never()).declineJoinRequest(anyLong(), anyLong());
        verify(client, never()).approveJoinRequest(anyLong(), anyLong());
    }

    @Test
    void aConnectedStudentAlreadyInTheChatShowsAsJoinedWithoutAnInvite() throws Exception {
        linkChat();
        long telegramId = NEXT_ID.incrementAndGet();
        when(client.getChatMember(chatId, telegramId)).thenReturn(plainMember());
        connectTelegram(memberToken, telegramId);

        mockMvc.perform(as(memberToken, post(path("/chat/invite"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.me.state").value("JOINED"));

        verify(client, never()).createJoinRequestLink(anyLong(), anyString(), any());
    }

    @Test
    void notYouRemovesWhoeverJoinedAndIssuesANewInvite() throws Exception {
        linkChat();
        long wrongPerson = NEXT_ID.incrementAndGet();
        askToJoin(wrongPerson, inviteUrl(memberToken));

        mockMvc.perform(as(memberToken, post(path("/chat/invite"))).content("{\"renew\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.me.state").value("INVITED"));

        verify(client).banMember(chatId, wrongPerson);
        verify(client).unbanMember(chatId, wrongPerson);
    }

    @Test
    void leavingTheGroupRemovesTheStudentFromTheChat() throws Exception {
        linkChat();
        long telegramId = NEXT_ID.incrementAndGet();
        askToJoin(telegramId, inviteUrl(memberToken));

        mockMvc.perform(as(memberToken, post(path("/leave")))).andExpect(status().isNoContent());

        verify(client, timeout(TIMEOUT_MS)).banMember(chatId, telegramId);
        verify(client, timeout(TIMEOUT_MS)).unbanMember(chatId, telegramId);
    }

    @Test
    void aBasicGroupKickIsOnlyABan() throws Exception {
        when(client.getChat(chatId)).thenReturn(new ChatInfo(chatId, "group", "Basic"));
        linkChat();
        long telegramId = NEXT_ID.incrementAndGet();
        askToJoin(telegramId, inviteUrl(memberToken));

        mockMvc.perform(as(memberToken, post(path("/leave")))).andExpect(status().isNoContent());

        verify(client, timeout(TIMEOUT_MS)).banMember(chatId, telegramId);
        verify(client, after(300).never()).unbanMember(chatId, telegramId);
    }

    @Test
    void aFailedRemovalIsShownToTheLeader() throws Exception {
        linkChat();
        long telegramId = NEXT_ID.incrementAndGet();
        askToJoin(telegramId, inviteUrl(memberToken));
        doThrow(new TelegramApiException(400, "banChatMember failed: Bad Request: can't remove chat owner", null, null))
                .when(client).banMember(chatId, telegramId);

        mockMvc.perform(as(leaderToken, delete(path("/members/" + idOf(memberEmail))))).andExpect(status().isNoContent());

        verify(client, timeout(TIMEOUT_MS)).banMember(chatId, telegramId);
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        String state = "";
        while (System.currentTimeMillis() < deadline && !"REMOVAL_FAILED".equals(state)) {
            state = jdbc.queryForObject("select state from chat_membership where telegram_user_id = ?", String.class, telegramId);
        }
        assertThat(state).isEqualTo("REMOVAL_FAILED");
    }

    @Test
    void aTelegramOutageDoesNotStopSomeoneLeavingTheGroup() throws Exception {
        linkChat();
        long telegramId = NEXT_ID.incrementAndGet();
        askToJoin(telegramId, inviteUrl(memberToken));
        doThrow(new TelegramApiException(TelegramApiException.NO_REPLY, "down", null, null)).when(client).banMember(anyLong(), anyLong());

        mockMvc.perform(as(memberToken, post(path("/leave")))).andExpect(status().isNoContent());
    }

    @Test
    void aFailedInviteReportsTelegramIsUnavailable() throws Exception {
        linkChat();
        when(client.createJoinRequestLink(anyLong(), anyString(), any()))
                .thenThrow(new TelegramApiException(TelegramApiException.NO_REPLY, "down", null, null));

        mockMvc.perform(as(memberToken, post(path("/chat/invite")))).andExpect(status().isServiceUnavailable());
    }

    @Test
    void aNewMemberWhoConnectedTelegramIsMessagedTheirInvite() throws Exception {
        linkChat();
        String newToken = login(createStudent("Fresh Member", "IS442"));
        long telegramId = NEXT_ID.incrementAndGet();
        connectTelegram(newToken, telegramId);

        acceptIntoGroup(newToken);

        verify(client, timeout(TIMEOUT_MS)).sendMessage(eq(telegramId), anyString(),
                argThat(markup -> markup instanceof ReplyMarkup.LinkButton));
    }

    @Test
    void closingTheGroupMakesTheChatReadOnly() throws Exception {
        linkChat();
        String url = inviteUrl(memberToken);

        mockMvc.perform(as(leaderToken, post(path("/close")))).andExpect(status().isOk());

        verify(client, timeout(TIMEOUT_MS)).revokeInviteLink(chatId, url);
        verify(client, timeout(TIMEOUT_MS)).setMemberPermissions(chatId, false);
    }

    @Test
    void renamingTheGroupRenamesTheChat() throws Exception {
        linkChat();

        mockMvc.perform(as(leaderToken, put(path(""))).content("""
                {"courseCode":"IS442","name":"New Name","description":"Weekly practice",
                 "goals":["EXAM_PREPARATION"],"meetingMode":"ONLINE","maxSize":4,
                 "availability":[{"day":"TUE","start":"19:00","end":"21:00"}]}""")).andExpect(status().isOk());

        verify(client, timeout(TIMEOUT_MS)).setTitle(chatId, "New Name · IS442");
    }

    @Test
    void aTransferPostsANoticeAndWarnsTheNewLeaderAboutOwnership() throws Exception {
        linkChat();

        mockMvc.perform(as(leaderToken, put(path("/leader"))).content("{\"studentId\":\"" + idOf(memberEmail) + "\"}"))
                .andExpect(status().isOk());

        verify(client, timeout(TIMEOUT_MS)).sendMessage(eq(chatId), contains("is now the leader"), any());
        mockMvc.perform(as(memberToken, get(path("/chat")))).andExpect(jsonPath("$.ownedByFormerLeader").value(true));
    }

    @Test
    void removingTheBotDetachesTheChatAndALeaderCanLinkAgain() throws Exception {
        linkChat();
        router.route(new Update(1, null, new Update.ChatMemberUpdated(new Update.Chat(chatId, "supergroup"), null, admin(),
                new ChatMember(null, "kicked", false, false, false, false, false), null), null, null));

        expectChatStatus(leaderToken, "DETACHED");

        long newChat = -NEXT_ID.incrementAndGet();
        when(client.getChatMember(newChat, leaderTelegramId)).thenReturn(owner());
        choose(leaderTelegramId, openSetup(leaderTelegramId, setupPayload()), newChat);

        expectChatStatus(leaderToken, "ACTIVE");
    }

    @Test
    void leavingInTelegramShowsAsLeftAndComingBackAsJoined() throws Exception {
        linkChat();
        long telegramId = NEXT_ID.incrementAndGet();
        askToJoin(telegramId, inviteUrl(memberToken));
        Update.Sender person = new Update.Sender(telegramId, false, "Mia", null);

        router.route(new Update(1, null, null, new Update.ChatMemberUpdated(new Update.Chat(chatId, "supergroup"), person, null,
                new ChatMember(person, "left", false, false, false, false, false), null), null));
        expectMyState(memberToken, "LEFT");

        router.route(new Update(1, null, null, new Update.ChatMemberUpdated(new Update.Chat(chatId, "supergroup"), person, null,
                new ChatMember(person, "member", true, false, false, false, false), null), null));
        expectMyState(memberToken, "JOINED");
    }

    @Test
    void aLeaderCanUnlinkTheChat() throws Exception {
        linkChat();
        String url = inviteUrl(memberToken);

        mockMvc.perform(as(leaderToken, delete(path("/chat")))).andExpect(status().isNoContent());

        verify(client).revokeInviteLink(chatId, url);
        verify(client).leaveChat(chatId);
        expectChatStatus(leaderToken, "NOT_LINKED");
    }

    @Test
    void verifyPicksUpRightsTheLeaderHasJustFixed() throws Exception {
        when(client.getChatMember(chatId, BOT_ID)).thenReturn(plainMember());
        choose(leaderTelegramId, openSetup(leaderTelegramId, setupPayload()) + 1, chatId);
        mockMvc.perform(as(leaderToken, get(path("/chat"))))
                .andExpect(jsonPath("$.missingRights.length()").value(4));

        when(client.getChatMember(chatId, BOT_ID)).thenReturn(admin());
        mockMvc.perform(as(leaderToken, post(path("/chat/verify"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void anotherBotsChatIsShownAsSuchAndCannotBeUsed() throws Exception {
        linkChat();
        jdbc.update("update group_chat set bot_id = 999 where chat_id = ?", chatId);

        expectChatStatus(memberToken, "MANAGED_BY_OTHER_BOT");
        mockMvc.perform(as(memberToken, post(path("/chat/invite")))).andExpect(status().isConflict());
        mockMvc.perform(as(leaderToken, post(path("/chat/setup")))).andExpect(status().isConflict());
    }

    @Test
    void deletingAnAccountRemovesThemFromTheChat() throws Exception {
        linkChat();
        long telegramId = NEXT_ID.incrementAndGet();
        askToJoin(telegramId, inviteUrl(memberToken));

        mockMvc.perform(as(login(createAdministrator()), delete("/api/admin/users/" + idOf(memberEmail)))).andExpect(status().isNoContent());

        verify(client, timeout(TIMEOUT_MS)).banMember(chatId, telegramId);
    }

    @Test
    void aSuspendedMemberCannotGetAnInvite() throws Exception {
        linkChat();
        jdbc.update("update users set status = 'SUSPENDED' where id = ?", idOf(memberEmail));

        mockMvc.perform(as(memberToken, post(path("/chat/invite")))).andExpect(status().is4xxClientError());
    }
}
