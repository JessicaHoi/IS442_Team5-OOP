package is442t1.studybuddy.matching.preview;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import is442t1.studybuddy.ApiTestSupport;

class MatchingPreviewApiTest extends ApiTestSupport {

    private static final String COURSE = "PREV101";

    @Test
    void comparesSavedAndProposedRankingsForTheSameStudent() throws Exception {
        String adminToken = login(createAdministrator());
        String viewerEmail = createStudent("Ivy Chua", COURSE);
        String viewerToken = login(viewerEmail);
        savePreference(viewerToken, "IN_PERSON", "SMALL_GROUP", "EXAM_PREPARATION", "WED");
        // Same preferences, no shared time: wins under Balanced.
        savePreference(login(createStudent("Jon Ang", COURSE)), "IN_PERSON", "SMALL_GROUP", "EXAM_PREPARATION", "SAT");
        // Different preferences, same time slot: wins under Availability-First.
        savePreference(login(createStudent("Kai Seah", COURSE)), "ONLINE", "ONE_TO_ONE", "CONCEPT_REVIEW", "WED");

        mockMvc.perform(as(adminToken, get("/api/admin/matching-config/preview/students")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Ivy Chua')].courses[0]").value(COURSE));

        String body = """
                {"studentId":"%s","course":"%s","proposed":%s}""".formatted(idOf(viewerEmail), COURSE,
                config("AVAILABILITY_FIRST"));
        mockMvc.perform(as(adminToken, post("/api/admin/matching-config/preview")).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentName").value("Ivy Chua"))
                .andExpect(jsonPath("$.current.strategy").value("BALANCED"))
                .andExpect(jsonPath("$.current.results[0].name").value("Jon Ang"))
                .andExpect(jsonPath("$.proposed.strategy").value("AVAILABILITY_FIRST"))
                .andExpect(jsonPath("$.proposed.results[0].name").value("Kai Seah"))
                .andExpect(jsonPath("$.proposed.results[0].otherRank").value(2))
                .andExpect(jsonPath("$.proposed.results[1].name").value("Jon Ang"));

        // The preview never saves anything.
        mockMvc.perform(as(adminToken, get("/api/admin/matching-config")))
                .andExpect(jsonPath("$.strategy").value("BALANCED"));
    }

    @Test
    void rejectsStudentsAndInvalidSettings() throws Exception {
        String adminToken = login(createAdministrator());
        String studentEmail = createStudent("Lea Toh", COURSE);
        String body = """
                {"studentId":"%s","proposed":%s}""";

        mockMvc.perform(as(login(studentEmail), get("/api/admin/matching-config/preview/students")))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(adminToken, post("/api/admin/matching-config/preview"))
                        .content(body.formatted(idOf(studentEmail), config("COURSE_FIRST"))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(as(adminToken, post("/api/admin/matching-config/preview"))
                        .content(body.formatted("00000000-0000-0000-0000-000000000000", config("BALANCED"))))
                .andExpect(status().isNotFound());
    }

    private static String config(String strategy) {
        return """
                {"strategy":"%s","criteria":[{"key":"course","enabled":true,"weight":30},
                 {"key":"availability","enabled":true,"weight":25},{"key":"studyMode","enabled":true,"weight":15},
                 {"key":"studyGoal","enabled":true,"weight":20},{"key":"groupSize","enabled":true,"weight":10}]}"""
                .formatted(strategy);
    }

    private void savePreference(String token, String meetingMode, String groupFormat, String goal, String day)
            throws Exception {
        String body = """
                {"preferences":[{"course":"%s","meetingMode":"%s","groupFormat":"%s","goals":["%s"],
                 "availability":[{"day":"%s","start":"19:00","end":"21:00"}]}]}"""
                .formatted(COURSE, meetingMode, groupFormat, goal, day);
        mockMvc.perform(as(token, put("/api/students/me/preferences")).content(body))
                .andExpect(status().isOk());
    }
}
