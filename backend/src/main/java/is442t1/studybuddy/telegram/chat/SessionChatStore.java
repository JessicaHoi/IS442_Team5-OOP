package is442t1.studybuddy.telegram.chat;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import is442t1.studybuddy.model.BaseEntity;
import is442t1.studybuddy.model.enums.RsvpResponse;
import is442t1.studybuddy.model.enums.SessionStatus;
import is442t1.studybuddy.studysession.StudySession;
import is442t1.studybuddy.studysession.StudySessionRepository;
import is442t1.studybuddy.telegram.TelegramProperties;
import is442t1.studybuddy.telegram.link.TelegramAccountRepository;

/** The database side of study session messages, in short transactions, for the same reason as {@link ChatStore}. */
@Service
@Transactional
@ConditionalOnProperty(prefix = "studybuddy.telegram", name = "enabled", havingValue = "true")
class SessionChatStore {

    /** How long an instance has to send a reminder before another may try. */
    private static final Duration CLAIM_EXPIRES_AFTER = Duration.ofMinutes(10);
    /** How early a call can start and still count as being for the next session. */
    private static final Duration VIDEO_CHAT_EARLY_START = Duration.ofMinutes(30);

    record SessionRef(UUID groupId, SessionStatus status, SessionMessages.Details details) {
    }

    /** The student a button press counts for. */
    record Responder(UUID groupId, UUID studentId) {
    }

    private final StudySessionRepository sessionRepository;
    private final GroupChatRepository chatRepository;
    private final ChatMembershipRepository membershipRepository;
    private final TelegramAccountRepository accountRepository;
    private final TelegramProperties properties;

    SessionChatStore(StudySessionRepository sessionRepository, GroupChatRepository chatRepository,
            ChatMembershipRepository membershipRepository, TelegramAccountRepository accountRepository, TelegramProperties properties) {
        this.sessionRepository = sessionRepository;
        this.chatRepository = chatRepository;
        this.membershipRepository = membershipRepository;
        this.accountRepository = accountRepository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public Optional<SessionRef> read(UUID sessionId) {
        return sessionRepository.findById(sessionId).map(session -> new SessionRef(session.getStudyGroup().getId(), session.getStatus(),
                new SessionMessages.Details(session.getTitle(), session.getStartsAt(), session.getEndsAt(), session.getMeetingMode(),
                        session.getLocation(), session.getNote(), session.getMeetingLink(), session.isGroupVideoChat(),
                        session.getRsvps().stream().filter(rsvp -> rsvp.getResponse() == RsvpResponse.GOING).count())));
    }

    /**
     * Who pressed a button: the student they joined this group's chat as, otherwise the student who connected that
     * Telegram account. Being a member is checked when the answer is saved.
     */
    @Transactional(readOnly = true)
    public Optional<Responder> responderFor(UUID sessionId, long telegramUserId) {
        return sessionRepository.findById(sessionId).flatMap(session -> {
            UUID groupId = session.getStudyGroup().getId();
            return joinedAs(groupId, telegramUserId)
                    .or(() -> connectedAs(telegramUserId))
                    .map(studentId -> new Responder(groupId, studentId));
        });
    }

    private Optional<UUID> joinedAs(UUID groupId, long telegramUserId) {
        return chatRepository.findByStudyGroup_Id(groupId)
                .filter(chat -> chat.getBotId() == properties.botId())
                .flatMap(chat -> membershipRepository.findByGroupChat_IdAndTelegramUserId(chat.getId(), telegramUserId))
                .filter(membership -> membership.getState() == ChatMemberState.JOINED)
                .map(membership -> membership.getStudent().getId());
    }

    private Optional<UUID> connectedAs(long telegramUserId) {
        return accountRepository.findByBotIdAndTelegramUserId(properties.botId(), telegramUserId)
                .map(account -> account.getStudent())
                .map(BaseEntity::getId);
    }

    /** Those going whom the bot can tag: people who aren't in the chat can't be notified there. */
    @Transactional(readOnly = true)
    public List<SessionMessages.Attendee> goingInChat(UUID sessionId) {
        StudySession session = sessionRepository.findById(sessionId).orElseThrow();
        Map<UUID, Long> inChat = chatRepository.findByStudyGroup_Id(session.getStudyGroup().getId())
                .filter(chat -> chat.getBotId() == properties.botId())
                .map(chat -> membershipRepository.findByGroupChat_Id(chat.getId()).stream()
                        .filter(membership -> membership.getState() == ChatMemberState.JOINED && membership.getTelegramUserId() != null)
                        .collect(Collectors.toMap(membership -> membership.getStudent().getId(), ChatMembership::getTelegramUserId)))
                .orElse(Map.of());
        return session.getRsvps().stream()
                .filter(rsvp -> rsvp.getResponse() == RsvpResponse.GOING && inChat.containsKey(rsvp.getStudent().getId()))
                .map(rsvp -> new SessionMessages.Attendee(rsvp.getStudent().getName(), inChat.get(rsvp.getStudent().getId())))
                .sorted(Comparator.comparing(SessionMessages.Attendee::name))
                .toList();
    }

    /** The video chat session a call in this group is most likely for. */
    @Transactional(readOnly = true)
    public Optional<UUID> videoChatSession(UUID groupId, Instant now) {
        return sessionRepository.findVideoChatSessions(groupId, now.plus(VIDEO_CHAT_EARLY_START), now).stream()
                .findFirst()
                .map(StudySession::getId);
    }

    @Transactional(readOnly = true)
    public List<UUID> dueReminders(Instant now) {
        Duration lead = properties.reminderLead();
        return chatRepository.findSessionsStartingSoon(properties.botId(), now, now.plus(lead)).stream()
                .filter(session -> session.isDueForReminder(lead, now))
                .map(StudySession::getId)
                .toList();
    }

    /** Whether this instance should send the reminder. Another instance on the same bot may have got there first. */
    public boolean claimReminder(UUID sessionId, Instant now) {
        return sessionRepository.claimReminder(sessionId, now, now.minus(CLAIM_EXPIRES_AFTER)) == 1;
    }

    public void markReminderSent(UUID sessionId, Instant claimedAt) {
        sessionRepository.markReminderSent(sessionId, claimedAt, Instant.now());
    }
}
