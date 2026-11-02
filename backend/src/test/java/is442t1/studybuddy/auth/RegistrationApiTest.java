package is442t1.studybuddy.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import is442t1.studybuddy.ApiTestSupport;
import is442t1.studybuddy.course.CourseRepository;
import is442t1.studybuddy.model.UserRepository;

class RegistrationApiTest extends ApiTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    void registersAStudentWithACompleteProfileAndReturnsAnAuthenticatedSession() throws Exception {
        ensureCourses("IS442");
        String email = "signup." + UUID.randomUUID() + "@example.com";
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.name").value("New Student"))
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.role").value("STUDENT"))
                .andReturn().getResponse().getContentAsString();

        String token = read(response, "$.token");
        mockMvc.perform(as(token, get("/api/students/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Student"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.school").value("School of Computing and Information Systems"))
                .andExpect(jsonPath("$.programme").value("Information Systems"))
                .andExpect(jsonPath("$.yearOfStudy").value(2))
                .andExpect(jsonPath("$.contactNumber").value("+65 9123 4567"))
                .andExpect(jsonPath("$.courses[0]").value("IS442"));

        var account = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(passwordEncoder.matches("secret-password", account.getPassword())).isTrue();
        assertThat(account.getLastLogin()).isNotNull();
    }

    @Test
    void rejectsDuplicateEmailsAndInvalidProfileDetails() throws Exception {
        String existingEmail = createStudent("Existing Signup", "IS442");
        ensureCourses("IS442");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration(existingEmail.toUpperCase())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Another account already uses this email address."));

        String invalidEmail = "invalid." + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration(invalidEmail).replace("+65 9123 4567", "not a phone")))
                .andExpect(status().isBadRequest());
        assertThat(userRepository.findByEmailIgnoreCase(invalidEmail)).isEmpty();
    }

    @Test
    void makesCourseCatalogueAvailableBeforeSignIn() throws Exception {
        mockMvc.perform(get("/api/public/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").isNotEmpty());
        mockMvc.perform(get("/api/courses")).andExpect(status().isUnauthorized());
    }

    @Test
    void signupCanCreateACourseAndSelectItInTheSameRequest() throws Exception {
        String email = "signup.course." + UUID.randomUUID() + "@example.com";
        String courseCode = ("N" + UUID.randomUUID().toString().replace("-", "")).substring(0, 12).toUpperCase();
        String body = registration(email).replace(
                "\"courses\":[\"IS442\"]",
                "\"courses\":[\"" + courseCode + "\"],\"newCourses\":[{\"courseCode\":\"" + courseCode.toLowerCase()
                        + "\",\"courseName\":\"New Course\"}]");

        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(as(read(response, "$.token"), get("/api/students/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courses[0]").value(courseCode));
        assertThat(courseRepository.findById(courseCode)).isPresent();
    }

    private static String registration(String email) {
        return """
                {"name":"New Student","email":"%s","password":"secret-password",
                 "school":"School of Computing and Information Systems","programme":"Information Systems",
                 "yearOfStudy":2,"contactNumber":"+65 9123 4567","courses":["IS442"]}
                """.formatted(email);
    }
}
