package is442t1.studybuddy.telegram.chat;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import is442t1.studybuddy.common.AppProperties;
import is442t1.studybuddy.common.exception.DomainException;
import is442t1.studybuddy.model.enums.SessionStatus;
import is442t1.studybuddy.studysession.RsvpRequest;
import is442t1.studybuddy.studysession.StudySessionResponse;
import is442t1.studybuddy.studysession.StudySessionService;
import is442t1.studybuddy.telegram.api.TelegramApiException;
import is442t1.studybuddy.telegram.api.TelegramClient;
import is442t1.studybuddy.telegram.chat.ChatStore.ChatRef;
import is442t1.studybuddy.telegram.chat.SessionChatStore.Responder;
import is442t1.studybuddy.telegram.chat.SessionChatStore.SessionRef;
import is442t1.studybuddy.telegram.chat.SessionMessages.Tagged;

/**
 * Study sessions in the group chat, apart from the card itself: Going and Can't buttons, reminders and short replies to
 * the card. {@link SessionCardRefresher} looks after the card. Like {@link GroupChatService}, it isn't transactional,
 * so no Telegram call holds a database connection.
 */
@Service
@ConditionalOnProperty(prefix = "studybuddy.telegram", name = "enabled", havingValue = "true")
public class SessionChatService {

    static final String UNKNOWN_PERSON = "StudyBuddy doesn't know this Telegram account. Join the chat from your study group's page, or answer there.";
    static final String BUTTON_NOT_VALID = "This button no longer works.";
    static final String BUTTON_FAILED = "That didn't save. Please try again, or answer in StudyBuddy.";
    private static final Logger LOG = LoggerFactory.getLogger(SessionChatService.class);

    private final SessionChatStore store;
    private final ChatStore chatStore;
    private final ChatSender sender;
    private final SessionCardRefresher refresher;
    private final StudySessionService sessionService;
    private final TelegramClient client;
    private final ZoneId zone;

    SessionChatService(SessionChatStore store, ChatStore chatStore, ChatSender sender, SessionCardRefresher refresher,
            StudySessionService sessionService, TelegramClient client, AppProperties appProperties) {
        this.store = store;
        this.chatStore = chatStore;
        this.sender = sender;
        this.refresher = refresher;
        this.sessionService = sessionService;
        this.client = client;
        this.zone = appProperties.timeZone();
    }

    /** The chat needs to hear about a new time, and the reminder can go out again. Cleared first, so a failed reply can't cost it. */
    void startMoved(UUID sessionId) {
        store.startMoved(sessionId, Instant.now());
        replyToCard(SessionStatus.SCHEDULED, sessionId, session -> SessionMessages.startMoved(session.details(), zone));
    }

    void cancelled(UUID sessionId) {
        replyToCard(SessionStatus.CANCELLED, sessionId, session -> SessionMessages.cancelled(session.details()));
    }

    private void replyToCard(SessionStatus expected, UUID sessionId, Function<SessionRef, String> wording) {
        store.read(sessionId).filter(session -> session.status() == expected).ifPresent(session ->
                activeChat(session).ifPresent(chat -> store.messageId(sessionId).ifPresent(messageId ->
                        sender.reply(chat, messageId, wording.apply(session), List.of()))));
    }

    void refreshUpcoming(UUID groupId) {
        store.upcomingSessions(groupId, Instant.now()).forEach(refresher::markDirty);
    }

    /** Everything that happens because time has passed. */
    public void sweep(Instant now) {
        store.cardsToRefresh(now).forEach(refresher::markDirty);
        sendDueReminders(now);
    }

    private Optional<ChatRef> activeChat(SessionRef session) {
        return chatStore.findByGroup(session.groupId()).filter(chat -> chat.status() == GroupChatStatus.ACTIVE);
    }

    /** Every press gets an answer, even after an unexpected error, or the button keeps spinning. */
    public void buttonPressed(String callbackQueryId, long telegramUserId, String data) {
        try {
            answerPress(callbackQueryId, telegramUserId, data);
        } catch (RuntimeException exception) {
            LOG.warn("Could not handle a button press: {}", exception.getClass().getSimpleName());
            try {
                client.answerButton(callbackQueryId, BUTTON_FAILED, true);
            } catch (TelegramApiException answerFailure) {
                LOG.warn("Could not answer a button press: {}", answerFailure.getMessage());
            }
        }
    }

    /** Saves the answer with the same rules as the app, then tells the person how it went. */
    private void answerPress(String callbackQueryId, long telegramUserId, String data) {
        Optional<RsvpPress> press = RsvpPress.parse(data);
        if (press.isEmpty()) {
            client.answerButton(callbackQueryId, BUTTON_NOT_VALID, true);
            return;
        }
        Optional<Responder> responder = store.responderFor(press.get().sessionId(), telegramUserId);
        if (responder.isEmpty()) {
            client.answerButton(callbackQueryId, UNKNOWN_PERSON, true);
            return;
        }
        try {
            StudySessionResponse saved = sessionService.respond(responder.get().studentId(), responder.get().groupId(),
                    press.get().sessionId(), new RsvpRequest(press.get().response()));
            client.answerButton(callbackQueryId, SessionMessages.answered(press.get().response(), saved.goingCount()), false);
        } catch (DomainException exception) {
            client.answerButton(callbackQueryId, exception.getMessage(), true);
        }
    }

    /**
     * Posts every reminder now due. Each is marked as sent first, so a crash can't send it twice, and unmarked if
     * Telegram refuses, so the next run tries again. One failure doesn't stop the rest.
     */
    public void sendDueReminders(Instant now) {
        for (UUID sessionId : store.dueReminders(now)) {
            try {
                sendReminder(sessionId, now);
                LOG.info("Sent the reminder for session {}.", sessionId);
            } catch (TelegramApiException exception) {
                store.clearReminder(sessionId);
                LOG.warn("Could not send a session reminder: {}", exception.getMessage());
            }
        }
    }

    private void sendReminder(UUID sessionId, Instant now) {
        store.read(sessionId).filter(session -> session.status() == SessionStatus.SCHEDULED).ifPresent(session ->
                activeChat(session).ifPresent(chat -> store.messageId(sessionId).ifPresent(messageId -> {
                    store.markReminderSent(sessionId, chat.id(), now);
                    Tagged reminder = session.details().groupVideoChat()
                            ? SessionMessages.videoChatReminder(session.details(), zone, store.goingInChat(sessionId))
                            : new Tagged(SessionMessages.reminder(session.details(), zone), List.of());
                    sender.reply(chat, messageId, reminder.text(), reminder.entities());
                })));
    }

    /**
     * Someone started a call in a chat. If a video chat session is due, tags the others going. Telegram gives bots no
     * way to start a call or invite anyone to one, so this is as close as StudyBuddy can get.
     */
    public void videoChatStarted(long chatId, Long startedBy) {
        chatStore.findByChat(chatId).filter(chat -> chat.status() == GroupChatStatus.ACTIVE).ifPresent(chat ->
                store.videoChatSession(chat.groupId(), Instant.now()).ifPresent(sessionId ->
                        store.read(sessionId).ifPresent(session -> {
                            List<SessionMessages.Attendee> others = store.goingInChat(sessionId).stream()
                                    .filter(attendee -> startedBy == null || attendee.telegramUserId() != startedBy)
                                    .toList();
                            Tagged notice = SessionMessages.videoChatStarted(session.details().title(), others);
                            sender.send(chat, notice.text(), notice.entities());
                        })));
    }
}
