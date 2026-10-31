package is442t1.studybuddy.telegram.link;

import java.time.Instant;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import is442t1.studybuddy.group.StudyGroup;
import is442t1.studybuddy.model.BaseEntity;
import is442t1.studybuddy.student.Student;

/**
 * A short-lived, single-use proof that a student asked to connect Telegram. Only the hash is stored, so reading
 * the table doesn't reveal a working link. Kept in the database because instances share it.
 */
@Entity
@Table(name = "telegram_link_token")
public class TelegramLinkToken extends BaseEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Student student;

    @Column(nullable = false)
    private long botId;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant usedAt;

    /** What the token is for. Rows from before group chats default to connecting an account. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ColumnDefault("'ACCOUNT'")
    private TelegramLinkPurpose purpose = TelegramLinkPurpose.ACCOUNT;

    /** The group being set up. Only for {@link TelegramLinkPurpose#GROUP_SETUP}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_group_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private StudyGroup studyGroup;

    /** The first of the two ids on the setup buttons, tying the bot's reply back to this token. */
    private Integer requestId;

    /** The Telegram user who opened a setup link. Only they can finish it. */
    private Long claimedTelegramUserId;

    protected TelegramLinkToken() {
    }

    TelegramLinkToken(String tokenHash, Student student, long botId, Instant expiresAt, StudyGroup studyGroup) {
        this(tokenHash, student, botId, expiresAt);
        this.purpose = TelegramLinkPurpose.GROUP_SETUP;
        this.studyGroup = studyGroup;
    }

    /** Ties a setup token to the first Telegram user to open it. Returns false if someone else already has. */
    boolean claimFor(long telegramUserId, int newRequestId) {
        if (claimedTelegramUserId != null) {
            return claimedTelegramUserId == telegramUserId;
        }
        claimedTelegramUserId = telegramUserId;
        requestId = newRequestId;
        return true;
    }

    boolean isUsable(Instant now) {
        return usedAt == null && expiresAt.isAfter(now);
    }

    void markUsed(Instant now) {
        usedAt = now;
    }

    public StudyGroup getStudyGroup() {
        return studyGroup;
    }

    public Integer getRequestId() {
        return requestId;
    }

    TelegramLinkToken(String tokenHash, Student student, long botId, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.student = student;
        this.botId = botId;
        this.expiresAt = expiresAt;
    }

    public Student getStudent() {
        return student;
    }
}
