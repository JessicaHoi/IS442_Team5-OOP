package is442t1.studybuddy.telegram.chat;

import java.time.Instant;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import is442t1.studybuddy.model.BaseEntity;
import is442t1.studybuddy.studysession.StudySession;

/**
 * The message in a group chat that shows one study session, and what the chat has been told about it. It lives here
 * rather than on {@link StudySession} so that sessions know nothing about Telegram. Only changed columns are written,
 * because the reminder job and the card refresher can each change a different column of the same row at about the same time.
 */
@Entity
@Table(name = "session_chat_card")
@DynamicUpdate
public class SessionChatCard extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_session_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private StudySession studySession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_chat_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private GroupChat groupChat;

    /** The card's message in the chat, or null if it hasn't been posted or was deleted. */
    @Column
    private Long messageId;

    /** When the chat last learnt this session's start time, or null if it never did. */
    private Instant announcedAt;

    private Instant reminderSentAt;

    private Instant nudgeSentAt;

    /** The phase the message last showed, so the sweep can tell when time has moved it on. */
    @Enumerated(EnumType.STRING)
    private CardPhase shownPhase;

    @ColumnDefault("false")
    @Column(nullable = false)
    private boolean pinned;

    protected SessionChatCard() {
    }

    SessionChatCard(StudySession studySession, GroupChat groupChat) {
        this.studySession = studySession;
        this.groupChat = groupChat;
    }

    void recordPost(long newMessageId, Instant now) {
        messageId = newMessageId;
        announcedAt = now;
    }

    void recordShown(CardPhase phase) {
        shownPhase = phase;
    }

    /** The message has gone, so the sweep will post a new one. */
    void forgetMessage() {
        messageId = null;
        pinned = false;
    }

    /** A moved session counts as newly announced, and can have a nudge and a reminder for its new time. */
    void startMoved(Instant now) {
        announcedAt = now;
        reminderSentAt = null;
        nudgeSentAt = null;
    }

    /** A session just announced needs no reminder as well, so this says whether that has happened since {@code moment}. */
    boolean announcedAfter(Instant moment) {
        return announcedAt != null && announcedAt.isAfter(moment);
    }

    void markReminderSent(Instant now) {
        reminderSentAt = now;
    }

    /** The send failed, so the next run tries again. */
    void clearReminder() {
        reminderSentAt = null;
    }

    void markNudgeSent(Instant now) {
        nudgeSentAt = now;
    }

    /** The send failed, so the next run tries again. */
    void clearNudge() {
        nudgeSentAt = null;
    }

    void setPinned(boolean nowPinned) {
        pinned = nowPinned;
    }

    public StudySession getStudySession() {
        return studySession;
    }

    public Long getMessageId() {
        return messageId;
    }

    public Instant getAnnouncedAt() {
        return announcedAt;
    }

    public CardPhase getShownPhase() {
        return shownPhase;
    }

    public boolean isPinned() {
        return pinned;
    }
}
