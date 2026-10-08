package is442t1.studybuddy.auth;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import is442t1.studybuddy.model.User;

/**
 * Issues and checks opaque bearer tokens. Tokens live in memory, so restarting
 * the server signs everyone out; the front end then returns to the login page.
 */
@Service
public class SessionTokenService {

    private static final int TOKEN_BYTES = 32;

    private record Session(AuthenticatedUser user, Instant expiresAt) {
    }

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final AuthProperties properties;
    private final Clock clock;

    @Autowired
    public SessionTokenService(AuthProperties properties) {
        this(properties, Clock.systemUTC());
    }

    SessionTokenService(AuthProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public String issue(User user) {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = clock.instant().plus(properties.tokenTtl());
        sessions.put(token, new Session(new AuthenticatedUser(user.getId(), user.getRole()), expiresAt));
        return token;
    }

    public Optional<AuthenticatedUser> resolve(String token) {
        Session session = sessions.get(token);
        if (session == null) {
            return Optional.empty();
        }
        if (session.expiresAt().isBefore(clock.instant())) {
            sessions.remove(token);
            return Optional.empty();
        }
        return Optional.of(session.user());
    }

    public void revoke(String token) {
        sessions.remove(token);
    }
}
