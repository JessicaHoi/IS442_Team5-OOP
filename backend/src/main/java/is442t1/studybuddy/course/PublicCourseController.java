package is442t1.studybuddy.course;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public course catalogue used to populate the student signup form. */
@RestController
@RequestMapping("/api/public/courses")
public class PublicCourseController {

    private final CourseService courseService;

    /**
     * Creates the public course controller.
     *
     * @param courseService the course catalogue service
     */
    public PublicCourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    /**
     * Lists the public course catalogue.
     *
     * @return courses ordered by code
     */
    @GetMapping
    public List<CourseResponse> listCourses() {
        return courseService.listCourses();
    }
}
