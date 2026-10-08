package is442t1.studybuddy.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import is442t1.studybuddy.common.exception.AuthenticationException;
import is442t1.studybuddy.model.User;
import is442t1.studybuddy.model.UserRepository;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid email or password.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionTokenService tokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            SessionTokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public LoginResponse login(String email, String password) {
        User user = userRepository.findByEmailIgnoreCase(email.trim())
                .filter(found -> passwordEncoder.matches(password, found.getPassword()))
                .orElseThrow(() -> new AuthenticationException(INVALID_CREDENTIALS));
        return new LoginResponse(tokenService.issue(user), SessionUser.from(user));
    }

    public SessionUser currentUser(AuthenticatedUser caller) {
        return userRepository.findById(caller.id())
                .map(SessionUser::from)
                .orElseThrow(() -> new AuthenticationException("Your account no longer exists."));
    }

    public void logout(String authorizationHeader) {
        BearerToken.extract(authorizationHeader).ifPresent(tokenService::revoke);
    }
}
