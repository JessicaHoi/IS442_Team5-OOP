package is442t1.studybuddy.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import is442t1.studybuddy.ApiTestSupport;
import is442t1.studybuddy.telegram.api.TelegramClient;

/** The default: with Telegram off, nothing reaches Telegram and the endpoints say it's off. */
class TelegramOffApiTest extends ApiTestSupport {

    @Autowired private ApplicationContext context;

    @Test
    void onlyTheDisabledClientExists() {
        assertThat(context.getBeansOfType(TelegramClient.class).values())
                .singleElement().satisfies(client -> assertThat(client.getClass().getSimpleName()).isEqualTo("DisabledTelegramClient"));
    }

    @Test
    void anySignedInUserCanSeeThatTelegramIsOff() throws Exception {
        for (String token : new String[] {login(createStudent("Off Student")), login(createAdministrator())}) {
            mockMvc.perform(as(token, get("/api/integrations")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.telegram.enabled").value(false))
                    .andExpect(jsonPath("$.telegram.botUsername").doesNotExist());
        }
    }

    @Test
    void integrationsNeedASignIn() throws Exception {
        mockMvc.perform(get("/api/integrations")).andExpect(status().isUnauthorized());
    }

    @Test
    void aStudentIsNotLinkedAndCannotAskForALink() throws Exception {
        String token = login(createStudent("Off Linker"));

        mockMvc.perform(as(token, get("/api/students/me/telegram")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linked").value(false));
        mockMvc.perform(as(token, post("/api/students/me/telegram/link")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Telegram isn't set up on this StudyBuddy server."));
        mockMvc.perform(as(token, delete("/api/students/me/telegram"))).andExpect(status().isNoContent());
    }

    @Test
    void administratorsCannotUseTheStudentEndpoints() throws Exception {
        String token = login(createAdministrator());

        mockMvc.perform(as(token, get("/api/students/me/telegram"))).andExpect(status().isForbidden());
        mockMvc.perform(as(token, post("/api/students/me/telegram/link"))).andExpect(status().isForbidden());
        mockMvc.perform(as(token, delete("/api/students/me/telegram"))).andExpect(status().isForbidden());
    }

    @Test
    void groupChatEndpointsSayItIsOff() throws Exception {
        String token = login(createStudent("Off Leader", "IS442"));
        String groupId = read(mockMvc.perform(as(token, post("/api/groups")).content("""
                        {"courseCode":"IS442","name":"Off Crew","description":"x","goals":["EXAM_PREPARATION"],
                         "meetingMode":"ONLINE","maxSize":4,"availability":[{"day":"TUE","start":"19:00","end":"21:00"}]}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(as(token, get("/api/groups/" + groupId + "/chat")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_LINKED"));
        mockMvc.perform(as(token, post("/api/groups/" + groupId + "/chat/setup"))).andExpect(status().isConflict());
        mockMvc.perform(as(token, post("/api/groups/" + groupId + "/chat/invite"))).andExpect(status().isConflict());
        mockMvc.perform(as(token, post("/api/groups/" + groupId + "/chat/verify"))).andExpect(status().isConflict());
        mockMvc.perform(as(token, delete("/api/groups/" + groupId + "/chat"))).andExpect(status().isConflict());
    }
}
