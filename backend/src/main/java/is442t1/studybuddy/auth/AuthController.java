package is442t1.studybuddy.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** Authentication and registration endpoints. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RegistrationService registrationService;

    /**
     * Creates the authentication controller.
     *
     * @param authService the sign-in and session service
     * @param registrationService the student registration service
     */
    public AuthController(AuthService authService, RegistrationService registrationService) {
        this.authService = authService;
        this.registrationService = registrationService;
    }

    /**
     * Registers and signs in a student.
     *
     * @param request the registration details
     * @return the new session with HTTP 201
     */
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationService.register(request));
    }

    /**
     * Signs in with account credentials.
     *
     * @param request the login credentials
     * @return the new session
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }

    /**
     * Returns the signed-in user's account details.
     *
     * @param caller the authenticated caller
     * @return the session user
     */
    @GetMapping("/me")
    public SessionUser me(@CurrentUser AuthenticatedUser caller) {
        return authService.currentUser(caller);
    }

    /**
     * Signs out the session identified by the authorization header.
     *
     * @param authorization the authorization header
     * @return an empty HTTP 204 response
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        authService.logout(authorization);
        return ResponseEntity.noContent().build();
    }
}
