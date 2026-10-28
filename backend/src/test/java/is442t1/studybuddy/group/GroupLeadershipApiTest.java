package is442t1.studybuddy.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import is442t1.studybuddy.ApiTestSupport;
import org.springframework.jdbc.core.JdbcTemplate;

class GroupLeadershipApiTest extends ApiTestSupport {

    @Autowired private StudyGroupLeaderRepository leaders;
    @Autowired private JdbcTemplate jdbc;

    private String leaderEmail;
    private String leaderToken;
    private String memberEmail;
    private String memberToken;
    private String groupId;

    @BeforeEach
    void createGroupWithMember() throws Exception {
        leaderEmail = createStudent("Transfer Leader", "LEAD101");
        leaderToken = login(leaderEmail);
        memberEmail = createStudent("Transfer Member", "LEAD101");
        memberToken = login(memberEmail);
        groupId = createGroup();
        accept(memberToken);
    }

    @Test
    void leaderCanTransferAndImmediatelyLosesManagementAccessButRemainsAMember() throws Exception {
        UUID oldId = idOf(leaderEmail);
        UUID successorId = idOf(memberEmail);
        mockMvc.perform(as(leaderToken, get("/api/groups/" + groupId + "/leadership")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.eligibleMembers[0].id").value(successorId.toString()));
        transfer(leaderToken, successorId, 200);
        assertThat(leaders.existsById(oldId)).isFalse();
        assertThat(leaders.existsById(successorId)).isTrue();
        mockMvc.perform(as(leaderToken, get("/api/groups/" + groupId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.myRole").value("MEMBER"))
                .andExpect(jsonPath("$.memberCount").value(2)).andExpect(jsonPath("$.leader.id").value(successorId.toString()));
        mockMvc.perform(as(leaderToken, get("/api/groups/" + groupId + "/join-requests"))).andExpect(status().isOk());
        mockMvc.perform(as(leaderToken, get("/api/groups/" + groupId + "/best-times"))).andExpect(status().isOk());
        mockMvc.perform(as(leaderToken, put("/api/groups/" + groupId)).content(groupBody())).andExpect(status().isForbidden());
        transfer(leaderToken, oldId, 403);
        mockMvc.perform(as(memberToken, put("/api/groups/" + groupId)).content(groupBody())).andExpect(status().isOk());
    }

    @Test
    void memberCanViewPendingRequestsButCannotPerformAnyManagementAction() throws Exception {
        String applicantToken = login(createStudent("Pending Applicant", "LEAD101"));
        String requestId = request(applicantToken);
        mockMvc.perform(as(memberToken, get("/api/groups/" + groupId + "/join-requests")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(requestId));
        mockMvc.perform(as(memberToken, get("/api/groups/" + groupId + "/leadership"))).andExpect(status().isForbidden());
        transfer(memberToken, idOf(leaderEmail), 403);
        mockMvc.perform(as(memberToken, put("/api/groups/" + groupId)).content(groupBody())).andExpect(status().isForbidden());
        mockMvc.perform(as(memberToken, post("/api/groups/" + groupId + "/close"))).andExpect(status().isForbidden());
        mockMvc.perform(as(memberToken, delete("/api/groups/" + groupId + "/members/" + idOf(leaderEmail))))
                .andExpect(status().isForbidden());
        for (String action : new String[] {"accept", "reject"}) {
            mockMvc.perform(as(memberToken, post("/api/groups/" + groupId + "/join-requests/" + requestId + "/" + action)))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(as(applicantToken, get("/api/groups/" + groupId + "/join-requests"))).andExpect(status().isForbidden());
    }

    @Test
    void rejectsSelfOutsiderSuspendedSuccessorsAndMissingSelection() throws Exception {
        transfer(leaderToken, idOf(leaderEmail), 400);
        transfer(leaderToken, idOf(createStudent("Outsider Successor", "LEAD101")), 400);
        jdbc.update("update users set status = 'SUSPENDED' where id = ?", idOf(memberEmail));
        transfer(leaderToken, idOf(memberEmail), 400);
        mockMvc.perform(as(leaderToken, get("/api/groups/" + groupId + "/leadership")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.eligibleMembers").isEmpty());
        mockMvc.perform(as(leaderToken, put("/api/groups/" + groupId + "/leader")).content("{}"))
                .andExpect(status().isBadRequest());
        assertThat(leaders.existsById(idOf(leaderEmail))).isTrue();
    }

    @Test
    void countsLeadershipSeparatelyFromMembershipUntilTheFinalTransfer() throws Exception {
        String firstGroup = groupId;
        groupId = createGroup();
        accept(memberToken);
        String secondGroup = groupId;
        String adminToken = login(createAdministrator());
        assertCounts(adminToken, 2, 2);
        groupId = firstGroup;
        transfer(leaderToken, idOf(memberEmail), 200);
        assertCounts(adminToken, 2, 1);
        assertThat(leaders.existsById(idOf(leaderEmail))).isTrue();
        groupId = secondGroup;
        transfer(leaderToken, idOf(memberEmail), 200);
        assertCounts(adminToken, 2, 0);
        assertThat(leaders.existsById(idOf(leaderEmail))).isFalse();
    }

    @Test
    void closedGroupLeadershipCanTransferWhileTheGroupRemainsClosed() throws Exception {
        mockMvc.perform(as(leaderToken, post("/api/groups/" + groupId + "/close"))).andExpect(status().isOk());
        transfer(leaderToken, idOf(memberEmail), 200);
        mockMvc.perform(as(memberToken, get("/api/groups/" + groupId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.myRole").value("LEADER"));
        mockMvc.perform(as(memberToken, put("/api/groups/" + groupId)).content(groupBody())).andExpect(status().isConflict());
    }

    private void assertCounts(String adminToken, int memberships, int leaderships) throws Exception {
        String account = "$[?(@.id=='" + idOf(leaderEmail) + "')]";
        mockMvc.perform(as(adminToken, get("/api/admin/users"))).andExpect(status().isOk())
                .andExpect(jsonPath(account + ".groupCount", hasItem(memberships)))
                .andExpect(jsonPath(account + ".ledGroupCount", hasItem(leaderships)));
    }

    private void transfer(String token, UUID successorId, int expectedStatus) throws Exception {
        mockMvc.perform(as(token, put("/api/groups/" + groupId + "/leader"))
                .content("{\"studentId\":\"" + successorId + "\"}"))
                .andExpect(status().is(expectedStatus));
    }

    private String createGroup() throws Exception {
        String body = mockMvc.perform(as(leaderToken, post("/api/groups")).content(groupBody()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return read(body, "$.id");
    }

    private String request(String token) throws Exception {
        mockMvc.perform(as(token, post("/api/groups/" + groupId + "/join-requests")).content("{}"))
                .andExpect(status().isCreated());
        String body = mockMvc.perform(as(leaderToken, get("/api/groups/" + groupId + "/join-requests")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return read(body, "$[0].id");
    }

    private void accept(String token) throws Exception {
        String requestId = request(token);
        mockMvc.perform(as(leaderToken, post("/api/groups/" + groupId + "/join-requests/" + requestId + "/accept")))
                .andExpect(status().isOk());
    }

    private String groupBody() {
        return """
                {"courseCode":"LEAD101","name":"Transfer Group","description":"Study together",
                 "goals":["EXAM_PREPARATION"],"meetingMode":"ONLINE","maxSize":8,
                 "availability":[{"day":"WED","start":"19:00","end":"21:00"}]}
                """;
    }
}
