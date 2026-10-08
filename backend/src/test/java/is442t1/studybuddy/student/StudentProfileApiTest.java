package is442t1.studybuddy.student;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import is442t1.studybuddy.ApiTestSupport;

class StudentProfileApiTest extends ApiTestSupport {

    private static final String IS442_PREFERENCE = """
            {"course":"IS442","meetingMode":"IN_PERSON","groupFormat":"SMALL_GROUP",
             "goals":["EXAM_PREPARATION"],"availability":[{"day":"WED","start":"19:00","end":"21:00"}]}""";
    private static final String IS212_PREFERENCE = """
            {"course":"IS212","meetingMode":"ONLINE","groupFormat":"ONE_TO_ONE",
             "goals":["CONCEPT_REVIEW","PROJECT_DISCUSSION"],
             "availability":[{"day":"TUE","start":"19:00","end":"21:00"},{"day":"SAT","start":"10:00","end":"12:00"}]}""";

    @Test
    void requiresSignInAndTheStudentRole() throws Exception {
        mockMvc.perform(get("/api/students/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Please sign in to continue."));

        String adminToken = login(createAdministrator());
        mockMvc.perform(as(adminToken, get("/api/students/me"))).andExpect(status().isForbidden());
    }

    @Test
    void updatesTheProfile() throws Exception {
        String token = login(createStudent("Aisha Rahman", "IS442"));
        ensureCourses("IS212", "STAT101");

        mockMvc.perform(as(token, put("/api/students/me/profile")).content("""
                        {"name":"  Aisha R  ","school":"School of Economics","programme":"Economics",
                         "yearOfStudy":3,"contactNumber":"+65 8000 0000","courses":["IS212","STAT101"]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Aisha R"))
                .andExpect(jsonPath("$.programme").value("Economics"))
                .andExpect(jsonPath("$.yearOfStudy").value(3))
                .andExpect(jsonPath("$.contactNumber").value("+65 8000 0000"))
                .andExpect(jsonPath("$.courses", contains("IS212", "STAT101")));
    }

    @Test
    void rejectsAnInvalidProfile() throws Exception {
        String token = login(createStudent("Ben Tan", "IS442"));

        mockMvc.perform(as(token, put("/api/students/me/profile")).content("""
                        {"name":"Ben","school":"S","programme":"P","yearOfStudy":3,
                         "contactNumber":"not a phone","courses":["IS442"]}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Enter a valid phone number, e.g. +65 9123 4567."));

        mockMvc.perform(as(token, put("/api/students/me/profile")).content("""
                        {"name":"Ben","school":"S","programme":"P","yearOfStudy":3,
                         "contactNumber":"+65 9123 4567","courses":["NOPE999"]}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void savesOnePreferencePerCourseAndDropsPreferencesForRemovedCourses() throws Exception {
        String token = login(createStudent("Cara Lim", "IS442", "IS212"));

        mockMvc.perform(as(token, put("/api/students/me/preferences"))
                        .content("{\"preferences\":[" + IS442_PREFERENCE + "," + IS212_PREFERENCE + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferences", hasSize(2)))
                .andExpect(jsonPath("$.preferences[0].course").value("IS212"))
                .andExpect(jsonPath("$.preferences[0].availability[0].day").value("TUE"))
                .andExpect(jsonPath("$.preferences[1].meetingMode").value("IN_PERSON"));

        mockMvc.perform(as(token, put("/api/students/me/profile")).content("""
                        {"name":"Cara Lim","school":"S","programme":"P","yearOfStudy":2,
                         "contactNumber":"+65 9123 4567","courses":["IS442"]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferences", hasSize(1)))
                .andExpect(jsonPath("$.preferences[0].course").value("IS442"));
    }

    @Test
    void rejectsPreferencesForCoursesNotTakenOrRepeated() throws Exception {
        String token = login(createStudent("Dan Koh", "IS442"));
        ensureCourses("IS212");

        mockMvc.perform(as(token, put("/api/students/me/preferences"))
                        .content("{\"preferences\":[" + IS212_PREFERENCE + "]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Choose one of the courses you are taking for each preference."));

        mockMvc.perform(as(token, put("/api/students/me/preferences"))
                        .content("{\"preferences\":[" + IS442_PREFERENCE + "," + IS442_PREFERENCE + "]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Each course can only have one study preference."));
    }

    @Test
    void hidesEmailAndContactNumberOnAnotherStudentsProfile() throws Exception {
        String viewerToken = login(createStudent("Eve Ng", "IS442"));
        String other = createStudent("Finn Ho", "IS442");

        mockMvc.perform(as(viewerToken, get("/api/students/" + idOf(other))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Finn Ho"))
                .andExpect(jsonPath("$.email").value(nullValue()))
                .andExpect(jsonPath("$.contactNumber").value(nullValue()))
                .andExpect(jsonPath("$.contactVisible").value(false))
                .andExpect(jsonPath("$.connectionStatus").value("NONE"));
    }
}
