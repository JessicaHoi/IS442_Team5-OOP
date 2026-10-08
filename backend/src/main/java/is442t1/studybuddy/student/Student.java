package is442t1.studybuddy.student;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import is442t1.studybuddy.course.Course;
import is442t1.studybuddy.model.TimeSlot;
import is442t1.studybuddy.model.User;
import is442t1.studybuddy.model.enums.GroupPreference;
import is442t1.studybuddy.model.enums.Role;
import is442t1.studybuddy.model.enums.StudyGoal;
import is442t1.studybuddy.model.enums.StudyMode;

@Entity
@Table(name = "student")
public class Student extends User {

    @Column(nullable = false)
    private String name;

    private String school;
    private String program;
    private Integer yearOfStudy;
    private String contactNum;

    @ManyToMany
    @JoinTable(
            name = "student_course",
            joinColumns = @JoinColumn(name = "student_id"),
            inverseJoinColumns = @JoinColumn(name = "course_code"))
    private Set<Course> coursesTaken = new HashSet<>();

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudyPreference> studyPreferences = new ArrayList<>();

    @Override
    public Role getRole() {
        return Role.STUDENT;
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    public String getName() { 
        return name; 
    }

    public void setName(String name) { 
        this.name = name; 
    }

    public String getSchool() { 
        return school; 
    }

    public void setSchool(String school) { 
        this.school = school; 
    }

    public String getProgram() { 
        return program; 
    }

    public void setProgram(String program) { 
        this.program = program; 
    }

    public Integer getYearOfStudy() { 
        return yearOfStudy; 
    }

    public void setYearOfStudy(Integer yearOfStudy) { 
        this.yearOfStudy = yearOfStudy; 
    }

    public String getContactNum() { 
        return contactNum; 
    }

    public void setContactNum(String contactNum) {
        this.contactNum = contactNum;
    }

    public Set<Course> getCoursesTaken() {
        return Collections.unmodifiableSet(coursesTaken);
    }

    public List<StudyPreference> getStudyPreferences() {
        return Collections.unmodifiableList(studyPreferences);
    }

    public boolean isSameStudent(Student other) {
        return other != null && hasId(other.getId());
    }

    public boolean hasId(UUID id) {
        return getId() != null && getId().equals(id);
    }

    public boolean takesCourse(String courseCode) {
        return coursesTaken.stream().anyMatch(course -> course.getCourseCode().equals(courseCode));
    }

    /**
     * Replaces the courses this student is taking. Study preferences for a
     * course that is no longer taken are removed, since a buddy is only
     * needed for a current course.
     */
    public void replaceCoursesTaken(Collection<Course> courses) {
        coursesTaken.clear();
        coursesTaken.addAll(courses);
        studyPreferences.removeIf(preference -> !takesCourse(preference.getCourse().getCourseCode()));
    }

    public Optional<StudyPreference> findPreferenceFor(String courseCode) {
        return studyPreferences.stream()
                .filter(preference -> preference.getCourse().getCourseCode().equals(courseCode))
                .findFirst();
    }

    /**
     * Creates or updates the study preference for one course. The existing
     * row is updated in place so the (student, course) unique key is kept.
     */
    public StudyPreference saveStudyPreference(Course course, StudyMode studyMode, GroupPreference groupPreference,
            Set<StudyGoal> studyGoals, List<TimeSlot> availability) {
        StudyPreference preference = findPreferenceFor(course.getCourseCode()).orElseGet(() -> {
            StudyPreference created = new StudyPreference(this, course);
            studyPreferences.add(created);
            return created;
        });
        preference.update(studyMode, groupPreference, studyGoals, availability);
        return preference;
    }

    /** Removes every study preference whose course is not in {@code courseCodes}. */
    public void retainPreferencesFor(Set<String> courseCodes) {
        studyPreferences.removeIf(preference -> !courseCodes.contains(preference.getCourse().getCourseCode()));
    }
}
