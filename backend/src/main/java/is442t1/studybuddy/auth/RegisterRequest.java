package is442t1.studybuddy.auth;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import is442t1.studybuddy.course.CreateCourseRequest;

/**
 * Public student-signup payload with a complete profile, selected course codes and optional new
 *     courses. New courses use courseCode and courseName, and can be selected in the same request.
 * Passwords require at least 6 characters and at most 72 UTF-8 bytes; year of study is 1 through 5.
 *     A missing newCourses list becomes an empty list.
 */
public record RegisterRequest(
        @NotBlank(message = "Enter your name.")
        @Size(max = 100, message = "Name must be at most 100 characters.")
        String name,

        @NotBlank(message = "Enter your email.")
        @Email(message = "Enter a valid email address.")
        @Size(max = 255, message = "Email must be at most 255 characters.")
        String email,

        @NotBlank(message = "Enter a password.")
        String password,

        @NotBlank(message = "Choose your school.")
        @Size(max = 255, message = "School must be at most 255 characters.")
        String school,

        @NotBlank(message = "Enter your programme.")
        @Size(max = 100, message = "Programme must be at most 100 characters.")
        String programme,

        @NotNull(message = "Choose your year of study.")
        @Min(value = 1, message = "Year of study must be between 1 and 5.")
        @Max(value = 5, message = "Year of study must be between 1 and 5.")
        Integer yearOfStudy,

        @NotBlank(message = "Enter your contact number.")
        @Pattern(regexp = "\\+?[\\d\\s-]{8,15}", message = "Enter a valid phone number, e.g. +65 9123 4567.")
        String contactNumber,

        String telegramHandle,

        @NotEmpty(message = "Select at least one course.")
        List<@NotBlank String> courses,

        List<@NotNull @Valid CreateCourseRequest> newCourses
) {
    public RegisterRequest {
        name = name == null ? null : name.trim();
        email = email == null ? null : email.trim();
        school = school == null ? null : school.trim();
        programme = programme == null ? null : programme.trim();
        contactNumber = contactNumber == null ? null : contactNumber.trim();
        newCourses = newCourses == null ? List.of() : newCourses;
    }
}
