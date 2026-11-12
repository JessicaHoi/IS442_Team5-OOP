package is442t1.studybuddy.course;

/**
 * Course as the API returns it.
 *
 * @param code the course code
 * @param name the course name
 * @param school the school offering the course
 */
public record CourseResponse(String code, String name, String school) {

    /**
     * Converts a course to its API representation.
     *
     * @param course the course to convert
     * @return the course response
     */
    public static CourseResponse from(Course course) {
        return new CourseResponse(course.getCourseCode(), course.getCourseName(), course.getSchool());
    }
}
