package is442t1.studybuddy.student;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import is442t1.studybuddy.course.Course;
import is442t1.studybuddy.model.User;

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
}