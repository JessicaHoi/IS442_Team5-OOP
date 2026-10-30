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
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import is442t1.studybuddy.student.Student;
import is442t1.studybuddy.model.enums.AccountStatus;

class UserAccountMutationApiTest extends AccountManagementTestSupport {

    @Test
    void everyNewEndpointRequiresAnAdministrator() throws Exception {
        String studentToken = login(createStudent("Restricted Account Editor"));
        String id = UUID.randomUUID().toString();
        Map<String, HttpMethod> paths = Map.of(
                "/api/admin/users", HttpMethod.POST,
                "/api/admin/users/" + id, HttpMethod.PUT,
                "/api/admin/users/" + id + "/led-groups", HttpMethod.GET,
                "/api/admin/users/" + id + "/led-groups/" + id + "/leader", HttpMethod.PUT);
        for (var path : paths.entrySet()) {
            mockMvc.perform(MockMvcRequestBuilders.request(path.getValue(), path.getKey())).andExpect(status().isUnauthorized());
            mockMvc.perform(as(studentToken, MockMvcRequestBuilders.request(path.getValue(), path.getKey())))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(as(studentToken, MockMvcRequestBuilders.delete("/api/admin/users/" + id)))
                .andExpect(status().isForbidden());
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/admin/users/" + id)).andExpect(status().isUnauthorized());
    }

    @Test
    void createsBothSubclassesWithHashedPasswordsAndNormalizedDetails() throws Exception {
        for (String role : List.of("STUDENT", "ADMIN")) {
            String email = UUID.randomUUID() + "@TEST.STUDYBUDDY.EXAMPLE";
            var body = fields("  New Account  ", "  " + email + "  ", role, "ACTIVE");
            String response = mockMvc.perform(as(adminToken, post("/api/admin/users")).content(json(body)))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("New Account"))
                    .andExpect(jsonPath("$.role").value(role)).andExpect(jsonPath("$.password").doesNotExist())
                    .andExpect(jsonPath("$.lastLogin").isEmpty()).andExpect(jsonPath("$.createdAt").isNotEmpty())
                    .andExpect(jsonPath("$.connectionCount").value(0)).andExpect(jsonPath("$.groupCount").value(0))
                    .andReturn().getResponse().getContentAsString();
            UUID id = UUID.fromString(read(response, "$.id"));
            var saved = users.findById(id).orElseThrow();
            assertThat(saved.getEmail()).isEqualTo(email.toLowerCase());
            assertThat(saved.getPassword()).isNotEqualTo(PASSWORD);
            assertThat(encoder.matches(PASSWORD, saved.getPassword())).isTrue();
            assertThat(saved).isInstanceOf(role.equals("STUDENT") ? Student.class : SystemAdministrator.class);
            mockMvc.perform(as(login(email), get("/api/auth/me"))).andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("New Account"));
        }
    }

    @Test
    void newlyCreatedStudentCanCompleteAnInitiallyEmptyProfile() throws Exception {
        var account = createAccount("Incomplete Student", "STUDENT");
        String token = login((String) account.get("email"));
        mockMvc.perform(as(token, get("/api/students/me"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.programme").isEmpty()).andExpect(jsonPath("$.courses").isEmpty())
                .andExpect(jsonPath("$.preferences").isEmpty());
        ensureCourses("ACCOUNT101");
        mockMvc.perform(as(token, MockMvcRequestBuilders.put("/api/students/me/profile")).content("""
                {"name":"Completed Student","school":"School of Computing and Information Systems",
                 "programme":"Information Systems","yearOfStudy":2,"contactNumber":"+65 9123 4567","courses":["ACCOUNT101"]}
                """)).andExpect(status().isOk()).andExpect(jsonPath("$.programme").value("Information Systems"));
        mockMvc.perform(as(token, MockMvcRequestBuilders.put("/api/students/me/preferences")).content("""
                {"preferences":[{"course":"ACCOUNT101","meetingMode":"ONLINE","groupFormat":"SMALL_GROUP",
                 "goals":["EXAM_PREPARATION"],"availability":[{"day":"WED","start":"19:00","end":"21:00"}]}]}
                """)).andExpect(status().isOk()).andExpect(jsonPath("$.preferences[0].course").value("ACCOUNT101"));
    }

    @Test
    void rejectsInvalidFieldsAndPasswordsWithoutCreatingAccounts() throws Exception {
        long count = users.count();
        for (var invalid : List.of(
                Map.of("name", " "), Map.of("email", "invalid"), Map.of("role", "OTHER"),
                Map.of("status", "OTHER"), Map.of("password", "short"), Map.of("password", "      "),
                Map.of("password", "a".repeat(73)), Map.of("password", "界".repeat(25)),
                Map.of("name", "a".repeat(101)), Map.of("email", "a".repeat(250) + "@test.studybuddy.example"))) {
            var body = fields("Invalid Account", UUID.randomUUID() + "@test.studybuddy.example", "STUDENT", "ACTIVE");
            body.putAll(invalid);
            mockMvc.perform(as(adminToken, post("/api/admin/users")).content(json(body)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isString());
        }
        assertThat(users.count()).isEqualTo(count);
    }

    @Test
    void rejectsDuplicateEmailAcrossRolesIncludingCaseAndWhitespace() throws Exception {
        var duplicate = fields("Duplicate", "  " + adminEmail.toUpperCase() + "  ", "STUDENT", "ACTIVE");
        mockMvc.perform(as(adminToken, post("/api/admin/users")).content(json(duplicate)))
                .andExpect(status().isConflict());
        var account = createAccount("Duplicate Update", "STUDENT");
        var update = fields(users.findById(accountId(account)).orElseThrow());
        update.put("email", adminEmail.toUpperCase());
        update(accountId(account), update, 409);
        assertThat(users.findById(accountId(account)).orElseThrow().getEmail()).isEqualTo(account.get("email"));
    }

    @Test
    void missingNullAndEmptyPasswordsPreserveTheExistingHashAndSessions() throws Exception {
        var account = createAccount("Password Preserved", "STUDENT");
        UUID id = accountId(account);
        String token = login((String) account.get("email"));
        String hash = users.findById(id).orElseThrow().getPassword();
        var update = fields(users.findById(id).orElseThrow());
        update.put("name", "Updated Name");
        update.put("email", "  " + UUID.randomUUID() + "@TEST.STUDYBUDDY.EXAMPLE  ");
        update(id, update, 200);
        update.put("password", null);
        update(id, update, 200);
        update.put("password", "");
        update(id, update, 200);
        assertThat(users.findById(id).orElseThrow().getPassword()).isEqualTo(hash);
        mockMvc.perform(as(token, get("/api/auth/me"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Test
    void passwordReplacementRevokesEveryOldSessionAndAllowsANewLogin() throws Exception {
        var account = createAccount("Reset Password", "ADMIN");
        UUID id = accountId(account);
        String first = login((String) account.get("email"));
        String second = login((String) account.get("email"));
        var update = fields(users.findById(id).orElseThrow());
        update.put("password", "replacement-password");
        update(id, update, 200);
        mockMvc.perform(as(first, get("/api/auth/me"))).andExpect(status().isUnauthorized());
        mockMvc.perform(as(second, get("/api/auth/me"))).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType("application/json")
                .content(json(Map.of("email", account.get("email"), "password", "replacement-password"))))
                .andExpect(status().isOk());
    }

    @Test
    void suspensionRevokesSessionsEvenWithoutARequestDuringSuspension() throws Exception {
        var account = createAccount("Suspend Account", "STUDENT");
        UUID id = accountId(account);
        String token = login((String) account.get("email"));
        var update = fields(users.findById(id).orElseThrow());
        update.put("status", "SUSPENDED");
        update(id, update, 200);
        update.put("status", "ACTIVE");
        update(id, update, 200);
        mockMvc.perform(as(token, get("/api/auth/me"))).andExpect(status().isUnauthorized());
        mockMvc.perform(as(login((String) account.get("email")), get("/api/auth/me"))).andExpect(status().isOk());
    }

    @Test
    void protectsSelfAndRejectsRoleConversionAndMissingTargets() throws Exception {
        UUID ownId = idOf(adminEmail);
        var self = fields(users.findById(ownId).orElseThrow());
        self.put("status", "SUSPENDED");
        update(ownId, self, 400);
        self.put("status", "ACTIVE");
        self.put("role", "STUDENT");
        update(ownId, self, 400);
        var account = createAccount("Fixed Student Role", "STUDENT");
        var update = fields(users.findById(accountId(account)).orElseThrow());
        update.put("role", "ADMIN");
        update(accountId(account), update, 400);
        update(UUID.randomUUID(), self, 404);
        mockMvc.perform(as(adminToken, get("/api/auth/me"))).andExpect(status().isOk());
    }

    @Test
    void rejectedPasswordUpdateDoesNotChangeDetailsOrRevokeSessions() throws Exception {
        var account = createAccount("Unchanged Account", "STUDENT");
        UUID id = accountId(account);
        String token = login((String) account.get("email"));
        var update = fields(users.findById(id).orElseThrow());
        update.put("name", "Must Not Persist");
        update.put("password", "short");
        update(id, update, 400);
        assertThat(users.findById(id).orElseThrow().getDisplayName()).isEqualTo("Unchanged Account");
        mockMvc.perform(as(token, get("/api/auth/me"))).andExpect(status().isOk());
    }

    @Test
    void selfPasswordResetSucceedsThenRequiresSigningInAgain() throws Exception {
        UUID id = idOf(adminEmail);
        var update = fields(users.findById(id).orElseThrow());
        update.put("password", "self-replacement-password");
        update(id, update, 200);
        mockMvc.perform(as(adminToken, get("/api/admin/users"))).andExpect(status().isUnauthorized());
    }
}
