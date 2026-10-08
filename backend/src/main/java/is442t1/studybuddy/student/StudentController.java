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

@RestController
@RequestMapping("/api/students")
@RequireRole(Role.STUDENT)
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/me")
    public StudentProfileResponse getOwnProfile(@CurrentUser AuthenticatedUser caller) {
        return studentService.getOwnProfile(caller.id());
    }

    @PutMapping("/me/profile")
    public StudentProfileResponse updateProfile(@CurrentUser AuthenticatedUser caller,
            @Valid @RequestBody UpdateProfileRequest request) {
        return studentService.updateProfile(caller.id(), request);
    }

    @PutMapping("/me/preferences")
    public StudentProfileResponse updatePreferences(@CurrentUser AuthenticatedUser caller,
            @Valid @RequestBody UpdatePreferencesRequest request) {
        return studentService.updatePreferences(caller.id(), request);
    }

    @GetMapping("/{studentId}")
    public StudentProfileResponse getPublicProfile(@CurrentUser AuthenticatedUser caller,
            @PathVariable UUID studentId) {
        return studentService.getPublicProfile(caller.id(), studentId);
    }
}
