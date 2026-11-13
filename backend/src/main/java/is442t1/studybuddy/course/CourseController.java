package is442t1.studybuddy.course;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import is442t1.studybuddy.auth.RequireRole;
import is442t1.studybuddy.model.enums.Role;

/**
 * Authenticated course listing and student-only course creation under /api/courses. Public signup
 *     catalogue access has its own controller.
 */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    /**
     * Lists courses sorted by code: {@code GET /api/courses}, either authenticated role.
     * Returns HTTP 200 with code, name and optional school for each course.
     *
     * @return response described above, with the documented HTTP status and payload
     */
    @GetMapping
    public List<CourseResponse> listCourses() {
        return courseService.listCourses();
    }

    /**
     * Adds a shared catalogue course: {@code POST /api/courses}, student only.
     * Example body: {@code {"courseCode":"IS442","courseName":"Object-Oriented Programming"}}.
     * Returns HTTP 201; invalid fields are 400, an administrator caller is 403 and duplicate code
     *     is 409.
     *
     * @param request JSON body described above
     * @return response described above, with the documented HTTP status and payload
     */
    @PostMapping
    @RequireRole(Role.STUDENT)
    public ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CreateCourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.createCourse(request));
    }
}
