package is442t1.studybuddy.seed;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import is442t1.studybuddy.course.CourseRepository;
import is442t1.studybuddy.group.StudyGroupLeaderRepository;
import is442t1.studybuddy.group.StudyGroupRepository;
import is442t1.studybuddy.student.StudentRepository;

@SpringBootTest(properties = {
        "studybuddy.seed.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:studybuddy-seed-test;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
class DemoDataSeederTest {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudyGroupLeaderRepository leaderRepository;

    @Autowired
    private StudyGroupRepository groupRepository;

    @Test
    void loadsAtLeastTenCoursesAndFiftyStudents() {
        assertThat(courseRepository.count()).isGreaterThanOrEqualTo(10);
        assertThat(studentRepository.count()).isGreaterThanOrEqualTo(50);
        assertThat(groupRepository.count()).isPositive();
        assertThat(leaderRepository.count()).isPositive();
        assertThat(studentRepository.findByEmail("aisha.rahman@smu.edu.sg")).isPresent();
    }
}
