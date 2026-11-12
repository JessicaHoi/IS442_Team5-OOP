package is442t1.studybuddy.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A course in the shared catalogue. */
@Entity
@Table(name = "course")
public class Course {

    /** Creates a course. */
    public Course() {
    }

    @Id 
    @Column(nullable = false)
    private String courseCode;
    private String courseName;

    /** School offering the course, e.g. "School of Computing and Information Systems". */
    private String school;

    /**
     * Returns the course name.
     *
     * @return the course name
     */
    public String getCourseName() { 
        return this.courseName; 
    }

    /**
     * Sets the course name.
     *
     * @param courseName the course name
     */
    public void setCourseName(String courseName) { 
        this.courseName = courseName; 
    }

    /**
     * Returns the course code.
     *
     * @return the course code
     */
    public String getCourseCode() {
        return this.courseCode;
    }

    /**
     * Sets the course code.
     *
     * @param courseCode the course code
     */
    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    /**
     * Returns the offering school.
     *
     * @return the offering school
     */
    public String getSchool() {
        return school;
    }

    /**
     * Sets the offering school.
     *
     * @param school the offering school
     */
    public void setSchool(String school) {
        this.school = school;
    }
}
