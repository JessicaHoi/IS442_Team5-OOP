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
import jakarta.persistence.UniqueConstraint;

import is442t1.studybuddy.model.BaseEntity;
import is442t1.studybuddy.student.Student;

/**
 * A student's Telegram account, as seen by one bot. Each teammate runs their own bot against the shared
 * database, so uniqueness is per bot.
 */
@Entity
@Table(name = "telegram_account", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"bot_id", "student_id"}),
        @UniqueConstraint(columnNames = {"bot_id", "telegram_user_id"})
})
public class TelegramAccount extends BaseEntity {

    /** The database removes the row with the student, so deleting an account never has to know about Telegram. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Student student;

    @Column(nullable = false)
    private long botId;

    @Column(nullable = false)
    private long telegramUserId;

    @Column(length = 32)
    private String username;

    private String firstName;

    /** Whether the bot may send this person a private message. Pressing Start allows it. */
    @Column(nullable = false)
    private boolean canMessage;

    @Column(nullable = false)
    private Instant linkedAt;

    protected TelegramAccount() {
    }

    TelegramAccount(Student student, long botId) {
        this.student = student;
        this.botId = botId;
    }

    /** Points this record at the Telegram user who just pressed Start. */
    void linkTo(long telegramUserId, String username, String firstName, Instant now) {
        this.telegramUserId = telegramUserId;
        this.username = username;
        this.firstName = firstName;
        this.canMessage = true;
        this.linkedAt = now;
    }

    /**
     * Returns the StudyBuddy student connected to this Telegram account.
     *
     * @return connected student
     */
    public Student getStudent() {
        return student;
    }

    /**
     * Returns the connected Telegram user identifier.
     *
     * @return Telegram user identifier
     */
    public long getTelegramUserId() {
        return telegramUserId;
    }

    /**
     * Returns the Telegram username, or null if the account has none.
     *
     * @return username, or null
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the Telegram first name recorded at linking.
     *
     * @return recorded first name
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Returns whether the user has opened the bot and can receive its private messages.
     *
     * @return true if the bot may message this account
     */
    public boolean canMessage() {
        return canMessage;
    }

    /**
     * Returns when this Telegram account was most recently linked.
     *
     * @return most recent link time
     */
    public Instant getLinkedAt() {
        return linkedAt;
    }
}
