package is442t1.studybuddy.connection;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import is442t1.studybuddy.model.BaseEntity;
import is442t1.studybuddy.model.enums.ConnectionStatus;
import is442t1.studybuddy.student.Student;

/** A connection between two study buddies. */
@Entity
@Table(name = "study_buddy_connection")
public class StudyBuddyConnection extends BaseEntity {

    /** Creates an active buddy connection. */
    public StudyBuddyConnection() {
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private Student requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private Student recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConnectionStatus status = ConnectionStatus.ACTIVE;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant startDate;

    /**
     * Returns the student who requested the connection.
     *
     * @return the student who requested the connection
     */
    public Student getRequester() {
        return requester;
    }

    /**
     * Sets the student who requested the connection.
     *
     * @param requester the student who requested the connection
     */
    public void setRequester(Student requester) {
        this.requester = requester;
    }

    /**
     * Returns the student who received the request.
     *
     * @return the student who received the request
     */
    public Student getRecipient() {
        return recipient;
    }

    /**
     * Sets the student who received the request.
     *
     * @param recipient the student who received the request
     */
    public void setRecipient(Student recipient) {
        this.recipient = recipient;
    }

    /**
     * Returns the connection status.
     *
     * @return the connection status
     */
    public ConnectionStatus getStatus() {
        return status;
    }

    /**
     * Sets the connection status.
     *
     * @param status the connection status
     */
    public void setStatus(ConnectionStatus status) {
        this.status = status;
    }

    /**
     * Returns the connection creation time.
     *
     * @return the connection creation time
     */
    public Instant getStartDate() {
        return startDate;
    }

}
