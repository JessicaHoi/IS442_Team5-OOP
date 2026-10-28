package is442t1.studybuddy.auth;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import is442t1.studybuddy.model.User;
import is442t1.studybuddy.model.UserRepository;

/**
 * Issues and checks opaque bearer tokens. Tokens live in memory, so restarting
 * the server signs everyone out; the front end then returns to the login page.
 */
@Service
public class SessionTokenService {

    private static final int TOKEN_BYTES = 32;

    private record Session(AuthenticatedUser user, String passwordHash, Instant expiresAt) {
    }

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final AuthProperties properties;
    private final Clock clock;
    private final UserRepository userRepository;

    @Autowired
    public SessionTokenService(AuthProperties properties, UserRepository userRepository) {
        this(properties, Clock.systemUTC(), userRepository);
    }

    SessionTokenService(AuthProperties properties, Clock clock, UserRepository userRepository) {
        this.properties = properties;
        this.clock = clock;
        this.userRepository = userRepository;
    }

    public synchronized String issue(User user) {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = clock.instant().plus(properties.tokenTtl());
        sessions.put(token, new Session(new AuthenticatedUser(user.getId(), user.getRole()), user.getPassword(), expiresAt));
        return token;
    }

    @Transactional(readOnly = true)
    public Optional<AuthenticatedUser> resolve(String token) {
        Session session = sessions.get(token);
        if (session == null) {
            return Optional.empty();
        }
        if (!session.expiresAt().isAfter(clock.instant())) {
            sessions.remove(token, session);
            return Optional.empty();
        }
        User account = userRepository.findById(session.user().id()).orElse(null);
        if (account == null || !account.isActive()) {
            revokeAllForUser(session.user().id());
            return Optional.empty();
        }
        if (!session.passwordHash().equals(account.getPassword())) {
            sessions.remove(token, session);
            return Optional.empty();
        }
        if (sessions.get(token) != session) {
            return Optional.empty();
        }
        return Optional.of(new AuthenticatedUser(account.getId(), account.getRole()));
    }

    public void revoke(String token) {
        sessions.remove(token);
    }

    public void revokeAllForUser(UUID userId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    removeSessionsForUser(userId);
                }
            });
        } else {
            removeSessionsForUser(userId);
        }
    }

    private synchronized void removeSessionsForUser(UUID userId) {
        sessions.entrySet().removeIf(entry -> entry.getValue().user().id().equals(userId));
    }
}
