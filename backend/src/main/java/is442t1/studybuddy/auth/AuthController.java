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

/**
 * Login, public student registration, current-session inspection and logout endpoints. Login and
 *     registration return opaque bearer tokens, not JWTs.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RegistrationService registrationService;

    public AuthController(AuthService authService, RegistrationService registrationService) {
        this.authService = authService;
        this.registrationService = registrationService;
    }

    /**
     * Creates an active student and signs them in: {@code POST /api/auth/register}, public.
     * The body supplies a complete {@link RegisterRequest}, with optional new courses.
     * Returns HTTP 201; invalid input is 400 and duplicate email or new course code is 409.
     *
     * @param request JSON body described above
     * @return response described above, with the documented HTTP status and payload
     */
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationService.register(request));
    }

    /**
     * Signs in with email and password: {@code POST /api/auth/login}, public.
     * Returns HTTP 200 with a bearer token and user; invalid credentials are 401 and suspended
     *     accounts are 403.
     *
     * @param request JSON body described above
     * @return response described above, with the documented HTTP status and payload
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }

    /**
     * Reads the signed-in account: {@code GET /api/auth/me}, either authenticated role.
     * Returns HTTP 200 with ID, name, email and role; an invalid or expired token is 401.
     *
     * @param caller authenticated caller injected from the bearer token
     * @return response described above, with the documented HTTP status and payload
     */
    @GetMapping("/me")
    public SessionUser me(@CurrentUser AuthenticatedUser caller) {
        return authService.currentUser(caller);
    }

    /**
     * Revokes the supplied bearer token: {@code POST /api/auth/logout}, either authenticated role.
     * Returns HTTP 204 without a body; missing or invalid authentication is 401.
     *
     * @param authorization Authorization header containing the bearer token to revoke
     * @return response described above, with the documented HTTP status and payload
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        authService.logout(authorization);
        return ResponseEntity.noContent().build();
    }
}
