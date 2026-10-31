package is442t1.studybuddy.telegram.chat;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

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
import is442t1.studybuddy.telegram.api.ReplyMarkup;
import is442t1.studybuddy.telegram.api.TelegramApiException;
import is442t1.studybuddy.telegram.api.TelegramClient;
import is442t1.studybuddy.telegram.chat.ChatStore.ChatRef;
import is442t1.studybuddy.telegram.chat.SessionChatStore.Responder;
import is442t1.studybuddy.telegram.chat.SessionChatStore.SessionRef;
import is442t1.studybuddy.telegram.chat.SessionMessages.Tagged;

/**
 * Study sessions in the group chat: announcements, Going and Can't buttons, and reminders. Like
 * {@link GroupChatService}, it isn't transactional, so no Telegram call holds a database connection. Posts read the
 * session's current state, because events can arrive after the session has moved on.
 */
@Service
@ConditionalOnProperty(prefix = "studybuddy.telegram", name = "enabled", havingValue = "true")
public class SessionChatService {

    static final String UNKNOWN_PERSON = "StudyBuddy doesn't know this Telegram account. Join the chat from your study group's page, or answer there.";
    static final String BUTTON_NOT_VALID = "This button no longer works.";
    private static final Logger LOG = LoggerFactory.getLogger(SessionChatService.class);

    private final SessionChatStore store;
    private final ChatStore chatStore;
    private final GroupChatService chatService;
    private final StudySessionService sessionService;
    private final TelegramClient client;
    private final ZoneId zone;

    SessionChatService(SessionChatStore store, ChatStore chatStore, GroupChatService chatService, StudySessionService sessionService,
            TelegramClient client, AppProperties appProperties) {
        this.store = store;
        this.chatStore = chatStore;
        this.chatService = chatService;
        this.sessionService = sessionService;
        this.client = client;
        this.zone = appProperties.timeZone();
    }

    void postScheduled(UUID sessionId) {
        postIf(SessionStatus.SCHEDULED, sessionId, SessionMessages::scheduled, SessionMessages.rsvpButtons(sessionId));
    }

    /** Clears the reminder first, so a post that fails can't cost the session its reminder for the new time. */
    void postChanged(UUID sessionId) {
        store.clearReminder(sessionId);
        postIf(SessionStatus.SCHEDULED, sessionId, SessionMessages::changed, SessionMessages.rsvpButtons(sessionId));
    }

    void postCancelled(UUID sessionId) {
        postIf(SessionStatus.CANCELLED, sessionId, SessionMessages::cancelled, new ReplyMarkup.RemoveKeyboard(true));
    }

    private void postIf(SessionStatus expected, UUID sessionId, BiFunction<SessionMessages.Details, ZoneId, String> wording, ReplyMarkup markup) {
        store.read(sessionId).filter(session -> session.status() == expected).ifPresent(session ->
                activeChat(session).ifPresent(chat -> {
                    long messageId = chatService.send(chat, wording.apply(session.details(), zone), markup);
                    store.recordPost(sessionId, chat.id(), messageId, Instant.now());
                    LOG.info("Posted to the chat of study group {} about session {}.", session.groupId(), sessionId);
                }));
    }

    private Optional<ChatRef> activeChat(SessionRef session) {
        return chatStore.findByGroup(session.groupId()).filter(chat -> chat.status() == GroupChatStatus.ACTIVE);
    }

    /** Saves the answer with the same rules as the app, then tells the person how it went. */
    public void buttonPressed(String callbackQueryId, long telegramUserId, String data) {
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
                activeChat(session).ifPresent(chat -> {
                    store.markReminderSent(sessionId, chat.id(), now);
                    ReplyMarkup buttons = SessionMessages.rsvpButtons(sessionId);
                    if (session.details().groupVideoChat()) {
                        Tagged reminder = SessionMessages.videoChatReminder(session.details(), zone, store.goingInChat(sessionId));
                        chatService.send(chat, reminder.text(), reminder.entities(), buttons);
                    } else {
                        chatService.send(chat, SessionMessages.reminder(session.details(), zone), buttons);
                    }
                }));
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
                            chatService.send(chat, notice.text(), notice.entities(), new ReplyMarkup.RemoveKeyboard(true));
                        })));
    }
}
