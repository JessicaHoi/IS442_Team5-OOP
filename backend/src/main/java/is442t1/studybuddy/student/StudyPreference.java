package is442t1.studybuddy.student;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import is442t1.studybuddy.course.Course;
import is442t1.studybuddy.model.BaseEntity;
import is442t1.studybuddy.model.TimeSlot;
import is442t1.studybuddy.model.enums.GroupPreference;
import is442t1.studybuddy.model.enums.StudyGoal;
import is442t1.studybuddy.model.enums.StudyMode;

@Entity
@Table(
        name = "study_preference",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "course_code"})
    )
public class StudyPreference extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_code", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    private StudyMode studyMode;

    @Enumerated(EnumType.STRING)
    private GroupPreference groupPreference;

    @ElementCollection
    @CollectionTable(name = "study_preference_availability", joinColumns = @JoinColumn(name = "study_preference_id"))
    private List<TimeSlot> availability = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "study_preference_goal", joinColumns = @JoinColumn(name = "study_preference_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "study_goal", nullable = false)
    private Set<StudyGoal> studyGoals = new HashSet<>();

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public StudyMode getStudyMode() {
        return studyMode;
    }

    public void setStudyMode(StudyMode studyMode) {
        this.studyMode = studyMode;
    }

    public GroupPreference getGroupPreference() {
        return groupPreference;
    }

    public void setGroupPreference(GroupPreference groupPreference) {
        this.groupPreference = groupPreference;
    }

    public List<TimeSlot> getAvailability() {
        return Collections.unmodifiableList(availability);
    }

    public Set<StudyGoal> getStudyGoals() {
        return Collections.unmodifiableSet(studyGoals);
    }
}
