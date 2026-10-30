package is442t1.studybuddy.studysession;

import java.time.Instant;

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
import jakarta.persistence.UniqueConstraint;

import is442t1.studybuddy.model.BaseEntity;
import is442t1.studybuddy.model.enums.RsvpResponse;
import is442t1.studybuddy.student.Student;

/** One student's answer to one study session. */
@Entity
@Table(name = "session_rsvp", uniqueConstraints = @UniqueConstraint(columnNames = {"study_session_id", "student_id"}))
public class SessionRsvp extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_session_id", nullable = false)
    private StudySession session;

    /** The database removes the row with the student, so deleting an account never has to know about RSVPs. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RsvpResponse response;

    @Column(nullable = false)
    private Instant respondedAt;

    protected SessionRsvp() {
    }

    SessionRsvp(StudySession session, Student student, RsvpResponse response, Instant respondedAt) {
        this.session = session;
        this.student = student;
        this.response = response;
        this.respondedAt = respondedAt;
    }

    public Student getStudent() {
        return student;
    }

    public RsvpResponse getResponse() {
        return response;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }

    void change(RsvpResponse newResponse, Instant at) {
        response = newResponse;
        respondedAt = at;
    }
}
