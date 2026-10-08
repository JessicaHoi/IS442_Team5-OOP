package is442t1.studybuddy.student;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Enter your name.")
        @Size(max = 100, message = "Name must be at most 100 characters.")
        String name,

        @NotBlank(message = "Choose your school.")
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

        @NotEmpty(message = "Select at least one course.")
        List<@NotBlank String> courses
) {
}
