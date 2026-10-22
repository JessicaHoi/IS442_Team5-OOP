package is442t1.studybuddy.matching;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import is442t1.studybuddy.ApiTestSupport;
import is442t1.studybuddy.connection.StudyBuddyConnection;
import is442t1.studybuddy.connection.StudyBuddyConnectionRepository;
import is442t1.studybuddy.model.enums.ConnectionStatus;
import is442t1.studybuddy.student.Student;

class MatchingApiTest extends ApiTestSupport {

    private static final String MATCHING_COURSE = "MATCH101";

    @Autowired
    private StudyBuddyConnectionRepository connectionRepository;

    @Test
    void ranksFilteredMatchesAndHidesContactUntilConnectionIsAccepted() throws Exception {
        String viewerEmail = createStudent("Aisha Rahman", MATCHING_COURSE);
        String perfectEmail = createStudent("Ben Tan", MATCHING_COURSE);
        String lowerScoreEmail = createStudent("Cara Lim", MATCHING_COURSE);
        String token = login(viewerEmail);
        savePreference(token, """
                {"preferences":[{"course":"MATCH101","meetingMode":"IN_PERSON","groupFormat":"SMALL_GROUP",
                 "goals":["EXAM_PREPARATION"],"availability":[{"day":"WED","start":"19:00","end":"21:00"}]}]}""");

        String perfectToken = login(perfectEmail);
        savePreference(perfectToken, """
                {"preferences":[{"course":"MATCH101","meetingMode":"EITHER","groupFormat":"EITHER",
                 "goals":["EXAM_PREPARATION"],"availability":[{"day":"WED","start":"19:00","end":"21:00"}]}]}""");
        String lowerScoreToken = login(lowerScoreEmail);
        savePreference(lowerScoreToken, """
                {"preferences":[{"course":"MATCH101","meetingMode":"IN_PERSON","groupFormat":"SMALL_GROUP",
                 "goals":["CONCEPT_REVIEW"],"availability":[{"day":"SAT","start":"10:00","end":"12:00"}]}]}""");

        String allMatches = mockMvc.perform(as(token, get("/api/matches").param("course", MATCHING_COURSE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].student.name").value("Ben Tan"))
                .andExpect(jsonPath("$[0].score").value(85))
                .andExpect(jsonPath("$[0].breakdown.availability").value(2.0 / 3.0))
                .andExpect(jsonPath("$[0].student.contactNumber").value(nullValue()))
                .andExpect(jsonPath("$[0].student.contactVisible").value(false))
                .andReturn().getResponse().getContentAsString();
        int perfectScore = read(allMatches, "$[0].score");
        int lowerScore = read(allMatches, "$[1].score");
        assertTrue(perfectScore > lowerScore);

        mockMvc.perform(as(token, get("/api/matches")
                        .param("course", MATCHING_COURSE)
                        .param("goal", "EXAM_PREPARATION")
                        .param("day", "WED")
                        .param("studyMode", "ONLINE")
                        .param("groupFormat", "ONE_TO_ONE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].student.name").value("Ben Tan"));

        Student viewer = studentRepository.findByEmail(viewerEmail).orElseThrow();
        Student matchedStudent = studentRepository.findByEmail(perfectEmail).orElseThrow();
        StudyBuddyConnection accepted = new StudyBuddyConnection();
        accepted.setRequester(viewer);
        accepted.setRecipient(matchedStudent);
        accepted.setStatus(ConnectionStatus.ACTIVE);
        connectionRepository.save(accepted);

        mockMvc.perform(as(token, get("/api/students/" + matchedStudent.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactVisible").value(true))
                .andExpect(jsonPath("$.contactNumber").value("+65 9123 4567"));
    }

    @Test
    void requiresStudentRoleAndRejectsInvalidFilters() throws Exception {
        String studentToken = login(createStudent("Dee Koh", "IS442"));
        String adminToken = login(createAdministrator());

        mockMvc.perform(as(adminToken, get("/api/matches")))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(studentToken, get("/api/matches").param("day", "MONDAY")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Choose a valid day for the availability filter."));
        mockMvc.perform(as(studentToken, get("/api/matches").param("goal", "NOPE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The value of 'goal' is not valid."));
    }

    private void savePreference(String token, String body) throws Exception {
        mockMvc.perform(as(token, put("/api/students/me/preferences")).content(body))
                .andExpect(status().isOk());
    }
}
