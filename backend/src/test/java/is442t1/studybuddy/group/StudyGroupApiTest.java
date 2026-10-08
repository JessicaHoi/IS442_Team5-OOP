package is442t1.studybuddy.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import is442t1.studybuddy.ApiTestSupport;

class StudyGroupApiTest extends ApiTestSupport {

    @Autowired
    private StudyGroupLeaderRepository leaderRepository;

    private String leaderEmail;
    private String leaderToken;

    @BeforeEach
    void createLeader() throws Exception {
        leaderEmail = createStudent("Leah Leader", "IS442");
        leaderToken = login(leaderEmail);
    }

    @Test
    void creatingAGroupPromotesTheStudentToLeader() throws Exception {
        assertThat(leaderRepository.existsById(idOf(leaderEmail))).isFalse();

        mockMvc.perform(as(leaderToken, post("/api/groups")).content(groupJson("IS442", "Exam Crew", 3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Exam Crew"))
                .andExpect(jsonPath("$.courseCode").value("IS442"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.myRole").value("LEADER"))
                .andExpect(jsonPath("$.memberCount").value(1))
                .andExpect(jsonPath("$.members[0].isLeader").value(true));

        assertThat(leaderRepository.existsById(idOf(leaderEmail))).isTrue();
        mockMvc.perform(as(leaderToken, get("/api/students/me"))).andExpect(status().isOk());

        // A second group reuses the existing leader.
        mockMvc.perform(as(leaderToken, post("/api/groups")).content(groupJson("IS442", "Second", 3)))
                .andExpect(status().isCreated());
        mockMvc.perform(as(leaderToken, get("/api/groups/mine"))).andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void validatesNewGroups() throws Exception {
        mockMvc.perform(as(leaderToken, post("/api/groups")).content(groupJson("IS442", "Too big", 99)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Maximum size must be between 2 and 20."));
        mockMvc.perform(as(leaderToken, post("/api/groups")).content(groupJson("NOPE999", "Bad course", 3)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(as(leaderToken, post("/api/groups")).content(groupJson("IS442", "", 3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Give your group a name."));
    }

    @Test
    void leaderManagesJoinRequestsAndMembers() throws Exception {
        String groupId = createGroup(2);
        String studentEmail = createStudent("Sam Student", "IS442");
        String studentToken = login(studentEmail);
        String latecomerToken = login(createStudent("Lia Late", "IS442"));

        // Students ask to join; the leader sees both requests.
        mockMvc.perform(as(studentToken, post("/api/groups/" + groupId + "/join-requests")).content("{\"message\":\"Hi!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.myRequestStatus").value("PENDING"));
        mockMvc.perform(as(latecomerToken, post("/api/groups/" + groupId + "/join-requests")).content("{}"))
                .andExpect(status().isCreated());
        String requests = mockMvc.perform(as(leaderToken, get("/api/groups/" + groupId + "/join-requests")))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].student.name").value("Sam Student"))
                .andExpect(jsonPath("$[0].message").value("Hi!"))
                .andReturn().getResponse().getContentAsString();
        String firstRequest = read(requests, "$[0].id");
        String secondRequest = read(requests, "$[1].id");

        // Accepting fills the group (max 2), so the second request cannot be accepted.
        mockMvc.perform(as(leaderToken, post("/api/groups/" + groupId + "/join-requests/" + firstRequest + "/accept")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberCount").value(2));
        mockMvc.perform(as(leaderToken, post("/api/groups/" + groupId + "/join-requests/" + secondRequest + "/accept")))
                .andExpect(status().isConflict());
        mockMvc.perform(as(leaderToken, post("/api/groups/" + groupId + "/join-requests/" + secondRequest + "/reject")))
                .andExpect(status().isOk());
        mockMvc.perform(as(leaderToken, get("/api/groups/" + groupId + "/join-requests")))
                .andExpect(jsonPath("$", hasSize(0)));

        // The leader removes the member, but cannot remove themselves.
        mockMvc.perform(as(leaderToken, delete("/api/groups/" + groupId + "/members/" + idOf(studentEmail))))
                .andExpect(status().isNoContent());
        mockMvc.perform(as(leaderToken, delete("/api/groups/" + groupId + "/members/" + idOf(leaderEmail))))
                .andExpect(status().isConflict());
        mockMvc.perform(as(leaderToken, get("/api/groups/" + groupId)))
                .andExpect(jsonPath("$.memberCount").value(1));
    }

    @Test
    void onlyTheLeaderCanManageTheGroup() throws Exception {
        String groupId = createGroup(5);
        String otherToken = login(createStudent("Olly Other", "IS442"));

        mockMvc.perform(as(otherToken, get("/api/groups/" + groupId + "/join-requests"))).andExpect(status().isForbidden());
        mockMvc.perform(as(otherToken, put("/api/groups/" + groupId)).content(groupJson(null, "Hijack", 5)))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(otherToken, post("/api/groups/" + groupId + "/close"))).andExpect(status().isForbidden());
        mockMvc.perform(as(otherToken, delete("/api/groups/" + groupId + "/members/" + idOf(leaderEmail))))
                .andExpect(status().isForbidden());
        // Non-members see the group but not its member list.
        mockMvc.perform(as(otherToken, get("/api/groups/" + groupId)))
                .andExpect(jsonPath("$.myRole").value("NONE"))
                .andExpect(jsonPath("$.members", hasSize(0)));
    }

    @Test
    void leaderUpdatesThenClosesTheGroup() throws Exception {
        String groupId = createGroup(5);
        String studentToken = login(createStudent("Pat Pending", "IS442"));
        mockMvc.perform(as(studentToken, post("/api/groups/" + groupId + "/join-requests")).content("{}"))
                .andExpect(status().isCreated());

        mockMvc.perform(as(leaderToken, put("/api/groups/" + groupId)).content(groupJson("STAT101", "Renamed", 6)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"))
                .andExpect(jsonPath("$.courseCode").value("IS442"))
                .andExpect(jsonPath("$.maxSize").value(6));

        mockMvc.perform(as(leaderToken, post("/api/groups/" + groupId + "/close")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
        mockMvc.perform(as(leaderToken, get("/api/groups/" + groupId + "/join-requests")))
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(as(studentToken, get("/api/groups/" + groupId)))
                .andExpect(jsonPath("$.myRequestStatus").value("NONE"));
        mockMvc.perform(as(leaderToken, put("/api/groups/" + groupId)).content(groupJson(null, "Again", 6)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This group is closed."));
        mockMvc.perform(as(studentToken, get("/api/groups?course=IS442")))
                .andExpect(jsonPath("$[?(@.id == '" + groupId + "')]", hasSize(0)));
        mockMvc.perform(as(leaderToken, get("/api/groups/mine")))
                .andExpect(jsonPath("$[0].status").value("CLOSED"));
    }

    private String createGroup(int maxSize) throws Exception {
        String body = mockMvc.perform(as(leaderToken, post("/api/groups")).content(groupJson("IS442", "Crew", maxSize)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return read(body, "$.id");
    }

    private static String groupJson(String courseCode, String name, int maxSize) {
        String course = courseCode == null ? "null" : "\"" + courseCode + "\"";
        return """
                {"courseCode":%s,"name":"%s","description":"Weekly practice","goals":["EXAM_PREPARATION"],
                 "meetingMode":"IN_PERSON","availability":[{"day":"WED","start":"19:00","end":"21:00"}],"maxSize":%d}"""
                .formatted(course, name, maxSize);
    }
}
