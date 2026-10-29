package is442t1.studybuddy.matching;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import is442t1.studybuddy.ApiTestSupport;

class MatchingConfigApiTest extends ApiTestSupport {

    private static final String COURSE = "AVAIL101";
    private static final String COURSE_FIRST_COURSE = "CFIRST101";
    private static final String OTHER_COURSE = "CFIRST202";

    @Autowired
    private MatchingConfigurationRepository configurationRepository;

    /** Other tests expect the default weights, so drop whatever this test saved. */
    @AfterEach
    void restoreDefaults() {
        configurationRepository.deleteAll();
    }

    @Test
    void adminReadsAndValidatesConfiguration() throws Exception {
        String adminToken = login(createAdministrator());
        String studentToken = login(createStudent("Eve Ong", COURSE));

        mockMvc.perform(as(adminToken, get("/api/admin/matching-config")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy").value("BALANCED"))
                .andExpect(jsonPath("$.criteria", hasSize(5)))
                .andExpect(jsonPath("$.criteria[0].key").value("course"))
                .andExpect(jsonPath("$.criteria[0].weight").value(30))
                .andExpect(jsonPath("$.criteria[0].enabled").value(true));

        mockMvc.perform(as(studentToken, get("/api/admin/matching-config")))
                .andExpect(status().isForbidden());

        mockMvc.perform(as(adminToken, put("/api/admin/matching-config"))
                        .content(config("BALANCED", false, false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Enable at least one criterion with a weight above 0."));

        mockMvc.perform(as(adminToken, put("/api/admin/matching-config"))
                        .content(config("COURSE_FIRST", true, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy").value("COURSE_FIRST"));

        mockMvc.perform(as(adminToken, get("/api/admin/matching-config")))
                .andExpect(jsonPath("$.strategy").value("COURSE_FIRST"));
    }

    @Test
    void courseFirstRanksCourseAlignmentBeforeOverallScore() throws Exception {
        String adminToken = login(createAdministrator());
        String viewerToken = login(createStudent("Ivy Tan", COURSE_FIRST_COURSE));
        String sameCourseToken = login(createStudent("Jon Lim", COURSE_FIRST_COURSE));
        String sameHabitsToken = login(createStudent("Kim Ng", COURSE_FIRST_COURSE, OTHER_COURSE));

        savePreference(viewerToken, COURSE_FIRST_COURSE, "IN_PERSON", "SMALL_GROUP", "EXAM_PREPARATION",
                "WED", "19:00", "21:00");
        // Wants a buddy for the searched course but nothing else lines up: course 1, score 30.
        savePreference(sameCourseToken, COURSE_FIRST_COURSE, "ONLINE", "ONE_TO_ONE", "CONCEPT_REVIEW",
                "SAT", "10:00", "12:00");
        // Takes the course but wants a buddy for another one, with identical habits: course 0.75, score 84.
        savePreference(sameHabitsToken, OTHER_COURSE, "IN_PERSON", "SMALL_GROUP", "EXAM_PREPARATION",
                "WED", "19:00", "21:00");

        mockMvc.perform(as(viewerToken, get("/api/matches").param("course", COURSE_FIRST_COURSE)))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].student.name").value("Kim Ng"))
                .andExpect(jsonPath("$[0].score").value(84))
                .andExpect(jsonPath("$[1].student.name").value("Jon Lim"))
                .andExpect(jsonPath("$[1].score").value(30));

        mockMvc.perform(as(adminToken, put("/api/admin/matching-config"))
                        .content(config("COURSE_FIRST", true, true)))
                .andExpect(status().isOk());

        // Scores are unchanged; only the order follows course alignment.
        mockMvc.perform(as(viewerToken, get("/api/matches").param("course", COURSE_FIRST_COURSE)))
                .andExpect(jsonPath("$[0].student.name").value("Jon Lim"))
                .andExpect(jsonPath("$[0].score").value(30))
                .andExpect(jsonPath("$[0].breakdown.course").value(1.0))
                .andExpect(jsonPath("$[1].student.name").value("Kim Ng"))
                .andExpect(jsonPath("$[1].score").value(84))
                .andExpect(jsonPath("$[1].breakdown.course").value(0.75));
    }

    @Test
    void strategyAndWeightsChangeTheRanking() throws Exception {
        String adminToken = login(createAdministrator());
        String viewerToken = login(createStudent("Fay Goh", COURSE));
        String sameGoalsToken = login(createStudent("Gus Lee", COURSE));
        String sameTimeToken = login(createStudent("Hana Yeo", COURSE));

        savePreference(viewerToken, "IN_PERSON", "SMALL_GROUP", "EXAM_PREPARATION", "WED", "19:00", "21:00");
        // Same preferences but no shared time slot: score 75, availability 0.
        savePreference(sameGoalsToken, "IN_PERSON", "SMALL_GROUP", "EXAM_PREPARATION", "SAT", "10:00", "12:00");
        // Different preferences but a 2-hour overlap: score 47, availability 2/3.
        savePreference(sameTimeToken, "ONLINE", "ONE_TO_ONE", "CONCEPT_REVIEW", "WED", "19:00", "21:00");

        mockMvc.perform(as(viewerToken, get("/api/matches").param("course", COURSE)))
                .andExpect(jsonPath("$[0].student.name").value("Gus Lee"))
                .andExpect(jsonPath("$[0].score").value(75))
                .andExpect(jsonPath("$[1].student.name").value("Hana Yeo"))
                .andExpect(jsonPath("$[1].score").value(47));

        mockMvc.perform(as(adminToken, put("/api/admin/matching-config"))
                        .content(config("AVAILABILITY_FIRST", true, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy").value("AVAILABILITY_FIRST"));

        mockMvc.perform(as(viewerToken, get("/api/matches").param("course", COURSE)))
                .andExpect(jsonPath("$[0].student.name").value("Hana Yeo"))
                .andExpect(jsonPath("$[0].score").value(47))
                .andExpect(jsonPath("$[1].student.name").value("Gus Lee"));

        // Balanced with only availability switched on: the score is the overlap alone.
        mockMvc.perform(as(adminToken, put("/api/admin/matching-config"))
                        .content(config("BALANCED", false, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.criteria[0].enabled").value(false))
                .andExpect(jsonPath("$.criteria[0].weight").value(0));

        mockMvc.perform(as(viewerToken, get("/api/matches").param("course", COURSE)))
                .andExpect(jsonPath("$[0].student.name").value("Hana Yeo"))
                .andExpect(jsonPath("$[0].score").value(67))
                .andExpect(jsonPath("$[1].score").value(0));
    }

    /** Builds a request body; {@code othersEnabled} covers every criterion except availability. */
    private static String config(String strategy, boolean othersEnabled, boolean availabilityEnabled) {
        return """
                {"strategy":"%s","criteria":[
                 {"key":"course","enabled":%s,"weight":30},
                 {"key":"availability","enabled":%s,"weight":25},
                 {"key":"studyMode","enabled":%s,"weight":15},
                 {"key":"studyGoal","enabled":%s,"weight":20},
                 {"key":"groupSize","enabled":%s,"weight":10}]}"""
                .formatted(strategy, othersEnabled, availabilityEnabled, othersEnabled, othersEnabled, othersEnabled);
    }

    private void savePreference(String token, String meetingMode, String groupFormat, String goal,
            String day, String start, String end) throws Exception {
        savePreference(token, COURSE, meetingMode, groupFormat, goal, day, start, end);
    }

    private void savePreference(String token, String course, String meetingMode, String groupFormat,
            String goal, String day, String start, String end) throws Exception {
        String body = """
                {"preferences":[{"course":"%s","meetingMode":"%s","groupFormat":"%s","goals":["%s"],
                 "availability":[{"day":"%s","start":"%s","end":"%s"}]}]}"""
                .formatted(course, meetingMode, groupFormat, goal, day, start, end);
        mockMvc.perform(as(token, put("/api/students/me/preferences")).content(body))
                .andExpect(status().isOk());
    }
}
