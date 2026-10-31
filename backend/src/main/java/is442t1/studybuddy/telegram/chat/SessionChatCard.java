package is442t1.studybuddy.telegram.chat;

import java.time.Instant;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import is442t1.studybuddy.model.BaseEntity;
import is442t1.studybuddy.studysession.StudySession;

/**
 * What the group chat has been told about one study session. It lives here rather than on {@link StudySession} so that
 * sessions know nothing about Telegram. Only changed columns are written, because the reminder job and the background
 * posts can each change a different column of the same row at about the same time.
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

    /** The chat message about this session, or null if only a reminder has gone out so far. */
    @Column
    private Long messageId;

    /** When the chat was last told this session's details, or null if it never was. */
    private Instant announcedAt;

    private Instant reminderSentAt;

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

    /** A session just announced needs no reminder as well, so this says whether that has happened since {@code moment}. */
    boolean announcedAfter(Instant moment) {
        return announcedAt != null && announcedAt.isAfter(moment);
    }

    void markReminderSent(Instant now) {
        reminderSentAt = now;
    }

    /** The session changed, or the send failed, so a reminder can go out again. */
    void clearReminder() {
        reminderSentAt = null;
    }
}
