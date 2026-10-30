package is442t1.studybuddy.seed;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import is442t1.studybuddy.connection.MatchRequestRepository;
import is442t1.studybuddy.course.CourseRepository;
import is442t1.studybuddy.group.StudyGroupLeaderRepository;
import is442t1.studybuddy.group.StudyGroupRepository;
import is442t1.studybuddy.matching.MatchingConfigurationRepository;
import is442t1.studybuddy.model.UserRepository;
import is442t1.studybuddy.model.enums.AccountStatus;
import is442t1.studybuddy.model.enums.RequestStatus;
import is442t1.studybuddy.student.StudentRepository;

@SpringBootTest(properties = {
        "studybuddy.seed.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:studybuddy-seed-test;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
class DemoDataSeederTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudyGroupLeaderRepository leaderRepository;

    @Autowired
    private StudyGroupRepository groupRepository;

    @Autowired
    private MatchingConfigurationRepository matchingConfigurationRepository;

    @Autowired
    private MatchRequestRepository matchRequestRepository;

    @Test
    void loadsAtLeastTenCoursesAndFiftyStudents() {
        assertThat(courseRepository.count()).isGreaterThanOrEqualTo(10);
        assertThat(studentRepository.count()).isGreaterThanOrEqualTo(50);
        assertThat(groupRepository.count()).isPositive();
        assertThat(leaderRepository.count()).isPositive();
        assertThat(studentRepository.findByEmail("aisha.rahman@smu.edu.sg")).isPresent();
        assertThat(matchingConfigurationRepository.count()).isEqualTo(1);
    }

    @Test
    void seededAccountsHavePersistedDefaultsAndTheExistingAdministratorName() {
        assertThat(userRepository.findAll()).hasSize(51).allSatisfy(account -> {
            assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
            assertThat(account.getCreatedAt()).isNotNull();
            assertThat(account.getLastLogin()).isNull();
        });
        assertThat(userRepository.findByEmailIgnoreCase("admin@smu.edu.sg")).get()
                .extracting(account -> account.getDisplayName()).isEqualTo("System Administrator");
    }

    @Test
    void answeredDemoRequestsHaveAResponseTime() {
        assertThat(matchRequestRepository.findAll())
                .filteredOn(request -> request.getStatus() != RequestStatus.PENDING)
                .isNotEmpty()
                .allSatisfy(request -> assertThat(request.getRespondedAt()).isNotNull());
    }
}
