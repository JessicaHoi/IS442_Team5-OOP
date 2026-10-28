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

import is442t1.studybuddy.model.enums.RequestStatus;

class SuspendedAccountInteractionApiTest extends AccountManagementTestSupport {

    @Test
    void suspendedStudentsAreExcludedFromMatchingAndCannotReceiveNewBuddyRequests() throws Exception {
        String viewer = createStudent("Active Matching Viewer", "ACCOUNT101");
        String target = createStudent("Suspended Match Student", "ACCOUNT101");
        String token = login(viewer);
        suspend(idOf(target));
        String matches = mockMvc.perform(as(token, get("/api/matches")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<String> ids = read(matches, "$[*].student.id");
        assertThat(ids).doesNotContain(idOf(target).toString());
        mockMvc.perform(as(token, post("/api/connections")).content(json(Map.of("toStudentId", idOf(target)))))
                .andExpect(status().isConflict());
        assertThat(requests.findAllForStudent(idOf(target))).isEmpty();
    }

    @Test
    void pendingBuddyRequestCannotBeAcceptedAfterTheSenderIsSuspended() throws Exception {
        String sender = createStudent("Suspended Pending Sender", "ACCOUNT101");
        String receiver = createStudent("Active Pending Receiver", "ACCOUNT101");
        String response = mockMvc.perform(as(login(sender), post("/api/connections"))
                .content(json(Map.of("toStudentId", idOf(receiver))))).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String requestId = read(response, "$.id");
        suspend(idOf(sender));
        mockMvc.perform(as(login(receiver), post("/api/connections/" + requestId + "/accept")))
                .andExpect(status().isConflict());
        assertThat(requests.findById(UUID.fromString(requestId)).orElseThrow().getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(connections.findActiveForStudent(idOf(sender))).isEmpty();
    }

    @Test
    void suspendedApplicantsCannotBeAcceptedAndSuspendedLeadersCannotReceiveNewJoinRequests() throws Exception {
        String owner = createStudent("Suspension Group Owner", "ACCOUNT101");
        String ownerToken = login(owner);
        UUID group = createGroup(ownerToken, "Suspension Requests Group");
        String applicant = createStudent("Suspended Applicant", "ACCOUNT101");
        UUID requestId = requestToJoin(login(applicant), group);
        suspend(idOf(applicant));
        mockMvc.perform(as(ownerToken, post("/api/groups/" + group + "/join-requests/" + requestId + "/accept")))
                .andExpect(status().isConflict());
        assertThat(membershipRequests.findById(requestId).orElseThrow().getStatus()).isEqualTo(RequestStatus.PENDING);
        mockMvc.perform(as(ownerToken, get("/api/groups/" + group))).andExpect(status().isOk())
                .andExpect(jsonPath("$.memberCount").value(1));
        suspend(idOf(owner));
        String newcomer = login(createStudent("New Active Applicant", "ACCOUNT101"));
        mockMvc.perform(as(newcomer, post("/api/groups/" + group + "/join-requests")).content("{}"))
                .andExpect(status().isConflict());
    }

    private void suspend(UUID id) throws Exception {
        var update = fields(users.findById(id).orElseThrow());
        update.put("status", "SUSPENDED");
        update(id, update, 200);
    }
}
