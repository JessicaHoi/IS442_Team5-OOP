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

/** A student's study preferences for one course. */
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

    protected StudyPreference() {
        // Required by JPA.
    }

    StudyPreference(Student student, Course course) {
        this.student = student;
        this.course = course;
    }

    /**
     * Returns the student who owns the preference.
     *
     * @return the student who owns the preference
     */
    public Student getStudent() {
        return student;
    }

    /**
     * Sets the student who owns the preference.
     *
     * @param student the student who owns the preference
     */
    public void setStudent(Student student) {
        this.student = student;
    }

    /**
     * Returns the course.
     *
     * @return the course
     */
    public Course getCourse() {
        return course;
    }

    /**
     * Sets the course.
     *
     * @param course the course
     */
    public void setCourse(Course course) {
        this.course = course;
    }

    /**
     * Returns the preferred meeting mode.
     *
     * @return the preferred meeting mode
     */
    public StudyMode getStudyMode() {
        return studyMode;
    }

    /**
     * Sets the preferred meeting mode.
     *
     * @param studyMode the preferred meeting mode
     */
    public void setStudyMode(StudyMode studyMode) {
        this.studyMode = studyMode;
    }

    /**
     * Returns the preferred group format.
     *
     * @return the preferred group format
     */
    public GroupPreference getGroupPreference() {
        return groupPreference;
    }

    /**
     * Sets the preferred group format.
     *
     * @param groupPreference the preferred group format
     */
    public void setGroupPreference(GroupPreference groupPreference) {
        this.groupPreference = groupPreference;
    }

    /**
     * Returns an unmodifiable view of the weekly availability.
     *
     * @return an unmodifiable view of the weekly availability
     */
    public List<TimeSlot> getAvailability() {
        return Collections.unmodifiableList(availability);
    }

    /**
     * Returns an unmodifiable view of the study goals.
     *
     * @return an unmodifiable view of the study goals
     */
    public Set<StudyGoal> getStudyGoals() {
        return Collections.unmodifiableSet(studyGoals);
    }

    /** Replaces every editable field of this preference. Use {@link Student#saveStudyPreference} to call it. */
    void update(StudyMode studyMode, GroupPreference groupPreference, Set<StudyGoal> studyGoals,
            List<TimeSlot> availability) {
        TimeSlot.requireValidAvailability(availability);
        this.studyMode = studyMode;
        this.groupPreference = groupPreference;
        this.studyGoals.clear();
        this.studyGoals.addAll(studyGoals);
        this.availability.clear();
        this.availability.addAll(availability);
    }
}
