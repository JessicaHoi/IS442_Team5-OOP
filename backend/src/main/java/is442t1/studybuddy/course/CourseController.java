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

/** Authenticated course catalogue endpoints. */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    /**
     * Creates the course controller.
     *
     * @param courseService the course catalogue service
     */
    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    /**
     * Lists the course catalogue.
     *
     * @return courses ordered by code
     */
    @GetMapping
    public List<CourseResponse> listCourses() {
        return courseService.listCourses();
    }

    /**
     * Adds a course to the catalogue.
     *
     * @param request the new course details
     * @return the created course with HTTP 201
     */
    @PostMapping
    @RequireRole(Role.STUDENT)
    public ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CreateCourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.createCourse(request));
    }
}
