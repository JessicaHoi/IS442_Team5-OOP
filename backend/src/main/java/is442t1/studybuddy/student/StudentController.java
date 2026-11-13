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

/**
 * Student-only own-profile and per-course preference updates, plus privacy-filtered profile
 *     viewing.
 */
@RestController
@RequestMapping("/api/students")
@RequireRole(Role.STUDENT)
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    /**
     * Reads the caller's full profile: {@code GET /api/students/me}, student only.
     * Returns HTTP 200 including email and contact details; a missing student is 404.
     *
     * @param caller authenticated caller injected from the bearer token
     * @return response described above, with the documented HTTP status and payload
     */
    @GetMapping("/me")
    public StudentProfileResponse getOwnProfile(@CurrentUser AuthenticatedUser caller) {
        return studentService.getOwnProfile(caller.id());
    }

    /**
     * Replaces own profile fields: {@code PUT /api/students/me/profile}, student only.
     * The body is {@link UpdateProfileRequest}. Removing courses removes their preferences.
     * Returns HTTP 200 with the full profile; invalid fields or unknown courses are 400.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param request JSON body described above
     * @return response described above, with the documented HTTP status and payload
     */
    @PutMapping("/me/profile")
    public StudentProfileResponse updateProfile(@CurrentUser AuthenticatedUser caller,
            @Valid @RequestBody UpdateProfileRequest request) {
        return studentService.updateProfile(caller.id(), request);
    }

    /**
     * Replaces all per-course preferences: {@code PUT /api/students/me/preferences}, student only.
     * The body is {@link UpdatePreferencesRequest}; omitted courses lose their preference.
     * Returns HTTP 200 with the profile; duplicate courses, courses not taken or invalid
     *     availability are 400.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param request JSON body described above
     * @return response described above, with the documented HTTP status and payload
     */
    @PutMapping("/me/preferences")
    public StudentProfileResponse updatePreferences(@CurrentUser AuthenticatedUser caller,
            @Valid @RequestBody UpdatePreferencesRequest request) {
        return studentService.updatePreferences(caller.id(), request);
    }

    /**
     * Reads a student profile: {@code GET /api/students/{studentId}}, student only.
     * The owner sees their full profile. Other viewers never see email and see contact details only
     *     when connected.
     * Returns HTTP 200; a missing student is 404.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param studentId student UUID from the path
     * @return response described above, with the documented HTTP status and payload
     */
    @GetMapping("/{studentId}")
    public StudentProfileResponse getPublicProfile(@CurrentUser AuthenticatedUser caller,
            @PathVariable UUID studentId) {
        return studentService.getPublicProfile(caller.id(), studentId);
    }
}
