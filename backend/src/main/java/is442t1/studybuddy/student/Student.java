package is442t1.studybuddy.student;

import is442t1.studybuddy.model.User;

import java.util.ArrayList;

import is442t1.studybuddy.course.Course;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "student")
public class Student extends User {

    @Column(nullable = false)
    private String name;

    private String school;
    private String program;
    private Integer yearOfStudy;
    private String contactNum;
    private ArrayList<Course> coursesTaken;

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
}