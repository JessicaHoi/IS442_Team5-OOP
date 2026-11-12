package is442t1.studybuddy.course;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import is442t1.studybuddy.common.exception.BusinessRuleException;
import is442t1.studybuddy.common.exception.ValidationException;

/** Maintains and resolves courses in the shared catalogue. */
@Service
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;

    /**
     * Creates the course service.
     *
     * @param courseRepository the course repository
     */
    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    /**
     * Lists courses in code order.
     *
     * @return the course responses
     */
    public List<CourseResponse> listCourses() {
        return courseRepository.findAll(Sort.by("courseCode")).stream().map(CourseResponse::from).toList();
    }

    /**
     * Adds a course to the catalogue.
     *
     * @param request the new course details
     * @return the created course
     * @throws BusinessRuleException if the course code already exists
     */
    @Transactional
    public CourseResponse createCourse(CreateCourseRequest request) {
        if (courseRepository.existsById(request.courseCode())) {
            throw new BusinessRuleException("A course with this code already exists.");
        }
        Course course = new Course();
        course.setCourseCode(request.courseCode());
        course.setCourseName(request.courseName());
        return CourseResponse.from(courseRepository.saveAndFlush(course));
    }

    /**
     * Loads a course by code.
     *
     * @param courseCode the course code
     * @return the matching course
     * @throws ValidationException if the code is unknown
     */
    public Course requireCourse(String courseCode) {
        return courseRepository.findById(courseCode)
                .orElseThrow(() -> new ValidationException("Unknown course: " + courseCode + "."));
    }

    /**
     * Loads the distinct courses in the supplied codes.
     *
     * @param courseCodes the course codes
     * @return the matching courses
     * @throws ValidationException if any code is unknown
     */
    public Set<Course> requireCourses(Collection<String> courseCodes) {
        Set<String> uniqueCodes = new HashSet<>(courseCodes);
        List<Course> found = courseRepository.findAllById(uniqueCodes);
        if (found.size() != uniqueCodes.size()) {
            throw new ValidationException("One of the selected courses does not exist.");
        }
        return new HashSet<>(found);
    }
}
