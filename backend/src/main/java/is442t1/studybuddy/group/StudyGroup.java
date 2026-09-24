package is442t1.studybuddy.group;

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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import is442t1.studybuddy.course.Course;
import is442t1.studybuddy.model.BaseEntity;
import is442t1.studybuddy.model.TimeSlot;
import is442t1.studybuddy.model.enums.StudyGoal;
import is442t1.studybuddy.model.enums.StudyMode;
import is442t1.studybuddy.student.Student;

@Entity
@Table(name = "study_group")
public class StudyGroup extends BaseEntity {

    @Column(nullable = false)
    private String groupName;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_code", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leader_id", nullable = false)
    private Student leader;

    @ManyToMany
    @JoinTable(
            name = "study_group_member",
            joinColumns = @JoinColumn(name = "study_group_id"),
            inverseJoinColumns = @JoinColumn(name = "student_id"))
    private Set<Student> members = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "study_group_goal", joinColumns = @JoinColumn(name = "study_group_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "study_goal", nullable = false)
    private Set<StudyGoal> studyGoals = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private StudyMode studyMode;

    @ElementCollection
    @CollectionTable(name = "study_group_availability", joinColumns = @JoinColumn(name = "study_group_id"))
    private List<TimeSlot> availability = new ArrayList<>();

    @Column(nullable = false)
    private int maxSize;

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public Student getLeader() {
        return leader;
    }

    public void setLeader(Student leader) {
        this.leader = leader;
    }

    public Set<Student> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public Set<StudyGoal> getStudyGoals() {
        return Collections.unmodifiableSet(studyGoals);
    }

    public StudyMode getStudyMode() {
        return studyMode;
    }

    public void setStudyMode(StudyMode studyMode) {
        this.studyMode = studyMode;
    }

    public List<TimeSlot> getAvailability() {
        return Collections.unmodifiableList(availability);
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }
}
