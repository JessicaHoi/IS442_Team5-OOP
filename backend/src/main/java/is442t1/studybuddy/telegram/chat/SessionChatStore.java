package is442t1.studybuddy.telegram.chat;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import is442t1.studybuddy.common.AppProperties;
import is442t1.studybuddy.model.BaseEntity;
import is442t1.studybuddy.model.enums.RsvpResponse;
import is442t1.studybuddy.model.enums.SessionStatus;
import is442t1.studybuddy.student.Student;
import is442t1.studybuddy.studysession.StudySession;
import is442t1.studybuddy.studysession.StudySessionRepository;
import is442t1.studybuddy.telegram.TelegramProperties;
import is442t1.studybuddy.telegram.link.TelegramAccount;
import is442t1.studybuddy.telegram.link.TelegramAccountRepository;

/** The database side of study session messages, in short transactions, for the same reason as {@link ChatStore}. */
@Service
@Transactional
@ConditionalOnProperty(prefix = "studybuddy.telegram", name = "enabled", havingValue = "true")
class SessionChatStore {

    /** How early a call can start and still count as being for the next session. */
    private static final Duration VIDEO_CHAT_EARLY_START = Duration.ofMinutes(30);

    record SessionRef(UUID groupId, SessionStatus status, SessionMessages.Details details) {
    }

    /** A posted card's message. */
    record PinnedCard(UUID sessionId, long messageId) {
    }

    /** What to pin and unpin so that the soonest upcoming session is the one pinned. @param pin null if nothing needs pinning */
    record PinChange(PinnedCard pin, List<PinnedCard> unpin) {
    }

    /** The student a button press counts for. */
    record Responder(UUID groupId, UUID studentId) {
    }

    private final StudySessionRepository sessionRepository;
    private final GroupChatRepository chatRepository;
    private final ChatMembershipRepository membershipRepository;
    private final SessionChatCardRepository cardRepository;
    private final TelegramAccountRepository accountRepository;
    private final TelegramProperties properties;
    private final ZoneId zone;

    SessionChatStore(StudySessionRepository sessionRepository, GroupChatRepository chatRepository,
            ChatMembershipRepository membershipRepository, SessionChatCardRepository cardRepository,
            TelegramAccountRepository accountRepository, TelegramProperties properties, AppProperties appProperties) {
        this.sessionRepository = sessionRepository;
        this.chatRepository = chatRepository;
        this.membershipRepository = membershipRepository;
        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
        this.properties = properties;
        this.zone = appProperties.timeZone();
    }

    @Transactional(readOnly = true)
    public Optional<SessionRef> read(UUID sessionId) {
        return sessionRepository.findById(sessionId).map(session -> new SessionRef(session.getStudyGroup().getId(), session.getStatus(),
                new SessionMessages.Details(session.getTitle(), session.getStartsAt(), session.getEndsAt(), session.getMeetingMode(),
                        session.getLocation(), session.getNote(), session.getMeetingLink(), session.isGroupVideoChat(),
                        answered(session, RsvpResponse.GOING), answered(session, RsvpResponse.NOT_GOING), notAnswered(session))));
    }

    private static List<String> answered(StudySession session, RsvpResponse response) {
        return session.getRsvps().stream()
                .filter(rsvp -> rsvp.getResponse() == response)
                .map(rsvp -> rsvp.getStudent().getName())
                .sorted()
                .toList();
    }

    private static List<String> notAnswered(StudySession session) {
        Set<UUID> answered = session.getRsvps().stream().map(rsvp -> rsvp.getStudent().getId()).collect(Collectors.toSet());
        return session.getStudyGroup().getMembers().stream()
                .filter(student -> !answered.contains(student.getId()))
                .map(Student::getName)
                .sorted()
                .toList();
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
        Map<UUID, Long> inChat = telegramIdsInChat(session);
        return session.getRsvps().stream()
                .filter(rsvp -> rsvp.getResponse() == RsvpResponse.GOING && inChat.containsKey(rsvp.getStudent().getId()))
                .map(rsvp -> new SessionMessages.Attendee(rsvp.getStudent().getName(), inChat.get(rsvp.getStudent().getId())))
                .sorted(Comparator.comparing(SessionMessages.Attendee::name))
                .toList();
    }

