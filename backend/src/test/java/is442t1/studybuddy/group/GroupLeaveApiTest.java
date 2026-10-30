package is442t1.studybuddy.group;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import is442t1.studybuddy.ApiTestSupport;

class GroupLeaveApiTest extends ApiTestSupport {

    private String leaderToken;
    private String memberToken;
    private String strangerToken;
    private String groupId;

    @BeforeEach
    void createGroupWithAMember() throws Exception {
        leaderToken = login(createStudent("Leah Leader", "IS442"));
        memberToken = login(createStudent("Mia Member", "IS442"));
        strangerToken = login(createStudent("Sean Stranger", "IS442"));

        groupId = read(mockMvc.perform(as(leaderToken, post("/api/groups")).content("""
                        {"courseCode":"IS442","name":"Exam Crew","description":"Weekly practice",
                         "goals":["EXAM_PREPARATION"],"meetingMode":"ONLINE","maxSize":4,
                         "availability":[{"day":"TUE","start":"19:00","end":"21:00"}]}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
        mockMvc.perform(as(memberToken, post(path("/join-requests"))).content("{}"))
                .andExpect(status().isCreated());
        String pending = read(mockMvc.perform(as(leaderToken, get(path("/join-requests"))))
                .andReturn().getResponse().getContentAsString(), "$[0].id");
        mockMvc.perform(as(leaderToken, post(path("/join-requests/" + pending + "/accept"))))
                .andExpect(status().isOk());
    }

    private String path(String suffix) {
        return "/api/groups/" + groupId + suffix;
    }

    @Test
    void aMemberCanLeaveAndAskToJoinAgain() throws Exception {
        mockMvc.perform(as(memberToken, post(path("/leave")))).andExpect(status().isNoContent());

        mockMvc.perform(as(memberToken, get(path(""))))
                .andExpect(jsonPath("$.myRole").value("NONE"));
        mockMvc.perform(as(leaderToken, get(path(""))))
                .andExpect(jsonPath("$.memberCount").value(1))
                .andExpect(jsonPath("$.members", hasSize(1)));
        mockMvc.perform(as(memberToken, post(path("/join-requests"))).content("{}")).andExpect(status().isCreated());
    }

    @Test
    void theLeaderAndNonMembersCannotLeave() throws Exception {
        mockMvc.perform(as(leaderToken, post(path("/leave"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The group leader cannot leave. Transfer leadership or close the group first."));
        mockMvc.perform(as(strangerToken, post(path("/leave"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You are not a member of this group."));
    }

    @Test
    void nobodyCanLeaveAClosedGroup() throws Exception {
        mockMvc.perform(as(leaderToken, post(path("/close")))).andExpect(status().isOk());

        mockMvc.perform(as(memberToken, post(path("/leave")))).andExpect(status().isConflict());
    }

    @Test
    void leavingRemovesTheMembersGroupAvailability() throws Exception {
        mockMvc.perform(as(memberToken, put(path("/availability/me"))).content("""
                        {"availability":[{"day":"MON","start":"10:00","end":"11:00"}]}"""))
                .andExpect(status().isOk());

        mockMvc.perform(as(memberToken, post(path("/leave")))).andExpect(status().isNoContent());

        mockMvc.perform(as(leaderToken, get(path("/best-times")))).andExpect(jsonPath("$.memberCount").value(1));
    }
}
