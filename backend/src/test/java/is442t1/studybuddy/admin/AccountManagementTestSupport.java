package is442t1.studybuddy.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import tools.jackson.databind.ObjectMapper;

import is442t1.studybuddy.ApiTestSupport;
import is442t1.studybuddy.auth.SessionTokenService;
import is442t1.studybuddy.connection.MatchRequestRepository;
import is442t1.studybuddy.connection.StudyBuddyConnectionRepository;
import is442t1.studybuddy.group.MembershipRequestRepository;
import is442t1.studybuddy.group.StudyGroupLeaderRepository;
import is442t1.studybuddy.group.StudyGroupRepository;
import is442t1.studybuddy.model.User;
import is442t1.studybuddy.model.UserRepository;

abstract class AccountManagementTestSupport extends ApiTestSupport {

    @Autowired protected UserRepository users;
    @Autowired protected StudyGroupRepository groups;
    @Autowired protected StudyGroupLeaderRepository leaders;
    @Autowired protected StudyBuddyConnectionRepository connections;
    @Autowired protected MatchRequestRepository requests;
    @Autowired protected MembershipRequestRepository membershipRequests;
    @Autowired protected PasswordEncoder encoder;
    @Autowired protected TransactionTemplate transaction;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired private SessionTokenService sessionTokens;

    protected String adminEmail;
    protected String adminToken;

    @BeforeEach
    void createActingAdministrator() throws Exception {
        adminEmail = createAdministrator();
        adminToken = login(adminEmail);
    }

    protected static String json(Object value) {
        return new ObjectMapper().writeValueAsString(value);
    }

    protected Map<String, Object> fields(String name, String email, String role, String status) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("email", email);
        body.put("role", role);
        body.put("status", status);
        body.put("password", PASSWORD);
        return body;
    }

    protected Map<String, Object> fields(User user) {
        Map<String, Object> body = fields(user.getDisplayName(), user.getEmail(), user.getRole().name(), user.getStatus().name());
        body.remove("password");
        return body;
    }

    protected Map<String, Object> createAccount(String name, String role) throws Exception {
        var body = fields(name, UUID.randomUUID() + "@test.studybuddy.example", role, "ACTIVE");
        String response = mockMvc.perform(as(adminToken, post("/api/admin/users")).content(json(body)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return read(response, "$");
    }

    protected UUID accountId(Map<String, Object> account) {
        return UUID.fromString((String) account.get("id"));
    }

    protected void update(UUID id, Map<String, Object> body, int expectedStatus) throws Exception {
        mockMvc.perform(as(adminToken, put("/api/admin/users/" + id)).content(json(body)))
                .andExpect(status().is(expectedStatus));
    }

    protected UUID createGroup(String leaderToken, String name) throws Exception {
        ensureCourses("ACCOUNT101");
        String body = """
                {"courseCode":"ACCOUNT101","name":"%s","description":"Account integration test",
                 "goals":["EXAM_PREPARATION"],"meetingMode":"ONLINE","maxSize":8,
                 "availability":[{"day":"WED","start":"19:00","end":"21:00"}]}
                """.formatted(name);
        String response = mockMvc.perform(as(leaderToken, post("/api/groups")).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(read(response, "$.id"));
    }

    protected UUID requestToJoin(String token, UUID groupId) throws Exception {
        mockMvc.perform(as(token, post("/api/groups/" + groupId + "/join-requests")).content("{}"))
                .andExpect(status().isCreated());
        return transaction.execute(status -> groups.findById(groupId).orElseThrow().getPendingRequests().stream()
                .filter(request -> request.getStudent().getId().equals(
                        userIdForToken(token)))
                .findFirst().orElseThrow().getId());
    }

    private UUID userIdForToken(String token) {
        return sessionTokens.resolve(token).orElseThrow().id();
    }

    protected void acceptMember(String leaderToken, UUID groupId, UUID requestId) throws Exception {
        mockMvc.perform(as(leaderToken, post("/api/groups/" + groupId + "/join-requests/" + requestId + "/accept")))
                .andExpect(status().isOk());
    }
}
