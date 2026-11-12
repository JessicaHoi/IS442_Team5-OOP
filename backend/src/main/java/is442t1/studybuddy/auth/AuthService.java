package is442t1.studybuddy.auth;

import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import is442t1.studybuddy.common.exception.AuthenticationException;
import is442t1.studybuddy.common.exception.PermissionDeniedException;
import is442t1.studybuddy.model.User;
import is442t1.studybuddy.model.UserRepository;

/** Signs users in and provides account information for active sessions. */
@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid email or password.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionTokenService tokenService;

    /**
     * Creates the authentication service.
     *
     * @param userRepository the account repository
     * @param passwordEncoder the password encoder
     * @param tokenService the session token service
     */
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            SessionTokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    /**
     * Authenticates an active account and issues a session token.
     *
     * @param email the account email
     * @param password the supplied password
     * @return the new session and account details
     * @throws AuthenticationException if the credentials are invalid or changed during sign-in
     * @throws PermissionDeniedException if the account is suspended
     */
    @Transactional
    public LoginResponse login(String email, String password) {
        User user = userRepository.findByEmailIgnoreCase(email.trim())
                .filter(found -> passwordEncoder.matches(password, found.getPassword()))
                .orElseThrow(() -> new AuthenticationException(INVALID_CREDENTIALS));
        if (!user.isActive()) {
            throw new PermissionDeniedException("This account is suspended. Contact an administrator.");
        }
        if (userRepository.updateLastLoginForActiveAccount(user.getId(), user.getPassword(), Instant.now()) != 1) {
            throw new AuthenticationException(INVALID_CREDENTIALS);
        }
        String token = tokenService.issue(user);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    tokenService.revoke(token);
                }
            }
        });
        return new LoginResponse(token, SessionUser.from(user));
    }

    /**
     * Looks up the account for an authenticated caller.
     *
     * @param caller the authenticated caller
     * @return the account's session representation
     * @throws AuthenticationException if the account no longer exists
     */
    public SessionUser currentUser(AuthenticatedUser caller) {
        return userRepository.findById(caller.id())
                .map(SessionUser::from)
                .orElseThrow(() -> new AuthenticationException("Your account no longer exists."));
    }

    /**
     * Revokes the bearer token in an authorization header, if present.
     *
     * @param authorizationHeader the authorization header
     */
    public void logout(String authorizationHeader) {
        BearerToken.extract(authorizationHeader).ifPresent(tokenService::revoke);
    }
}
