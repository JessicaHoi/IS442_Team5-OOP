package is442t1.studybuddy.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import is442t1.studybuddy.model.UserRepository;
import is442t1.studybuddy.model.enums.AccountStatus;
import is442t1.studybuddy.student.Student;

class SessionTokenServiceTest {

    private final Instant now = Instant.parse("2026-10-28T01:00:00Z");
    private UserRepository userRepository;
    private Clock clock;
    private SessionTokenService tokenService;
    private Student account;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        clock = mock(Clock.class);
        when(clock.instant()).thenReturn(now);
        tokenService = new SessionTokenService(new AuthProperties(Duration.ofMinutes(1)), clock, userRepository);
        account = account(UUID.randomUUID(), "password-hash");
        when(userRepository.findById(account.getId())).thenReturn(Optional.of(account));
    }

    @Test
    void tokensAreDistinctAndExpireAtTheConfiguredBoundary() {
        String first = tokenService.issue(account);
        String second = tokenService.issue(account);
        assertThat(first).isNotEqualTo(second);
        assertThat(tokenService.resolve(first)).contains(new AuthenticatedUser(account.getId(), account.getRole()));
        when(clock.instant()).thenReturn(now.plusSeconds(60));
        assertThat(tokenService.resolve(first)).isEmpty();
        assertThat(tokenService.resolve(second)).isEmpty();
    }

    @Test
    void unknownAndExpiredTokensDoNotQueryAccounts() {
        assertThat(tokenService.resolve("unknown-token")).isEmpty();
        String expired = tokenService.issue(account);
        when(clock.instant()).thenReturn(now.plusSeconds(61));
        assertThat(tokenService.resolve(expired)).isEmpty();
        verifyNoInteractions(userRepository);
    }

    @Test
    void revokingAllSessionsLeavesOtherAccountsSignedIn() {
        Student other = account(UUID.randomUUID(), "another-hash");
        when(userRepository.findById(other.getId())).thenReturn(Optional.of(other));
        String first = tokenService.issue(account);
        String second = tokenService.issue(account);
        String unrelated = tokenService.issue(other);
        tokenService.revokeAllForUser(account.getId());
        assertThat(tokenService.resolve(first)).isEmpty();
        assertThat(tokenService.resolve(second)).isEmpty();
        assertThat(tokenService.resolve(unrelated)).isPresent();
    }

    @Test
    void staleLoginCannotIssueAUsableTokenAfterPasswordChange() {
        Student updated = account(account.getId(), "replacement-hash");
        when(userRepository.findById(account.getId())).thenReturn(Optional.of(updated));
        String staleToken = tokenService.issue(account);
        String freshToken = tokenService.issue(updated);
        assertThat(tokenService.resolve(staleToken)).isEmpty();
        assertThat(tokenService.resolve(freshToken)).isPresent();
    }

    @Test
    void staleLoginCannotIssueAUsableTokenAfterSuspension() {
        Student suspended = account(account.getId(), account.getPassword());
        suspended.setStatus(AccountStatus.SUSPENDED);
        when(userRepository.findById(account.getId())).thenReturn(Optional.of(suspended));
        String staleToken = tokenService.issue(account);
        assertThat(tokenService.resolve(staleToken)).isEmpty();
        suspended.setStatus(AccountStatus.ACTIVE);
        assertThat(tokenService.resolve(staleToken)).isEmpty();
    }

    @Test
    void revocationDuringAccountLookupRejectsTheInFlightToken() throws Exception {
        CountDownLatch lookupStarted = new CountDownLatch(1);
        CountDownLatch lookupMayComplete = new CountDownLatch(1);
        when(userRepository.findById(account.getId())).thenAnswer(invocation -> {
            lookupStarted.countDown();
            if (!lookupMayComplete.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Account lookup timed out.");
            }
            return Optional.of(account);
        });
        String token = tokenService.issue(account);
        try (var executor = Executors.newSingleThreadExecutor()) {
            var result = executor.submit(() -> tokenService.resolve(token));
            try {
                assertThat(lookupStarted.await(5, TimeUnit.SECONDS)).isTrue();
                tokenService.revokeAllForUser(account.getId());
            } finally {
                lookupMayComplete.countDown();
            }
            assertThat(result.get(5, TimeUnit.SECONDS)).isEmpty();
        }
    }

    private Student account(UUID id, String passwordHash) {
        Student student = new Student();
        student.setId(id);
        student.setName("Session Student");
        student.setEmail(id + "@test.smu.edu.sg");
        student.setPassword(passwordHash);
        return student;
    }
}
