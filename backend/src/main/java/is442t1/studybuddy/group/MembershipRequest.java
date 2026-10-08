package is442t1.studybuddy.group;

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

import is442t1.studybuddy.common.exception.BusinessRuleException;
import is442t1.studybuddy.model.BaseEntity;
import is442t1.studybuddy.model.enums.RequestStatus;
import is442t1.studybuddy.student.Student;

/** A student's request to join a study group, answered by the group leader. */
@Entity
@Table(name = "membership_request")
public class MembershipRequest extends BaseEntity {

    public static final int MESSAGE_MAX_LENGTH = 300;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_group_id", nullable = false)
    private StudyGroup studyGroup;

    @Column(length = MESSAGE_MAX_LENGTH)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant requestDate;

    protected MembershipRequest() {
        // Required by JPA.
    }

    MembershipRequest(Student student, StudyGroup studyGroup, String message) {
        this.student = student;
        this.studyGroup = studyGroup;
        this.message = message;
    }

    public Student getStudent() {
        return student;
    }

    public StudyGroup getStudyGroup() {
        return studyGroup;
    }

    public String getMessage() {
        return message;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public Instant getRequestDate() {
        return requestDate;
    }

    public boolean isPending() {
        return status == RequestStatus.PENDING;
    }

    void accept() {
        requirePending();
        status = RequestStatus.ACCEPTED;
    }

    void reject() {
        requirePending();
        status = RequestStatus.REJECTED;
    }

    private void requirePending() {
        if (!isPending()) {
            throw new BusinessRuleException("This join request has already been answered.");
        }
    }
}
