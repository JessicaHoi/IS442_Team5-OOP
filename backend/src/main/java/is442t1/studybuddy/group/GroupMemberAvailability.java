package is442t1.studybuddy.group;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import is442t1.studybuddy.model.BaseEntity;
import is442t1.studybuddy.model.TimeSlot;
import is442t1.studybuddy.student.Student;

@Entity
@Table(name = "group_member_availability", uniqueConstraints = @UniqueConstraint(columnNames = {"study_group_id", "student_id"}))
public class GroupMemberAvailability extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_group_id", nullable = false)
    private StudyGroup studyGroup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private UUID revision;

    @ElementCollection
    @CollectionTable(name = "group_member_availability_slot", joinColumns = @JoinColumn(name = "availability_id"))
    private List<TimeSlot> slots = new ArrayList<>();

    protected GroupMemberAvailability() { }

    GroupMemberAvailability(StudyGroup group, Student student, List<TimeSlot> slots) {
        this.studyGroup = group;
        this.student = student;
        replace(slots);
    }

    boolean belongsTo(UUID studentId) { return student.hasId(studentId); }
    public UUID getRevision() { return revision; }
    public List<TimeSlot> getSlots() { return List.copyOf(slots); }

    void replace(List<TimeSlot> slots) {
        if (!slots.isEmpty()) TimeSlot.requireValidAvailability(slots);
        this.slots.clear();
        this.slots.addAll(slots);
        revision = UUID.randomUUID();
    }
}
