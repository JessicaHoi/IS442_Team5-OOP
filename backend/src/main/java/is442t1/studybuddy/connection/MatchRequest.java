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
import is442t1.studybuddy.model.enums.RequestStatus;
import is442t1.studybuddy.student.Student;

/** A request to form a study buddy connection. */
@Entity
@Table(name = "match_request")
public class MatchRequest extends BaseEntity {

    /** Creates a pending buddy request. */
    public MatchRequest() {
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private Student sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private Student receiver;

    @Column(length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant requestDate;

    private Instant respondedAt;

    /**
     * Returns the requesting student.
     *
     * @return the requesting student
     */
    public Student getSender() {
        return sender;
    }

    /**
     * Sets the requesting student.
     *
     * @param sender the requesting student
     */
    public void setSender(Student sender) {
        this.sender = sender;
    }

    /**
     * Returns the recipient student.
     *
     * @return the recipient student
     */
    public Student getReceiver() {
        return receiver;
    }

    /**
     * Sets the recipient student.
     *
     * @param receiver the recipient student
     */
    public void setReceiver(Student receiver) {
        this.receiver = receiver;
    }

    /**
     * Returns the request message.
     *
     * @return the request message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the request message.
     *
     * @param message the request message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Returns the request status.
     *
     * @return the request status
     */
    public RequestStatus getStatus() {
        return status;
    }

    /**
     * Sets the request status.
     *
     * @param status the request status
     */
    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    /**
     * Returns the request creation time.
     *
     * @return the request creation time
     */
    public Instant getRequestDate() {
        return requestDate;
    }

    /**
     * Returns the response time, if answered.
     *
     * @return the response time, if answered
     */
    public Instant getRespondedAt() {
        return respondedAt;
    }

    /**
     * Sets the response time.
     *
     * @param respondedAt the response time
     */
    public void setRespondedAt(Instant respondedAt) {
        this.respondedAt = respondedAt;
    }
}
