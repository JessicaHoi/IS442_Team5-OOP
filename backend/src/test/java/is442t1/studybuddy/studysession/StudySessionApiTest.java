package is442t1.studybuddy.studysession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import is442t1.studybuddy.ApiTestSupport;

class StudySessionApiTest extends ApiTestSupport {

    private static final String COURSE = "SESSION101";

    @Autowired private JdbcTemplate jdbc;

    private String leaderToken;
    private String memberToken;
    private String strangerToken;
    private UUID memberId;
    private UUID groupId;

    @BeforeEach
    void createGroupWithAMember() throws Exception {
        leaderToken = login(createStudent("Session Leader", COURSE));
        String memberEmail = createStudent("Session Member", COURSE);
        memberId = idOf(memberEmail);
        memberToken = login(memberEmail);
        strangerToken = login(createStudent("Session Stranger", COURSE));
        groupId = createGroup(leaderToken);
        addMember(groupId, leaderToken, memberToken);
    }

    @Test
    void theLeaderSchedulesAndMembersSeeAndAnswer() throws Exception {
        UUID sessionId = schedule(leaderToken);

        sessions(memberToken, "upcoming").andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(sessionId.toString()))
                .andExpect(jsonPath("$[0].groupId").value(groupId.toString()))
                .andExpect(jsonPath("$[0].title").value("Paper drill"))
                .andExpect(jsonPath("$[0].status").value("SCHEDULED"))
                .andExpect(jsonPath("$[0].meetingMode").value("IN_PERSON"))
                .andExpect(jsonPath("$[0].myRsvp").doesNotExist())
                .andExpect(jsonPath("$[0].goingCount").value(0));

