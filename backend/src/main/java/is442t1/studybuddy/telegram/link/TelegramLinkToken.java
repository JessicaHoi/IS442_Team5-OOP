package is442t1.studybuddy.telegram.link;

import java.time.Instant;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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

    protected TelegramLinkToken() {
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
