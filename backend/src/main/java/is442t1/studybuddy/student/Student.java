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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import is442t1.studybuddy.course.Course;
import is442t1.studybuddy.model.TimeSlot;
import is442t1.studybuddy.model.User;
import is442t1.studybuddy.model.enums.GroupPreference;
import is442t1.studybuddy.model.enums.Role;
import is442t1.studybuddy.model.enums.StudyGoal;
import is442t1.studybuddy.model.enums.StudyMode;

/** A student account and its course-specific study preferences. */
@Entity
@Table(name = "student")
public class Student extends User {

    /** Creates a student account. */
    public Student() {
    }

    @Column(nullable = false)
    private String name;

    private String school;
    private String program;
    private Integer yearOfStudy;
    private String contactNum;

    /** Bare username without the leading {@code @}. Follows the same privacy rule as {@link #contactNum}. */
    @Column(length = 32)
    private String telegramHandle;

    @ManyToMany
    @JoinTable(
            name = "student_course",
            joinColumns = @JoinColumn(name = "student_id"),
            inverseJoinColumns = @JoinColumn(name = "course_code"))
    private Set<Course> coursesTaken = new HashSet<>();

    /** Loaded in course-code order, so "the first preference" is the same on every database. */
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("course.courseCode")
    private List<StudyPreference> studyPreferences = new ArrayList<>();

    @Override
    public Role getRole() {
        return Role.STUDENT;
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    /**
     * Returns the student's name.
     *
     * @return the student's name
     */
    public String getName() { 
        return name; 
    }

    /**
     * Sets the student's name.
     *
     * @param name the student's name
     */
    public void setName(String name) { 
        this.name = name; 
    }

    /**
     * Returns the student's school.
     *
     * @return the student's school
     */
    public String getSchool() { 
        return school; 
    }

    /**
     * Sets the student's school.
     *
     * @param school the student's school
     */
    public void setSchool(String school) { 
        this.school = school; 
    }

    /**
     * Returns the student's programme.
     *
     * @return the student's programme
     */
    public String getProgram() { 
        return program; 
    }

    /**
     * Sets the student's programme.
     *
     * @param program the student's programme
     */
    public void setProgram(String program) { 
        this.program = program; 
    }

    /**
     * Returns the year of study.
     *
     * @return the year of study
     */
    public Integer getYearOfStudy() { 
        return yearOfStudy; 
    }

    /**
     * Sets the year of study.
     *
     * @param yearOfStudy the year of study
     */
    public void setYearOfStudy(Integer yearOfStudy) { 
        this.yearOfStudy = yearOfStudy; 
    }

    /**
     * Returns the student's contact number.
     *
     * @return the student's contact number
     */
    public String getContactNum() { 
        return contactNum; 
    }

    /**
     * Sets the student's contact number.
     *
     * @param contactNum the student's contact number
     */
    public void setContactNum(String contactNum) {
        this.contactNum = contactNum;
    }

    /**
     * Returns the student's Telegram handle.
     *
     * @return the student's Telegram handle
     */
    public String getTelegramHandle() {
        return telegramHandle;
    }

    /**
     * Sets the student's Telegram handle.
     *
     * @param telegramHandle the student's Telegram handle
     */
    public void setTelegramHandle(String telegramHandle) {
        this.telegramHandle = telegramHandle;
    }

    /**
     * Returns an unmodifiable view of the courses taken.
     *
     * @return an unmodifiable view of the courses taken
     */
    public Set<Course> getCoursesTaken() {
        return Collections.unmodifiableSet(coursesTaken);
    }

    /**
     * Returns an unmodifiable view of the study preferences.
     *
     * @return an unmodifiable view of the study preferences
     */
    public List<StudyPreference> getStudyPreferences() {
        return Collections.unmodifiableList(studyPreferences);
    }

    /**
     * Compares student identifiers.
     *
     * @param other the student to compare
     * @return whether both students have the same identifier
     */
    public boolean isSameStudent(Student other) {
        return other != null && hasId(other.getId());
    }

    /**
     * Checks this student's identifier.
     *
     * @param id the identifier to compare
     * @return whether this student has the identifier
     */
    public boolean hasId(UUID id) {
        return getId() != null && getId().equals(id);
    }

    /**
     * Checks whether the student takes a course.
     *
     * @param courseCode the course code
     * @return whether the course is in the student's courses
     */
    public boolean takesCourse(String courseCode) {
        return coursesTaken.stream().anyMatch(course -> course.getCourseCode().equals(courseCode));
    }

    /**
     * Replaces the courses this student is taking. Study preferences for a
     * course that is no longer taken are removed, since a buddy is only
     * needed for a current course.
     *
     * @param courses the replacement courses
     */
    public void replaceCoursesTaken(Collection<Course> courses) {
        coursesTaken.clear();
        coursesTaken.addAll(courses);
        studyPreferences.removeIf(preference -> !takesCourse(preference.getCourse().getCourseCode()));
    }

    /**
     * Finds the preference for a course.
     *
     * @param courseCode the course code
     * @return the preference, if present
     */
    public Optional<StudyPreference> findPreferenceFor(String courseCode) {
        return studyPreferences.stream()
                .filter(preference -> preference.getCourse().getCourseCode().equals(courseCode))
                .findFirst();
    }
    /**
     * The weekly availability this student gave for one course, or an empty
     * list if they have not set a study preference for it.
     *
     * @param courseCode the course code
     * @return the course availability, or an empty list
     */
    public List<TimeSlot> availabilityFor(String courseCode) {
        return findPreferenceFor(courseCode)
                .map(StudyPreference::getAvailability)
                .orElse(List.of());
    }

    /**
     * Creates or updates the study preference for one course. The existing
     * row is updated in place so the (student, course) unique key is kept.
     *
     * @param course the course
     * @param studyMode the preferred meeting mode
     * @param groupPreference the preferred group format
     * @param studyGoals the study goals
     * @param availability the weekly availability
     * @return the saved preference
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

    /**
     * Removes preferences whose course is not in the supplied set.
     *
     * @param courseCodes the course codes whose preferences should be retained
     */
    public void retainPreferencesFor(Set<String> courseCodes) {
        studyPreferences.removeIf(preference -> !courseCodes.contains(preference.getCourse().getCourseCode()));
    }
}
