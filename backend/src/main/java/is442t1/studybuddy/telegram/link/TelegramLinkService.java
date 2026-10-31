package is442t1.studybuddy.telegram.link;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import is442t1.studybuddy.common.exception.BusinessRuleException;
import is442t1.studybuddy.group.StudyGroup;
import is442t1.studybuddy.student.Student;
import is442t1.studybuddy.student.StudentService;
import is442t1.studybuddy.telegram.TelegramProperties;

/** Connects a student to their Telegram account through a one-time link they open in Telegram. */
@Service
@Transactional
public class TelegramLinkService {

    private static final int TOKEN_BYTES = 32;

    private final TelegramAccountRepository accountRepository;
    private final TelegramLinkTokenRepository tokenRepository;
    private final StudentService studentService;
    private final TelegramProperties properties;
    private final SecureRandom random = new SecureRandom();

    public TelegramLinkService(TelegramAccountRepository accountRepository, TelegramLinkTokenRepository tokenRepository,
            StudentService studentService, TelegramProperties properties) {
        this.accountRepository = accountRepository;
        this.tokenRepository = tokenRepository;
        this.studentService = studentService;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public TelegramStatusResponse status(UUID studentId) {
        return findAccount(studentId).map(TelegramStatusResponse::of).orElseGet(TelegramStatusResponse::notLinked);
    }

    /** The Telegram user connected to this student through this bot, if any. */
    @Transactional(readOnly = true)
    public Optional<Long> linkedTelegramUserId(UUID studentId) {
        return findAccount(studentId).map(TelegramAccount::getTelegramUserId);
    }

    public TelegramLinkResponse createLink(UUID studentId) {
        if (!properties.enabled()) {
            throw new BusinessRuleException(TelegramProperties.OFF_MESSAGE);
        }
        Student student = studentService.requireStudent(studentId);
        Instant now = Instant.now();
        tokenRepository.deleteReplacedOrExpired(properties.botId(), studentId, TelegramLinkPurpose.ACCOUNT, now);

        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = now.plus(properties.linkTokenTtl());
        tokenRepository.save(new TelegramLinkToken(hash(token), student, properties.botId(), expiresAt));

        return new TelegramLinkResponse("https://t.me/" + properties.botUsername() + "?start=a_" + token, expiresAt);
    }

    public void unlink(UUID studentId) {
        findAccount(studentId).ifPresent(accountRepository::delete);
    }

    /**
     * Called when someone presses Start on a link. If this Telegram user was linked to another student, the latest
     * link wins. Returns false if the token is unknown, used or expired, or the student's account is no longer active.
     */
    public boolean completeLink(String token, long telegramUserId, String username, String firstName) {
        long botId = properties.botId();
        String tokenHash = hash(token);
        Instant now = Instant.now();
        Optional<TelegramLinkToken> found = tokenRepository.findByTokenHashAndBotId(tokenHash, botId)
                .filter(candidate -> candidate.getStudyGroup() == null);
        if (found.isEmpty() || tokenRepository.claim(tokenHash, botId, now) == 0) {
            return false;
        }
        Student student = found.get().getStudent();
        if (!student.isActive()) {
            return false;
        }
        accountRepository.findByBotIdAndTelegramUserId(botId, telegramUserId)
                .filter(existing -> !existing.getStudent().getId().equals(student.getId()))
                .ifPresent(existing -> {
                    accountRepository.delete(existing);
                    // Hibernate runs inserts before deletes, which would break the unique constraint.
                    accountRepository.flush();
                });
        TelegramAccount account = accountRepository.findByBotIdAndStudent_Id(botId, student.getId())
                .orElseGet(() -> new TelegramAccount(student, botId));
        account.linkTo(telegramUserId, username, firstName, now);
        accountRepository.save(account);
        return true;
    }

    /** A link the leader opens in Telegram to set up their group's chat. A new one replaces any earlier one. */
    public TelegramLinkResponse createGroupSetupLink(Student leader, StudyGroup group) {
        Instant now = Instant.now();
        tokenRepository.deleteReplacedOrExpired(properties.botId(), leader.getId(), TelegramLinkPurpose.GROUP_SETUP, now);
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = now.plus(properties.linkTokenTtl());
        tokenRepository.save(new TelegramLinkToken(hash(token), leader, properties.botId(), expiresAt, group));
        return new TelegramLinkResponse("https://t.me/" + properties.botUsername() + "?start=g_" + token, expiresAt);
    }

    /**
     * Ties a setup link to the Telegram user who opened it and returns its first button id. Opening it again gives
     * the same id, so the buttons can be resent. Empty if the link isn't usable or belongs to someone else, the
     * student no longer leads the group, or the group has closed.
     */
    public Optional<Integer> openGroupSetup(String token, long telegramUserId) {
        Instant now = Instant.now();
        return tokenRepository.findByTokenHashAndBotId(hash(token), properties.botId())
                .filter(found -> found.getStudyGroup() != null && found.isUsable(now))
                .filter(TelegramLinkService::canStillSetUp)
                .filter(found -> found.claimFor(telegramUserId, random.nextInt(1, Integer.MAX_VALUE - 1)))
                .map(TelegramLinkToken::getRequestId);
    }

    /** The study group a chosen chat would be linked to, if the choice is valid. Leaves the link usable. */
    @Transactional(readOnly = true)
    public Optional<UUID> groupInSetup(long telegramUserId, int requestId) {
        return tokenRepository.findSetupInProgress(properties.botId(), telegramUserId, requestId, Instant.now())
                .filter(TelegramLinkService::canStillSetUp)
                .map(found -> found.getStudyGroup().getId());
    }

    /** Called once the chosen chat is saved, so a failure before that leaves the leader able to try again. */
    public void useUpGroupSetup(long telegramUserId, int requestId) {
        Instant now = Instant.now();
        tokenRepository.findSetupInProgress(properties.botId(), telegramUserId, requestId, now).ifPresent(found -> found.markUsed(now));
    }

    private static boolean canStillSetUp(TelegramLinkToken token) {
        StudyGroup group = token.getStudyGroup();
        return group.isOpen() && group.isLedBy(token.getStudent().getId()) && token.getStudent().isActive();
    }

    private Optional<TelegramAccount> findAccount(UUID studentId) {
        return properties.enabled() ? accountRepository.findByBotIdAndStudent_Id(properties.botId(), studentId) : Optional.empty();
    }

    private static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Every Java runtime has SHA-256.", exception);
        }
    }
}
