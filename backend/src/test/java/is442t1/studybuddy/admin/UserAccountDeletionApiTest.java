package is442t1.studybuddy.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import is442t1.studybuddy.common.exception.PermissionDeniedException;
import is442t1.studybuddy.connection.MatchRequest;
import is442t1.studybuddy.connection.StudyBuddyConnection;
import is442t1.studybuddy.model.enums.AccountStatus;
import is442t1.studybuddy.model.enums.ConnectionStatus;
import is442t1.studybuddy.model.enums.RequestStatus;
import is442t1.studybuddy.student.Student;

class UserAccountDeletionApiTest extends AccountManagementTestSupport {

    @Autowired private UserAccountService accountService;

    @Test
    void removesOnlyTheDeletedStudentsRelationshipsIncludingClosedGroupMemberships() throws Exception {
        String targetEmail = createStudent("Delete Related Student", "ACCOUNT101");
        UUID targetId = idOf(targetEmail);
        String targetToken = login(targetEmail);
        String leaderEmail = createStudent("Surviving Leader", "ACCOUNT101");
        String leaderToken = login(leaderEmail);
        String survivorEmail = createStudent("Surviving Member", "ACCOUNT101");
        UUID survivorId = idOf(survivorEmail);
        String survivorToken = login(survivorEmail);
        UUID closedGroup = createGroup(leaderToken, "Closed Surviving Group");
        acceptMember(leaderToken, closedGroup, requestToJoin(targetToken, closedGroup));
        acceptMember(leaderToken, closedGroup, requestToJoin(survivorToken, closedGroup));
        mockMvc.perform(as(leaderToken, post("/api/groups/" + closedGroup + "/close"))).andExpect(status().isOk());
        UUID openGroup = createGroup(leaderToken, "Open Surviving Group");
        requestToJoin(targetToken, openGroup);
        UUID survivingRequest = requestToJoin(survivorToken, openGroup);
        mockMvc.perform(as(targetToken, put("/api/students/me/preferences")).content("""
                {"preferences":[{"course":"ACCOUNT101","meetingMode":"ONLINE","groupFormat":"SMALL_GROUP",
                 "goals":["EXAM_PREPARATION"],"availability":[{"day":"WED","start":"19:00","end":"21:00"}]}]}
                """)).andExpect(status().isOk());
        UUID preferenceId = jdbc.queryForObject("select id from study_preference where student_id = ?", UUID.class, targetId);
        createConnection(targetId, survivorId, ConnectionStatus.ACTIVE);
        createConnection(idOf(leaderEmail), targetId, ConnectionStatus.ENDED);
        UUID preservedConnection = createConnection(survivorId, idOf(leaderEmail), ConnectionStatus.ACTIVE);
        createRequest(targetId, survivorId, RequestStatus.PENDING);
        createRequest(idOf(leaderEmail), targetId, RequestStatus.ACCEPTED);
        createRequest(targetId, idOf(leaderEmail), RequestStatus.REJECTED);

        mockMvc.perform(as(adminToken, delete("/api/admin/users/" + targetId))).andExpect(status().isNoContent());

        assertThat(users.existsById(targetId)).isFalse();
        assertThat(studentRepository.existsById(targetId)).isFalse();
        assertThat(connections.findAllForStudent(targetId)).isEmpty();
        assertThat(requests.findAllForStudent(targetId)).isEmpty();
        assertThat(membershipRequests.findByStudent_Id(targetId)).isEmpty();
        assertThat(groups.findByMemberId(targetId)).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from student_course where student_id = ?", Long.class, targetId)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from study_preference where student_id = ?", Long.class, targetId)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from study_preference_goal where study_preference_id = ?", Long.class, preferenceId)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from study_preference_availability where study_preference_id = ?", Long.class, preferenceId)).isZero();
        assertThat(groups.existsById(closedGroup)).isTrue();
        assertThat(groups.existsById(openGroup)).isTrue();
        assertThat(membershipRequests.existsById(survivingRequest)).isTrue();
        assertThat(connections.existsById(preservedConnection)).isTrue();
        assertThat(users.existsById(survivorId)).isTrue();
        mockMvc.perform(as(leaderToken, get("/api/groups/" + closedGroup))).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED")).andExpect(jsonPath("$.memberCount").value(2));
        mockMvc.perform(as(targetToken, get("/api/auth/me"))).andExpect(status().isUnauthorized());
    }

    @Test
    void everyLedGroupMustBeTransferredBeforeDeletionAndTheLastTransferRemovesLeaderStatus() throws Exception {
        String oldEmail = createStudent("Transferring Leader", "ACCOUNT101");
        UUID oldId = idOf(oldEmail);
        String oldToken = login(oldEmail);
        String newEmail = createStudent("Appointed Member", "ACCOUNT101");
        UUID newId = idOf(newEmail);
        String newToken = login(newEmail);
        UUID first = createGroup(oldToken, "First Transfer Group");
        UUID second = createGroup(oldToken, "Second Transfer Group");
        acceptMember(oldToken, first, requestToJoin(newToken, first));
        acceptMember(oldToken, second, requestToJoin(newToken, second));
        mockMvc.perform(as(oldToken, post("/api/groups/" + second + "/close"))).andExpect(status().isOk());
        mockMvc.perform(as(adminToken, delete("/api/admin/users/" + oldId))).andExpect(status().isConflict());
        mockMvc.perform(as(oldToken, get("/api/auth/me"))).andExpect(status().isOk());
        transfer(oldId, first, newId, 200);
        assertThat(leaders.existsById(oldId)).isTrue();
        mockMvc.perform(as(adminToken, delete("/api/admin/users/" + oldId))).andExpect(status().isConflict());
        transfer(oldId, second, newId, 200);
        assertThat(leaders.existsById(oldId)).isFalse();
        assertThat(studentRepository.findById(oldId).orElseThrow().getClass()).isEqualTo(Student.class);
        assertThat(leaders.existsById(newId)).isTrue();
        mockMvc.perform(as(newToken, get("/api/groups/" + first + "/join-requests"))).andExpect(status().isOk());
        mockMvc.perform(as(oldToken, get("/api/groups/" + first + "/join-requests"))).andExpect(status().isOk());
        mockMvc.perform(as(adminToken, delete("/api/admin/users/" + oldId))).andExpect(status().isNoContent());
        for (UUID group : List.of(first, second)) {
            mockMvc.perform(as(newToken, get("/api/groups/" + group))).andExpect(status().isOk())
                    .andExpect(jsonPath("$.leader.id").value(newId.toString())).andExpect(jsonPath("$.memberCount").value(1));
        }
    }

    @Test
    void administratorCanReplaceASuspendedLeaderWithAnExistingActiveMember() throws Exception {
        String oldEmail = createStudent("Suspended Group Owner", "ACCOUNT101");
        UUID oldId = idOf(oldEmail);
        String oldToken = login(oldEmail);
        String newEmail = createStudent("Active Replacement", "ACCOUNT101");
        UUID newId = idOf(newEmail);
        String newToken = login(newEmail);
        UUID group = createGroup(oldToken, "Suspended Owner Group");
        acceptMember(oldToken, group, requestToJoin(newToken, group));
        var update = fields(users.findById(oldId).orElseThrow());
        update.put("status", "SUSPENDED");
        update(oldId, update, 200);
        mockMvc.perform(as(adminToken, get("/api/admin/users/" + oldId + "/led-groups")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].eligibleMembers[0].id").value(newId.toString()));
        transfer(oldId, group, newId, 200);
        assertThat(leaders.existsById(oldId)).isFalse();
        assertThat(users.findById(oldId).orElseThrow().getStatus()).isEqualTo(AccountStatus.SUSPENDED);
        mockMvc.perform(as(newToken, get("/api/groups/" + group + "/join-requests"))).andExpect(status().isOk());
        mockMvc.perform(as(oldToken, get("/api/groups/" + group))).andExpect(status().isUnauthorized());
    }

    @Test
    void transferRejectsNonmembersSuspendedMembersAdministratorsAndWrongGroups() throws Exception {
        String oldEmail = createStudent("Protected Leader", "ACCOUNT101");
        UUID oldId = idOf(oldEmail);
        String oldToken = login(oldEmail);
        UUID group = createGroup(oldToken, "Protected Leadership");
        String outsider = createStudent("Nonmember Replacement", "ACCOUNT101");
        transfer(oldId, group, idOf(outsider), 400);
        transfer(oldId, group, idOf(adminEmail), 400);
        transfer(oldId, group, oldId, 400);
        acceptMember(oldToken, group, requestToJoin(login(outsider), group));
        var update = fields(users.findById(idOf(outsider)).orElseThrow());
        update.put("status", "SUSPENDED");
        update(idOf(outsider), update, 200);
        transfer(oldId, group, idOf(outsider), 400);
        transfer(oldId, UUID.randomUUID(), oldId, 404);
        mockMvc.perform(as(adminToken, delete("/api/admin/users/" + oldId))).andExpect(status().isConflict());
        assertThat(leaders.existsById(oldId)).isTrue();
    }

    @Test
    void closedGroupWithoutAnEligibleSuccessorKeepsDeletionBlocked() throws Exception {
        String oldEmail = createStudent("Only Closed Member", "ACCOUNT101");
        String oldToken = login(oldEmail);
        UUID group = createGroup(oldToken, "Closed Solo Group");
        mockMvc.perform(as(oldToken, post("/api/groups/" + group + "/close"))).andExpect(status().isOk());
        mockMvc.perform(as(adminToken, get("/api/admin/users/" + idOf(oldEmail) + "/led-groups")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].eligibleMembers").isEmpty());
        mockMvc.perform(as(adminToken, delete("/api/admin/users/" + idOf(oldEmail)))).andExpect(status().isConflict());
    }

    @Test
    void rejectsSelfDeletionAndMissingAccountsButAllowsDeletingAnotherAdministrator() throws Exception {
        mockMvc.perform(as(adminToken, delete("/api/admin/users/" + idOf(adminEmail)))).andExpect(status().isBadRequest());
        mockMvc.perform(as(adminToken, delete("/api/admin/users/" + UUID.randomUUID()))).andExpect(status().isNotFound());
        var other = createAccount("Removable Administrator", "ADMIN");
        String token = login((String) other.get("email"));
        mockMvc.perform(as(adminToken, delete("/api/admin/users/" + accountId(other)))).andExpect(status().isNoContent());
        mockMvc.perform(as(token, get("/api/auth/me"))).andExpect(status().isUnauthorized());
        mockMvc.perform(as(adminToken, get("/api/admin/users"))).andExpect(status().isOk());
    }

    @Test
    void rollbackRestoresDeletedAccountRelationshipsAndSessions() throws Exception {
        String target = createStudent("Rollback Deletion", "ACCOUNT101");
        UUID targetId = idOf(target);
        String token = login(target);
        String leader = createStudent("Rollback Group Owner", "ACCOUNT101");
        String leaderToken = login(leader);
        UUID group = createGroup(leaderToken, "Rollback Group");
        acceptMember(leaderToken, group, requestToJoin(token, group));
        UUID connection = createConnection(targetId, idOf(leader), ConnectionStatus.ACTIVE);
        transaction.executeWithoutResult(status -> {
            accountService.deleteAccount(idOf(adminEmail), targetId);
            assertThat(users.existsById(targetId)).isFalse();
            status.setRollbackOnly();
        });
        assertThat(users.existsById(targetId)).isTrue();
        assertThat(connections.existsById(connection)).isTrue();
        assertThat(groups.findByMemberId(targetId)).hasSize(1);
        mockMvc.perform(as(token, get("/api/auth/me"))).andExpect(status().isOk());
    }

    @Test
    void concurrentAdministratorDeletionCannotRemoveBothActingAccounts() throws Exception {
        String otherEmail = createAdministrator();
        UUID firstId = idOf(adminEmail);
        UUID secondId = idOf(otherEmail);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> deleteAfterStart(start, firstId, secondId));
            var second = executor.submit(() -> deleteAfterStart(start, secondId, firstId));
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }
        assertThat(users.findAllById(List.of(firstId, secondId))).hasSize(1);
    }

    private boolean deleteAfterStart(CountDownLatch start, UUID caller, UUID target) throws Exception {
        start.await(5, TimeUnit.SECONDS);
        try {
            accountService.deleteAccount(caller, target);
            return true;
        } catch (PermissionDeniedException expected) {
            return false;
        }
    }

    private void transfer(UUID oldId, UUID groupId, UUID newId, int expectedStatus) throws Exception {
        mockMvc.perform(as(adminToken, put("/api/admin/users/" + oldId + "/led-groups/" + groupId + "/leader"))
                .content(json(Map.of("studentId", newId)))).andExpect(status().is(expectedStatus));
    }

    private UUID createConnection(UUID first, UUID second, ConnectionStatus status) {
        StudyBuddyConnection connection = new StudyBuddyConnection();
        connection.setRequester(studentRepository.findById(first).orElseThrow());
        connection.setRecipient(studentRepository.findById(second).orElseThrow());
        connection.setStatus(status);
        return connections.saveAndFlush(connection).getId();
    }

    private void createRequest(UUID first, UUID second, RequestStatus status) {
        MatchRequest request = new MatchRequest();
        request.setSender(studentRepository.findById(first).orElseThrow());
        request.setReceiver(studentRepository.findById(second).orElseThrow());
        request.setStatus(status);
        requests.saveAndFlush(request);
    }
}
