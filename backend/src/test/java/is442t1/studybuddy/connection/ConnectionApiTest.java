package is442t1.studybuddy.connection;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import is442t1.studybuddy.ApiTestSupport;

class ConnectionApiTest extends ApiTestSupport {

    @Autowired
    private StudyBuddyConnectionRepository connectionRepository;

    @Test
    void sendsAcceptsListsAndEndsAConnectionWithContactPrivacy() throws Exception {
        String requesterEmail = createStudent("Request Sender", "CON101");
        String recipientEmail = createStudent("Request Receiver", "CON101");
        String requesterToken = login(requesterEmail);
        String recipientToken = login(recipientEmail);

        String created = mockMvc.perform(as(requesterToken, post("/api/connections"))
                        .content("{\"toStudentId\":\"" + idOf(recipientEmail)
                                + "\",\"message\":\"  Let's study together!  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.direction").value("OUTGOING"))
                .andExpect(jsonPath("$.message").value("Let's study together!"))
                .andExpect(jsonPath("$.other.contactNumber").value(nullValue()))
                .andExpect(jsonPath("$.other.contactVisible").value(false))
                .andReturn().getResponse().getContentAsString();
        String requestId = read(created, "$.id");

        mockMvc.perform(as(requesterToken, post("/api/connections")
                        .content("{\"toStudentId\":\"" + idOf(recipientEmail) + "\"}")))
                .andExpect(status().isConflict());
        mockMvc.perform(as(requesterToken, get("/api/connections").param("view", "sent")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.other.name == 'Request Receiver')]", hasSize(1)));
        mockMvc.perform(as(recipientToken, get("/api/connections").param("view", "incoming")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.other.name == 'Request Sender')]", hasSize(1)));

        mockMvc.perform(as(requesterToken, post("/api/connections/" + requestId + "/accept")))
                .andExpect(status().isForbidden());

        mockMvc.perform(as(recipientToken, post("/api/connections/" + requestId + "/accept")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.respondedAt").isNotEmpty())
                .andExpect(jsonPath("$.other.contactVisible").value(true))
                .andExpect(jsonPath("$.other.contactNumber").value("+65 9123 4567"));

        String active = mockMvc.perform(as(requesterToken,
                        get("/api/connections").param("view", "active")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.other.name == 'Request Receiver')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.other.name == 'Request Receiver')].other.contactVisible").value(hasItem(true)))
                .andReturn().getResponse().getContentAsString();
        String connectionId = connectionRepository.findActiveBetween(
                        idOf(requesterEmail), idOf(recipientEmail))
                .orElseThrow()
                .getId()
                .toString();

        mockMvc.perform(as(requesterToken, delete("/api/connections/" + connectionId)))
                .andExpect(status().isNoContent());
        mockMvc.perform(as(requesterToken, get("/api/connections").param("view", "active")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.other.name == 'Request Receiver')]", hasSize(0)));
        mockMvc.perform(as(requesterToken, get("/api/students/" + idOf(recipientEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactVisible").value(false))
                .andExpect(jsonPath("$.contactNumber").value(nullValue()));
    }

    @Test
    void allowsDeclineAndResendWithoutAnOptionalMessage() throws Exception {
        String requesterEmail = createStudent("Decline Sender", "CON102");
        String recipientEmail = createStudent("Decline Receiver", "CON102");
        String requesterToken = login(requesterEmail);
        String recipientToken = login(recipientEmail);

        String created = mockMvc.perform(as(requesterToken, post("/api/connections"))
                        .content("{\"toStudentId\":\"" + idOf(recipientEmail) + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value(nullValue()))
                .andReturn().getResponse().getContentAsString();
        String requestId = read(created, "$.id");

        mockMvc.perform(as(recipientToken, post("/api/connections/" + requestId + "/decline")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.respondedAt").isNotEmpty())
                .andExpect(jsonPath("$.other.contactVisible").value(false));

        mockMvc.perform(as(requesterToken, post("/api/connections")
                        .content("{\"toStudentId\":\"" + idOf(recipientEmail) + "\"}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void rejectsInvalidConnectionOperations() throws Exception {
        String studentEmail = createStudent("Connection Student", "CON103");
        String otherEmail = createStudent("Connection Other", "CON103");
        String studentToken = login(studentEmail);
        String adminToken = login(createAdministrator());

        mockMvc.perform(as(studentToken, post("/api/connections")
                        .content("{\"toStudentId\":\"" + idOf(studentEmail) + "\"}")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(as(studentToken, post("/api/connections")
                        .content("{\"toStudentId\":\"" + idOf(otherEmail)
                                + "\",\"message\":\"" + "x".repeat(301) + "\"}")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(as(studentToken, get("/api/connections").param("view", "unknown")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(as(adminToken, get("/api/connections")))
                .andExpect(status().isForbidden());
    }
}
