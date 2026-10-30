package is442t1.studybuddy.group;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import is442t1.studybuddy.ApiTestSupport;

class GroupChatLinkApiTest extends ApiTestSupport {

    private static final String LINK = "https://t.me/+AbC_123-xyz";

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

    private void setLink() throws Exception {
        mockMvc.perform(as(leaderToken, put(path("/chat-link"))).content("{\"url\":\"" + LINK + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chatLink").value(LINK));
    }

    @Test
    void onlyMembersSeeTheChatLink() throws Exception {
        setLink();

        mockMvc.perform(as(memberToken, get(path("")))).andExpect(jsonPath("$.chatLink").value(LINK));
        mockMvc.perform(as(strangerToken, get(path("")))).andExpect(jsonPath("$.chatLink").doesNotExist());
        mockMvc.perform(as(memberToken, get("/api/groups/mine"))).andExpect(jsonPath("$[0].chatLink").doesNotExist());
        mockMvc.perform(as(strangerToken, get("/api/groups"))).andExpect(jsonPath("$[?(@.chatLink)]", hasSize(0)));
    }

    @Test
    void onlyTheLeaderCanChangeTheChatLinkWhateverTheySend() throws Exception {
        mockMvc.perform(as(memberToken, put(path("/chat-link"))).content("{\"url\":\"nonsense\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(strangerToken, delete(path("/chat-link")))).andExpect(status().isForbidden());
    }

    @Test
    void rejectsALinkThatIsNotTelegram() throws Exception {
        for (String body : new String[] {"{\"url\":\"https://evil.example/x\"}", "{\"url\":\"http://t.me/x\"}", "{\"url\":\"javascript:alert(1)\"}", "{}"}) {
            mockMvc.perform(as(leaderToken, put(path("/chat-link"))).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Enter a Telegram link that starts with https://t.me/."));
        }
    }

    @Test
    void savingGroupDetailsKeepsTheChatLinkAndClearingRemovesIt() throws Exception {
        setLink();
        mockMvc.perform(as(leaderToken, put(path(""))).content("""
                        {"name":"Renamed","description":"Weekly practice","goals":["EXAM_PREPARATION"],
                         "meetingMode":"ONLINE","maxSize":4,"availability":[{"day":"TUE","start":"19:00","end":"21:00"}]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chatLink").value(LINK));

        mockMvc.perform(as(leaderToken, delete(path("/chat-link")))).andExpect(status().isNoContent());
        mockMvc.perform(as(memberToken, get(path("")))).andExpect(jsonPath("$.chatLink").doesNotExist());
    }

    @Test
    void theChatLinkOfAClosedGroupCannotChange() throws Exception {
        mockMvc.perform(as(leaderToken, post(path("/close")))).andExpect(status().isOk());

        mockMvc.perform(as(leaderToken, put(path("/chat-link"))).content("{\"url\":\"" + LINK + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void aMemberWhoLeavesLosesAccessToTheChatLink() throws Exception {
        setLink();

        mockMvc.perform(as(memberToken, post(path("/leave")))).andExpect(status().isNoContent());

        mockMvc.perform(as(memberToken, get(path("")))).andExpect(jsonPath("$.chatLink").doesNotExist());
    }
}