    /** Those with no answer whom the bot can tag. The rest are still named on the card. */
    @Transactional(readOnly = true)
    public List<SessionMessages.Attendee> silentInChat(UUID sessionId) {
        StudySession session = sessionRepository.findById(sessionId).orElseThrow();
        Map<UUID, Long> inChat = telegramIdsInChat(session);
        Set<UUID> answered = session.getRsvps().stream().map(rsvp -> rsvp.getStudent().getId()).collect(Collectors.toSet());
        return session.getStudyGroup().getMembers().stream()
                .filter(student -> !answered.contains(student.getId()) && inChat.containsKey(student.getId()))
                .map(student -> new SessionMessages.Attendee(student.getName(), inChat.get(student.getId())))
                .sorted(Comparator.comparing(SessionMessages.Attendee::name))
                .toList();
    }

    private Map<UUID, Long> telegramIdsInChat(StudySession session) {
        return chatRepository.findByStudyGroup_Id(session.getStudyGroup().getId())
                .filter(chat -> chat.getBotId() == properties.botId())
                .map(chat -> membershipRepository.findByGroupChat_Id(chat.getId()).stream()
                        .filter(membership -> membership.getState() == ChatMemberState.JOINED && membership.getTelegramUserId() != null)
                        .collect(Collectors.toMap(membership -> membership.getStudent().getId(), ChatMembership::getTelegramUserId)))
                .orElse(Map.of());
    }

    /**
     * Where to message the leader privately: their connected account, otherwise their account in the chat. Setting up a
     * chat means starting the bot, which it needs to message them, but someone made leader later may never have.
     */
    @Transactional(readOnly = true)
    public Optional<Long> leaderTelegramId(UUID sessionId) {
        return sessionRepository.findById(sessionId).flatMap(session -> {
            UUID leaderId = session.getStudyGroup().getLeader().getId();
            return accountRepository.findByBotIdAndStudent_Id(properties.botId(), leaderId)
                    .map(TelegramAccount::getTelegramUserId)
                    .or(() -> Optional.ofNullable(telegramIdsInChat(session).get(leaderId)));
        });
    }

    /** The video chat session a call in this group is most likely for. */
    @Transactional(readOnly = true)
    public Optional<UUID> videoChatSession(UUID groupId, Instant now) {
        return sessionRepository.findVideoChatSessions(groupId, now.plus(VIDEO_CHAT_EARLY_START), now).stream()
                .findFirst()
                .map(StudySession::getId);
    }

    /**
     * Sessions whose reminder should go out now. It's a reply to the card, so a session needs a card first. One whose
     * card was posted or moved after the reminder window opened has only just been announced, so it gets no reminder as well.
     */
    @Transactional(readOnly = true)
    public List<UUID> dueReminders(Instant now) {
        Duration lead = properties.reminderLead();
        return chatRepository.findSessionsStartingSoon(properties.botId(), now, now.plus(lead)).stream()
                .filter(session -> cardRepository.findByStudySession_Id(session.getId())
                        .filter(card -> card.getMessageId() != null)
                        .filter(card -> !card.announcedAfter(session.getStartsAt().minus(lead)))
                        .isPresent())
                .map(StudySession::getId)
                .toList();
    }

    /** Sessions whose chat can be asked for missing answers now. */
    @Transactional(readOnly = true)
    public List<UUID> dueNudges(Instant now) {
        Duration lead = properties.nudgeLead();
        return cardRepository.findAwaitingNudge(properties.botId(), now, now.plus(lead)).stream()
                .filter(card -> NudgeRule.isDue(card.getStudySession().getStartsAt(), card.getAnnouncedAt(), lead, zone, now))
                .map(card -> card.getStudySession().getId())
                .toList();
    }

