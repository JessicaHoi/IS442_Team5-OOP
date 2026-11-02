package is442t1.studybuddy.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import is442t1.studybuddy.ApiTestSupport;

class CourseCreationApiTest extends ApiTestSupport {

    @Autowired
    private CourseRepository courses;

    @Test
    void allowsStudentsToAddCoursesToTheSharedCatalogue() throws Exception {
        String courseCode = ("N" + UUID.randomUUID().toString().replace("-", "")).substring(0, 12).toUpperCase();
        String studentToken = login(createStudent("Course Creator", "IS442"));
        mockMvc.perform(as(studentToken, post("/api/courses"))
                        .content("""
                                {"courseCode":"  %s ","courseName":" New Course "}
                                """.formatted(courseCode.toLowerCase())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(courseCode))
                .andExpect(jsonPath("$.name").value("New Course"));
        assertThat(courses.findById(courseCode)).isPresent();

        mockMvc.perform(as(studentToken, post("/api/courses"))
                        .content("""
                                {"courseCode":"%s","courseName":"Duplicate"}
                                """.formatted(courseCode.toLowerCase())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A course with this code already exists."));
    }

    @Test
    void onlyStudentsCanAddAValidCourse() throws Exception {
        String adminToken = login(createAdministrator());
        mockMvc.perform(as(adminToken, post("/api/courses"))
                        .content("""
                                {"courseCode":"NEW102","courseName":"New Course"}
                                """))
                .andExpect(status().isForbidden());

        String studentToken = login(createStudent("Invalid Course Creator", "IS442"));
        mockMvc.perform(as(studentToken, post("/api/courses"))
                        .content("""
                                {"courseCode":"NOT VALID","courseName":"New Course"}
                                """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/courses").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseCode":"NEW103","courseName":"New Course"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
