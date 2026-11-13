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

/**
 * Lists the catalogue in code order, creates new courses and validates selected course codes.
 *     Unknown selected courses are validation errors rather than missing-resource errors.
 */
@Service
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public List<CourseResponse> listCourses() {
        return courseRepository.findAll(Sort.by("courseCode")).stream().map(CourseResponse::from).toList();
    }

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

    /** Loads one course, or fails with a 400 because the client sent an unknown code. */
    public Course requireCourse(String courseCode) {
        return courseRepository.findById(courseCode)
                .orElseThrow(() -> new ValidationException("Unknown course: " + courseCode + "."));
    }

    /** Loads every course in {@code courseCodes}, failing if any code is unknown. */
    public Set<Course> requireCourses(Collection<String> courseCodes) {
        Set<String> uniqueCodes = new HashSet<>(courseCodes);
        List<Course> found = courseRepository.findAllById(uniqueCodes);
        if (found.size() != uniqueCodes.size()) {
            throw new ValidationException("One of the selected courses does not exist.");
        }
        return new HashSet<>(found);
    }
}