        rsvp(memberToken, sessionId, "GOING").andExpect(status().isOk())
                .andExpect(jsonPath("$.myRsvp").value("GOING"))
                .andExpect(jsonPath("$.goingCount").value(1))
                .andExpect(jsonPath("$.attendees[0].name").value("Session Member"))
                .andExpect(jsonPath("$.attendees[0].response").value("GOING"));
        rsvp(memberToken, sessionId, "NOT_GOING").andExpect(status().isOk())
                .andExpect(jsonPath("$.goingCount").value(0))
                .andExpect(jsonPath("$.notGoingCount").value(1));
    }

    @Test
    void onlyTheLeaderCanManageSessions() throws Exception {
        UUID sessionId = schedule(leaderToken);
        String invalid = "{\"title\":\"\"}";

        mockMvc.perform(as(memberToken, post(sessionsUrl())).content(invalid)).andExpect(status().isForbidden());
        mockMvc.perform(as(strangerToken, post(sessionsUrl())).content(invalid)).andExpect(status().isForbidden());
        mockMvc.perform(as(memberToken, put(sessionsUrl() + "/" + sessionId)).content(invalid)).andExpect(status().isForbidden());
        mockMvc.perform(as(memberToken, post(sessionsUrl() + "/" + sessionId + "/cancel"))).andExpect(status().isForbidden());
    }

    @Test
    void nonMembersCannotSeeOrAnswerSessions() throws Exception {
        UUID sessionId = schedule(leaderToken);

        sessions(strangerToken, "upcoming").andExpect(status().isForbidden());
        rsvp(strangerToken, sessionId, "GOING").andExpect(status().isForbidden());
        mockMvc.perform(as(strangerToken, get(sessionsUrl() + "/" + sessionId + "/calendar.ics"))).andExpect(status().isForbidden());
    }

    @Test
    void refusesAnInvalidScopeAndMissingOrInvalidAnswers() throws Exception {
        UUID sessionId = schedule(leaderToken);

        sessions(memberToken, "later").andExpect(status().isBadRequest());
        mockMvc.perform(as(memberToken, put(sessionsUrl() + "/" + sessionId + "/rsvp")).content("{}")).andExpect(status().isBadRequest());
        rsvp(memberToken, sessionId, "MAYBE").andExpect(status().isBadRequest());
    }

    @Test
    void aSessionFromAnotherGroupIsNotFound() throws Exception {
        UUID sessionId = schedule(leaderToken);
        UUID otherGroup = createGroup(strangerToken);

        mockMvc.perform(as(strangerToken, post("/api/groups/" + otherGroup + "/sessions/" + sessionId + "/cancel")))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancellingKeepsTheSessionVisibleAndStopsAnswers() throws Exception {
        UUID sessionId = schedule(leaderToken);

        mockMvc.perform(as(leaderToken, post(sessionsUrl() + "/" + sessionId + "/cancel"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        sessions(memberToken, "upcoming").andExpect(jsonPath("$[0].status").value("CANCELLED"));
        rsvp(memberToken, sessionId, "GOING").andExpect(status().isConflict());
        mockMvc.perform(as(memberToken, get("/api/sessions/upcoming"))).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void theLeaderChangesASessionAndTheCalendarFileShowsANewSequence() throws Exception {
        UUID sessionId = schedule(leaderToken);
        String calendarUrl = sessionsUrl() + "/" + sessionId + "/calendar.ics";

        mockMvc.perform(as(memberToken, get(calendarUrl))).andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/calendar;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"study-session.ics\""))
                .andExpect(content().string(containsString("UID:" + sessionId + "@studybuddy\r\n")))
                .andExpect(content().string(containsString("SEQUENCE:0\r\n")));

        mockMvc.perform(as(leaderToken, put(sessionsUrl() + "/" + sessionId)).content(body("New title"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New title"));

        mockMvc.perform(as(memberToken, get(calendarUrl)))
                .andExpect(content().string(containsString("SEQUENCE:1\r\n")))
                .andExpect(content().string(containsString("SUMMARY:New title\r\n")));
    }

    @Test
    void upcomingListsTheCallersSessionsInAllTheirGroupsSoonestFirst() throws Exception {
        UUID later = schedule(leaderToken, Instant.now().plus(Duration.ofDays(5)));
        String otherLeader = login(createStudent("Other Leader", COURSE));
        UUID otherGroup = createGroup(otherLeader);
        addMember(otherGroup, otherLeader, memberToken);
        String sooner = read(mockMvc.perform(as(otherLeader, post("/api/groups/" + otherGroup + "/sessions"))
                .content(body("Sooner", Instant.now().plus(Duration.ofDays(1))))).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(as(memberToken, get("/api/sessions/upcoming"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(sooner))
                .andExpect(jsonPath("$[1].id").value(later.toString()));
        mockMvc.perform(as(strangerToken, get("/api/sessions/upcoming"))).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void aFinishedSessionMovesToPast() throws Exception {
        UUID sessionId = schedule(leaderToken);
        jdbc.update("update study_session set starts_at = ?, ends_at = ? where id = ?",
                Timestamp.from(Instant.now().minus(Duration.ofHours(3))),
                Timestamp.from(Instant.now().minus(Duration.ofHours(1))), sessionId);

        sessions(memberToken, "upcoming").andExpect(jsonPath("$.length()").value(0));
        sessions(memberToken, "past").andExpect(jsonPath("$[0].id").value(sessionId.toString()));
        rsvp(memberToken, sessionId, "GOING").andExpect(status().isConflict());
    }

    @Test
    void closingTheGroupCancelsUpcomingSessions() throws Exception {
        UUID sessionId = schedule(leaderToken);

        mockMvc.perform(as(leaderToken, post("/api/groups/" + groupId + "/close"))).andExpect(status().isOk());

        sessions(memberToken, "upcoming").andExpect(jsonPath("$[0].id").value(sessionId.toString()))
                .andExpect(jsonPath("$[0].status").value("CANCELLED"));
        mockMvc.perform(as(leaderToken, post(sessionsUrl())).content(body("Too late"))).andExpect(status().isConflict());
    }

    @Test
    void leavingOrBeingRemovedDeletesAnswersToUpcomingSessions() throws Exception {
        UUID sessionId = schedule(leaderToken);
        rsvp(memberToken, sessionId, "GOING").andExpect(status().isOk());

        mockMvc.perform(as(memberToken, post("/api/groups/" + groupId + "/leave"))).andExpect(status().isNoContent());
        sessions(leaderToken, "upcoming").andExpect(jsonPath("$[0].goingCount").value(0));

        String secondEmail = createStudent("Second Member", COURSE);
        String secondToken = login(secondEmail);
        addMember(groupId, leaderToken, secondToken);
        rsvp(secondToken, sessionId, "GOING").andExpect(status().isOk());
        mockMvc.perform(as(leaderToken, delete("/api/groups/" + groupId + "/members/" + idOf(secondEmail))))
                .andExpect(status().isNoContent());
        sessions(leaderToken, "upcoming").andExpect(jsonPath("$[0].goingCount").value(0));
    }

    @Test
    void afterATransferTheNewLeaderManagesSessionsAndTheOldOneCannot() throws Exception {
        UUID sessionId = schedule(leaderToken);

        mockMvc.perform(as(leaderToken, put("/api/groups/" + groupId + "/leader"))
                .content("{\"studentId\":\"" + memberId + "\"}")).andExpect(status().isOk());

        mockMvc.perform(as(leaderToken, post(sessionsUrl() + "/" + sessionId + "/cancel"))).andExpect(status().isForbidden());
        mockMvc.perform(as(memberToken, post(sessionsUrl() + "/" + sessionId + "/cancel"))).andExpect(status().isOk());
    }

    @Test
    void deletingAnAccountWithAnAnswerAlsoRemovesTheAnswer() throws Exception {
        UUID sessionId = schedule(leaderToken);
        rsvp(memberToken, sessionId, "GOING").andExpect(status().isOk());
        String adminToken = login(createAdministrator());

        mockMvc.perform(as(adminToken, delete("/api/admin/users/" + memberId))).andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("select count(*) from session_rsvp where student_id = ?", Long.class, memberId)).isZero();
        sessions(leaderToken, "upcoming").andExpect(jsonPath("$[0].goingCount").value(0));
    }

    private String sessionsUrl() {
        return "/api/groups/" + groupId + "/sessions";
    }

    private ResultActions sessions(String token, String scope) throws Exception {
        return mockMvc.perform(as(token, get(sessionsUrl()).param("scope", scope)));
    }

    private ResultActions rsvp(String token, UUID sessionId, String response) throws Exception {
        return mockMvc.perform(as(token, put(sessionsUrl() + "/" + sessionId + "/rsvp"))
                .content("{\"response\":\"" + response + "\"}"));
    }

    private UUID schedule(String token) throws Exception {
        return schedule(token, Instant.now().plus(Duration.ofDays(3)));
    }

    private UUID schedule(String token, Instant start) throws Exception {
        String response = mockMvc.perform(as(token, post(sessionsUrl())).content(body("Paper drill", start)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(read(response, "$.id"));
    }

    private static String body(String title) {
        return body(title, Instant.now().plus(Duration.ofDays(3)));
    }

    private static String body(String title, Instant start) {
        return """
                {"title":"%s","startsAt":"%s","endsAt":"%s","meetingMode":"IN_PERSON",
                 "location":"SCIS GSR 2-1","note":"Bring notes"}
                """.formatted(title, start, start.plus(Duration.ofHours(2)));
    }

    private UUID createGroup(String token) throws Exception {
        String response = mockMvc.perform(as(token, post("/api/groups")).content("""
                {"courseCode":"%s","name":"Session Group","description":"Sessions","goals":["CONCEPT_REVIEW"],
                 "meetingMode":"IN_PERSON","availability":[{"day":"WED","start":"19:00","end":"21:00"}],"maxSize":5}
                """.formatted(COURSE))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(read(response, "$.id"));
    }

    private void addMember(UUID group, String leader, String token) throws Exception {
        mockMvc.perform(as(token, post("/api/groups/" + group + "/join-requests")).content("{\"message\":\"Hi\"}"))
                .andExpect(status().is2xxSuccessful());
        String requests = mockMvc.perform(as(leader, get("/api/groups/" + group + "/join-requests")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String requestId = read(requests, "$[0].id");
        mockMvc.perform(as(leader, post("/api/groups/" + group + "/join-requests/" + requestId + "/accept")))
                .andExpect(status().isOk());
    }
}
