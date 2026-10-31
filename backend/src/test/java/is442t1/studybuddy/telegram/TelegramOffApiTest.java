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
    void noTelegramClientExists() {
        assertThat(context.getBeansOfType(TelegramClient.class)).isEmpty();
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
}
