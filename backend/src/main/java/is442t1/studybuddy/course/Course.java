package is442t1.studybuddy.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "course")
public class Course {

    @Id 
    @Column(nullable = false)
    private String courseCode;
    private String courseName;

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
}