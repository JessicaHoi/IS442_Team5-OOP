package is442t1.studybuddy.student;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import is442t1.studybuddy.auth.AuthenticatedUser;
import is442t1.studybuddy.auth.CurrentUser;
import is442t1.studybuddy.auth.RequireRole;
import is442t1.studybuddy.model.enums.Role;

/** Endpoints for student profiles and study preferences. */
@RestController
@RequestMapping("/api/students")
@RequireRole(Role.STUDENT)
public class StudentController {

    private final StudentService studentService;

    /**
     * Creates the student controller.
     *
     * @param studentService the student profile service
     */
    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    /**
     * Gets the caller's profile.
     *
     * @param caller the authenticated student
     * @return the caller's profile
     */
    @GetMapping("/me")
    public StudentProfileResponse getOwnProfile(@CurrentUser AuthenticatedUser caller) {
        return studentService.getOwnProfile(caller.id());
    }

    /**
     * Updates the caller's profile.
     *
     * @param caller the authenticated student
     * @param request the replacement profile details
     * @return the updated profile
     */
    @PutMapping("/me/profile")
    public StudentProfileResponse updateProfile(@CurrentUser AuthenticatedUser caller,
            @Valid @RequestBody UpdateProfileRequest request) {
        return studentService.updateProfile(caller.id(), request);
    }

    /**
     * Replaces the caller's course preferences.
     *
     * @param caller the authenticated student
     * @param request the replacement preferences
     * @return the updated profile
     */
    @PutMapping("/me/preferences")
    public StudentProfileResponse updatePreferences(@CurrentUser AuthenticatedUser caller,
            @Valid @RequestBody UpdatePreferencesRequest request) {
        return studentService.updatePreferences(caller.id(), request);
    }

    /**
     * Gets a student's profile as visible to the caller.
     *
     * @param caller the authenticated student
     * @param studentId the requested student's identifier
     * @return the visible profile
     */
    @GetMapping("/{studentId}")
    public StudentProfileResponse getPublicProfile(@CurrentUser AuthenticatedUser caller,
            @PathVariable UUID studentId) {
        return studentService.getPublicProfile(caller.id(), studentId);
    }
}
