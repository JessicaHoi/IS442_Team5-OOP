package is442t1.studybuddy.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Shared catalogue entry identified by course code, with a course name and optional school.
 *     Students take courses and may set one study preference per course.
 */
@Entity
@Table(name = "course")
public class Course {

    @Id 
    @Column(nullable = false)
    private String courseCode;
    private String courseName;

    /** School offering the course, e.g. "School of Computing and Information Systems". */
    private String school;

    public String getCourseName() { 
        return this.courseName; 
    }

    public void setCourseName(String courseName) { 
        this.courseName = courseName; 
    }

    public String getCourseCode() {
        return this.courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }
}
