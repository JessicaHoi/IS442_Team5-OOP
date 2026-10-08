package is442t1.studybuddy;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import is442t1.studybuddy.admin.SystemAdministrator;
import is442t1.studybuddy.course.Course;
import is442t1.studybuddy.course.CourseRepository;
import is442t1.studybuddy.model.UserRepository;
import is442t1.studybuddy.student.Student;
import is442t1.studybuddy.student.StudentRepository;

/**
 * Base class for API tests: a full application on an in-memory database,
 * with helpers to create accounts and sign in. Each test makes its own
 * accounts (unique emails), so tests do not depend on each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ApiTestSupport {

    protected static final String PASSWORD = "secret-password";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    protected void ensureCourses(String... codes) {
        for (String code : codes) {
            if (!courseRepository.existsById(code)) {
                Course course = new Course();
                course.setCourseCode(code);
                course.setCourseName("Course " + code);
                courseRepository.save(course);
            }
        }
    }

    /** Creates a student taking the given courses and returns their email. */
    protected String createStudent(String name, String... courseCodes) {
        ensureCourses(courseCodes);
        Student student = new Student();
        student.setEmail(uniqueEmail(name));
        student.setPassword(passwordEncoder.encode(PASSWORD));
        student.setName(name);
        student.setSchool("School of Computing and Information Systems");
        student.setProgram("Information Systems");
        student.setYearOfStudy(2);
        student.setContactNum("+65 9123 4567");
        student.replaceCoursesTaken(courseRepository.findAllById(List.of(courseCodes)));
        return studentRepository.save(student).getEmail();
    }

    protected String createAdministrator() {
        SystemAdministrator administrator = new SystemAdministrator();
        administrator.setEmail(uniqueEmail("admin"));
        administrator.setPassword(passwordEncoder.encode(PASSWORD));
        return userRepository.save(administrator).getEmail();
    }

    protected UUID idOf(String email) {
        return userRepository.findByEmailIgnoreCase(email).orElseThrow().getId();
    }

    protected String login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    /** Adds the bearer token and JSON content type to a request. */
    protected static MockHttpServletRequestBuilder as(String token, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    protected static <T> T read(String json, String path) {
        return JsonPath.read(json, path);
    }

    private static String uniqueEmail(String name) {
        return name.toLowerCase().replace(' ', '.') + "." + UUID.randomUUID() + "@test.smu.edu.sg";
    }
}