    /**
     * Sessions whose card needs attention without anything having changed: upcoming ones with no card, such as after a
     * failed post or linking a chat late, and ones whose card shows a phase that time has moved on from.
     */
    @Transactional(readOnly = true)
    public List<UUID> cardsToRefresh(Instant now) {
        Stream<UUID> missing = chatRepository.findSessionsWithoutCard(properties.botId(), now).stream().map(StudySession::getId);
        Stream<UUID> outOfDate = cardRepository.findUnfinished(properties.botId()).stream()
                .filter(card -> {
                    StudySession session = card.getStudySession();
                    return CardPhase.of(session.getStatus(), session.getStartsAt(), session.getEndsAt(), now) != card.getShownPhase();
                })
                .map(card -> card.getStudySession().getId());
        return Stream.concat(missing, outOfDate).toList();
    }

    /** Sessions still to start in this group, whose cards list its members. */
    @Transactional(readOnly = true)
    public List<UUID> upcomingSessions(UUID groupId, Instant now) {
        return sessionRepository.findByStudyGroup_IdAndStartsAtAfter(groupId, now).stream().map(StudySession::getId).toList();
    }

    @Transactional(readOnly = true)
    public Optional<Long> messageId(UUID sessionId) {
        return cardRepository.findByStudySession_Id(sessionId).map(SessionChatCard::getMessageId);
    }

    public void recordPost(UUID sessionId, UUID groupChatId, long messageId, Instant now) {
        cardFor(sessionId, groupChatId).recordPost(messageId, now);
    }

    public void recordShown(UUID sessionId, CardPhase phase) {
        cardRepository.findByStudySession_Id(sessionId).ifPresent(card -> card.recordShown(phase));
    }

    public void forgetMessage(UUID sessionId) {
        cardRepository.findByStudySession_Id(sessionId).ifPresent(SessionChatCard::forgetMessage);
    }

    public void startMoved(UUID sessionId, Instant now) {
        cardRepository.findByStudySession_Id(sessionId).ifPresent(card -> card.startMoved(now));
    }

    @Transactional(readOnly = true)
    public PinChange pinChange(UUID groupChatId, Instant now) {
        List<SessionChatCard> cards = cardRepository.findPosted(groupChatId);
        Optional<SessionChatCard> soonest = cards.stream()
                .filter(card -> card.getStudySession().getStatus() == SessionStatus.SCHEDULED && card.getStudySession().getEndsAt().isAfter(now))
                .min(Comparator.comparing(card -> card.getStudySession().getStartsAt()));
        PinnedCard pin = soonest.filter(card -> !card.isPinned()).map(SessionChatStore::pinned).orElse(null);
        List<PinnedCard> unpin = cards.stream()
                .filter(card -> card.isPinned() && soonest.filter(card::equals).isEmpty())
                .map(SessionChatStore::pinned)
                .toList();
        return new PinChange(pin, unpin);
    }

    private static PinnedCard pinned(SessionChatCard card) {
        return new PinnedCard(card.getStudySession().getId(), card.getMessageId());
    }

    public void setPinned(UUID sessionId, boolean pinned) {
        cardRepository.findByStudySession_Id(sessionId).ifPresent(card -> card.setPinned(pinned));
    }

    public void markReminderSent(UUID sessionId, UUID groupChatId, Instant now) {
        cardFor(sessionId, groupChatId).markReminderSent(now);
    }

    public void clearReminder(UUID sessionId) {
        cardRepository.findByStudySession_Id(sessionId).ifPresent(SessionChatCard::clearReminder);
    }

    public void markNudgeSent(UUID sessionId, Instant now) {
        cardRepository.findByStudySession_Id(sessionId).ifPresent(card -> card.markNudgeSent(now));
    }

    public void clearNudge(UUID sessionId) {
        cardRepository.findByStudySession_Id(sessionId).ifPresent(SessionChatCard::clearNudge);
    }

    private SessionChatCard cardFor(UUID sessionId, UUID groupChatId) {
        return cardRepository.findByStudySession_Id(sessionId).orElseGet(() -> cardRepository.save(
                new SessionChatCard(sessionRepository.getReferenceById(sessionId), chatRepository.getReferenceById(groupChatId))));
    }
}
