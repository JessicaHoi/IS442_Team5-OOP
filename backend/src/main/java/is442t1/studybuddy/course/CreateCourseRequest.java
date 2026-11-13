package is442t1.studybuddy.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Student-created course payload. The code is trimmed, uppercased, alphanumeric and at most 20
 *     characters; the trimmed name is required and at most 255 characters.
 */
public record CreateCourseRequest(
        @NotBlank(message = "Enter a course code.")
        @Size(max = 20, message = "Course code must be at most 20 characters.")
        @Pattern(regexp = "[A-Za-z0-9]+", message = "Course code can contain only letters and numbers.")
        String courseCode,

        @NotBlank(message = "Enter a course name.")
        @Size(max = 255, message = "Course name must be at most 255 characters.")
        String courseName
) {
    public CreateCourseRequest {
        courseCode = courseCode == null ? null : courseCode.trim().toUpperCase(java.util.Locale.ROOT);
        courseName = courseName == null ? null : courseName.trim();
    }
}
