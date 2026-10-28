package is442t1.studybuddy.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import is442t1.studybuddy.ApiTestSupport;
import is442t1.studybuddy.connection.MatchRequest;
import is442t1.studybuddy.connection.MatchRequestRepository;
import is442t1.studybuddy.connection.StudyBuddyConnection;
import is442t1.studybuddy.connection.StudyBuddyConnectionRepository;
import is442t1.studybuddy.model.UserRepository;
import is442t1.studybuddy.model.enums.AccountStatus;
import is442t1.studybuddy.model.enums.ConnectionStatus;

class UserAccountApiTest extends ApiTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudyBuddyConnectionRepository connectionRepository;

    @Autowired
    private MatchRequestRepository requestRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void onlyAdministratorsCanListAccounts() throws Exception {
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(as(login(createStudent("Restricted Student")), get("/api/admin/users")))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(login(createAdministrator()), get("/api/admin/users")))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
    }

    @Test
    void listsSafeAccountFieldsIncludingSuspendedAndIncompleteAccounts() throws Exception {
        String adminEmail = createAdministrator();
        SystemAdministrator administrator = (SystemAdministrator) userRepository.findByEmailIgnoreCase(adminEmail).orElseThrow();
        administrator.setName("Account Administrator");
        userRepository.saveAndFlush(administrator);
        String adminToken = login(adminEmail);
        String studentEmail = createStudent("Suspended Account");
        UUID studentId = idOf(studentEmail);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var student = studentRepository.findById(studentId).orElseThrow();
            student.setStatus(AccountStatus.SUSPENDED);
            student.setProgram(null);
            student.setSchool(null);
            student.setYearOfStudy(null);
        });

        List<Map<String, Object>> accounts = accounts(adminToken);
        Map<String, Object> admin = account(accounts, idOf(adminEmail));
        assertThat(admin.get("name")).isEqualTo("Account Administrator");
        assertThat(admin.get("role")).isEqualTo("ADMIN");
        assertThat(admin.get("status")).isEqualTo("ACTIVE");
        assertThat(admin.get("lastLogin")).isNotNull();
        assertThat(admin.get("createdAt")).isNotNull();
        assertThat(admin.get("connectionCount")).isEqualTo(0);
        assertThat(admin.get("groupCount")).isEqualTo(0);

        Map<String, Object> suspended = account(accounts, studentId);
        assertThat(suspended.get("status")).isEqualTo("SUSPENDED");
        assertThat(suspended.get("lastLogin")).isNull();
        assertThat(suspended.get("connectionCount")).isEqualTo(0);
        assertThat(suspended.get("groupCount")).isEqualTo(0);
        assertThat(accounts).allSatisfy(row -> assertThat(row).containsOnlyKeys("id", "name", "email", "role",
                "status", "lastLogin", "createdAt", "connectionCount", "groupCount", "ledGroupCount"));
    }

    @Test
    void countsActiveConnectionsInBothDirectionsAndMembershipsInOpenAndClosedGroups() throws Exception {
        String leaderEmail = createStudent("Usage Leader", "USAGE101");
        String memberEmail = createStudent("Usage Member", "USAGE101");
        String thirdEmail = createStudent("Usage Third", "USAGE101");
        String fourthEmail = createStudent("Usage Fourth", "USAGE101");
        UUID leaderId = idOf(leaderEmail);
        UUID memberId = idOf(memberEmail);
        connection(leaderId, memberId, ConnectionStatus.ACTIVE);
        connection(idOf(thirdEmail), leaderId, ConnectionStatus.ACTIVE);
        connection(leaderId, idOf(fourthEmail), ConnectionStatus.ENDED);
        MatchRequest pending = new MatchRequest();
        pending.setSender(studentRepository.findById(leaderId).orElseThrow());
        pending.setReceiver(studentRepository.findById(idOf(fourthEmail)).orElseThrow());
        requestRepository.saveAndFlush(pending);

        String leaderToken = login(leaderEmail);
        String memberToken = login(memberEmail);
        createGroup(leaderToken, "Open Group");
        String closedGroup = createGroup(leaderToken, "Closed Group");
        mockMvc.perform(as(memberToken, post("/api/groups/" + closedGroup + "/join-requests")).content("{}"))
                .andExpect(status().isCreated());
        String requests = mockMvc.perform(as(leaderToken, get("/api/groups/" + closedGroup + "/join-requests")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String requestId = read(requests, "$[0].id");
        mockMvc.perform(as(leaderToken, post("/api/groups/" + closedGroup + "/join-requests/" + requestId + "/accept")))
                .andExpect(status().isOk());
        mockMvc.perform(as(leaderToken, post("/api/groups/" + closedGroup + "/close")))
                .andExpect(status().isOk());

        List<Map<String, Object>> accounts = accounts(login(createAdministrator()));
        assertThat(account(accounts, leaderId).get("connectionCount")).isEqualTo(2);
        assertThat(account(accounts, leaderId).get("groupCount")).isEqualTo(2);
        assertThat(account(accounts, memberId).get("connectionCount")).isEqualTo(1);
        assertThat(account(accounts, memberId).get("groupCount")).isEqualTo(1);
        assertThat(account(accounts, idOf(fourthEmail)).get("connectionCount")).isEqualTo(0);
    }

    private List<Map<String, Object>> accounts(String token) throws Exception {
        String body = mockMvc.perform(as(token, get("/api/admin/users"))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return read(body, "$");
    }

    private Map<String, Object> account(List<Map<String, Object>> accounts, UUID id) {
        return accounts.stream().filter(row -> id.toString().equals(row.get("id"))).findFirst().orElseThrow();
    }

    private void connection(UUID requester, UUID recipient, ConnectionStatus status) {
        StudyBuddyConnection connection = new StudyBuddyConnection();
        connection.setRequester(studentRepository.findById(requester).orElseThrow());
        connection.setRecipient(studentRepository.findById(recipient).orElseThrow());
        connection.setStatus(status);
        connectionRepository.saveAndFlush(connection);
    }

    private String createGroup(String token, String name) throws Exception {
        String body = mockMvc.perform(as(token, post("/api/groups")).content("""
                {"courseCode":"USAGE101","name":"%s","description":"Account usage test",
                 "goals":["EXAM_PREPARATION"],"meetingMode":"ONLINE","maxSize":5,
                 "availability":[{"day":"WED","start":"19:00","end":"21:00"}]}
                """.formatted(name))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return read(body, "$.id");
    }
}
