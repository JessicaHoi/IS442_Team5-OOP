package is442t1.studybuddy.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import is442t1.studybuddy.ApiTestSupport;
import is442t1.studybuddy.common.exception.AuthenticationException;
import is442t1.studybuddy.model.User;
import is442t1.studybuddy.model.UserRepository;
import is442t1.studybuddy.model.enums.AccountStatus;

class AccountStatusAuthApiTest extends ApiTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionTokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private AuthService authService;

    @Test
    void recordsOnlySuccessfulLoginsAndPreservesCreationTime() throws Exception {
        String email = createStudent("Login Timestamp");
        UUID id = idOf(email);
        Instant createdAt = userRepository.findById(id).orElseThrow().getCreatedAt();
        assertThat(userRepository.findById(id).orElseThrow().getLastLogin()).isNull();
        signIn(email, "wrong-password", 401);
        assertThat(userRepository.findById(id).orElseThrow().getLastLogin()).isNull();
        Instant before = Instant.now().minusMillis(1);
        String token = login("  " + email.toUpperCase() + "  ");
        Instant firstLogin = userRepository.findById(id).orElseThrow().getLastLogin();
        assertThat(firstLogin).isBetween(before, Instant.now());
        mockMvc.perform(as(token, get("/api/auth/me"))).andExpect(status().isOk());
        assertThat(userRepository.findById(id).orElseThrow().getLastLogin()).isEqualTo(firstLogin);
        signIn(email, "wrong-password", 401);
        assertThat(userRepository.findById(id).orElseThrow().getLastLogin()).isEqualTo(firstLogin);
        login(email);
        assertThat(userRepository.findById(id).orElseThrow().getLastLogin()).isAfterOrEqualTo(firstLogin);
        assertThat(userRepository.findById(id).orElseThrow().getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void suspendedLoginDoesNotRecordTimeOrRevealStatusForWrongPassword() throws Exception {
        String email = createStudent("Suspended Login");
        changeStatus(idOf(email), AccountStatus.SUSPENDED);
        signIn(email, PASSWORD, 403);
        signIn(email, "wrong-password", 401);
        assertThat(userRepository.findById(idOf(email)).orElseThrow().getLastLogin()).isNull();
        changeStatus(idOf(email), AccountStatus.ACTIVE);
        mockMvc.perform(as(login(email), get("/api/students/me"))).andExpect(status().isOk());
    }

    @Test
    void suspensionInvalidatesEverySessionAndReactivationRequiresFreshLogin() throws Exception {
        String email = createStudent("Suspended Sessions");
        String first = login(email);
        String second = login(email);
        String unrelated = login(createStudent("Unaffected Session"));
        changeStatus(idOf(email), AccountStatus.SUSPENDED);
        mockMvc.perform(as(first, get("/api/courses"))).andExpect(status().isUnauthorized());
        changeStatus(idOf(email), AccountStatus.ACTIVE);
        mockMvc.perform(as(first, get("/api/auth/me"))).andExpect(status().isUnauthorized());
        mockMvc.perform(as(second, get("/api/students/me"))).andExpect(status().isUnauthorized());
        mockMvc.perform(as(unrelated, get("/api/auth/me"))).andExpect(status().isOk());
        mockMvc.perform(as(login(email), get("/api/auth/me"))).andExpect(status().isOk());
    }

    @Test
    void suspendedAdministratorsLoseAccessToBothAdminFeatures() throws Exception {
        String email = createAdministrator();
        String token = login(email);
        changeStatus(idOf(email), AccountStatus.SUSPENDED);
        mockMvc.perform(as(token, get("/api/admin/users"))).andExpect(status().isUnauthorized());
        mockMvc.perform(as(token, get("/api/admin/matching-config"))).andExpect(status().isUnauthorized());
        signIn(email, PASSWORD, 403);
    }

    @Test
    void deletedAccountCannotUseGenericAuthenticatedEndpoints() throws Exception {
        String email = createStudent("Deleted Session");
        String token = login(email);
        userRepository.deleteById(idOf(email));
        mockMvc.perform(as(token, get("/api/courses"))).andExpect(status().isUnauthorized());
        mockMvc.perform(as(token, get("/api/auth/me"))).andExpect(status().isUnauthorized());
    }

    @Test
    void passwordChangesRejectOldSessionsWithoutRevokingNewLogins() throws Exception {
        String email = createStudent("Password Sessions");
        String first = login(email);
        String second = login(email);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var account = userRepository.findById(idOf(email)).orElseThrow();
            account.setPassword(passwordEncoder.encode("replacement-password"));
        });
        String body = signIn(email, "replacement-password", 200);
        String newToken = read(body, "$.token");
        mockMvc.perform(as(first, get("/api/auth/me"))).andExpect(status().isUnauthorized());
        mockMvc.perform(as(second, get("/api/auth/me"))).andExpect(status().isUnauthorized());
        mockMvc.perform(as(newToken, get("/api/auth/me"))).andExpect(status().isOk());
        signIn(email, PASSWORD, 401);
    }

    @Test
    void logoutRevokesOnlyThePresentedSession() throws Exception {
        String email = createStudent("Logout Sessions");
        String first = login(email);
        String second = login(email);
        mockMvc.perform(as(first, post("/api/auth/logout"))).andExpect(status().isNoContent());
        mockMvc.perform(as(first, get("/api/auth/me"))).andExpect(status().isUnauthorized());
        mockMvc.perform(as(second, get("/api/auth/me"))).andExpect(status().isOk());
    }

    @Test
    void accountWideRevocationWaitsForCommitAndDoesNotRunOnRollback() throws Exception {
        String email = createStudent("Transactional Revocation");
        UUID id = idOf(email);
        String first = login(email);
        String second = login(email);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            tokenService.revokeAllForUser(id);
            assertThat(tokenService.resolve(first)).isPresent();
            status.setRollbackOnly();
        });
        assertThat(tokenService.resolve(first)).isPresent();
        transaction.executeWithoutResult(status -> {
            tokenService.revokeAllForUser(id);
            assertThat(tokenService.resolve(first)).isPresent();
        });
        mockMvc.perform(as(first, get("/api/auth/me"))).andExpect(status().isUnauthorized());
        mockMvc.perform(as(second, get("/api/auth/me"))).andExpect(status().isUnauthorized());
    }

    @Test
    void rolledBackLoginDoesNotLeaveAUsableTokenOrLoginTimestamp() {
        String email = createStudent("Rolled Back Login");
        String token = new TransactionTemplate(transactionManager).execute(status -> {
            LoginResponse response = authService.login(email, PASSWORD);
            status.setRollbackOnly();
            return response.token();
        });
        assertThat(tokenService.resolve(token)).isEmpty();
        assertThat(userRepository.findById(idOf(email)).orElseThrow().getLastLogin()).isNull();
    }

    @Test
    void loginCannotOverwriteAConcurrentPasswordChange() throws Exception {
        String email = createStudent("Concurrent Password Change");
        String replacement = passwordEncoder.encode("replacement-password");
        rejectConcurrentLogin(email, account -> account.setPassword(replacement));
        assertThat(userRepository.findById(idOf(email)).orElseThrow().getPassword()).isEqualTo(replacement);
        signIn(email, "replacement-password", 200);
    }

    @Test
    void loginCannotUndoAConcurrentSuspension() throws Exception {
        String email = createStudent("Concurrent Suspension");
        rejectConcurrentLogin(email, account -> account.setStatus(AccountStatus.SUSPENDED));
        assertThat(userRepository.findById(idOf(email)).orElseThrow().getStatus()).isEqualTo(AccountStatus.SUSPENDED);
        signIn(email, PASSWORD, 403);
    }

    private void rejectConcurrentLogin(String email, Consumer<User> change) throws Exception {
        CountDownLatch credentialsChecked = new CountDownLatch(1);
        CountDownLatch changeCommitted = new CountDownLatch(1);
        PasswordEncoder blockingEncoder = new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return passwordEncoder.encode(rawPassword);
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
                credentialsChecked.countDown();
                try {
                    if (!changeCommitted.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Concurrent account change timed out.");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
                return matches;
            }
        };
        AuthService loginService = new AuthService(userRepository, blockingEncoder, tokenService);
        try (var executor = Executors.newSingleThreadExecutor()) {
            var result = executor.submit(() -> new TransactionTemplate(transactionManager)
                    .execute(status -> loginService.login(email, PASSWORD)));
            try {
                assertThat(credentialsChecked.await(5, TimeUnit.SECONDS)).isTrue();
                new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                        change.accept(userRepository.findById(idOf(email)).orElseThrow()));
            } finally {
                changeCommitted.countDown();
            }
            assertThatThrownBy(() -> result.get(5, TimeUnit.SECONDS)).hasCauseInstanceOf(AuthenticationException.class);
        }
        assertThat(userRepository.findById(idOf(email)).orElseThrow().getLastLogin()).isNull();
    }

    private void changeStatus(UUID id, AccountStatus status) {
        new TransactionTemplate(transactionManager).executeWithoutResult(transaction -> {
            var account = userRepository.findById(id).orElseThrow();
            account.setStatus(status);
        });
    }

    private String signIn(String email, String password, int expectedStatus) throws Exception {
        var result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().is(expectedStatus));
        if (expectedStatus != 200) {
            result.andExpect(jsonPath("$.message").isString());
        }
        return result.andReturn().getResponse().getContentAsString();
    }
}
