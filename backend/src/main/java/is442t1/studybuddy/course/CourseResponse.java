package is442t1.studybuddy.course;

/** Course as the API returns it: {@code {code, name, school}}. */
public record CourseResponse(String code, String name, String school) {

    public static CourseResponse from(Course course) {
        return new CourseResponse(course.getCourseCode(), course.getCourseName(), course.getSchool());
    }
}
