package is442t1.studybuddy.seed;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.ActiveProfiles;

import is442t1.studybuddy.model.enums.AccountStatus;
import is442t1.studybuddy.student.Student;
import is442t1.studybuddy.student.StudentRepository;

@SpringBootTest(properties = {
        "studybuddy.seed.enabled=true",
        "studybuddy.seed.add-students-to-existing=true",
        "spring.datasource.url=jdbc:h2:mem:studybuddy-existing-seed-test;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
@Import(ExistingDatabaseDemoStudentsTest.ExistingAccountConfiguration.class)
class ExistingDatabaseDemoStudentsTest {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private DemoDataSeeder demoDataSeeder;

    @Test
    void addsMissingDemoStudentsWithoutReplacingExistingAccountsAndIsIdempotent() throws Exception {
        Student existingStudent = studentRepository.findByEmail("local.student@test.smu.edu.sg").orElseThrow();
        assertThat(existingStudent.getName()).isEqualTo("Existing Local Student");
        assertThat(existingStudent.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(existingStudent.getCreatedAt()).isNotNull();
        assertThat(existingStudent.getLastLogin()).isNull();
        assertThat(studentRepository.count()).isGreaterThanOrEqualTo(50);
        assertThat(studentRepository.findByEmail("aisha.rahman@smu.edu.sg")).isPresent();

        long countAfterInitialMerge = studentRepository.count();
        ApplicationArguments args = new DefaultApplicationArguments(new String[0]);
        demoDataSeeder.run(args);

        assertThat(studentRepository.count()).isEqualTo(countAfterInitialMerge);
        assertThat(studentRepository.findByEmail("local.student@test.smu.edu.sg"))
                .get()
                .extracting(Student::getName)
                .isEqualTo("Existing Local Student");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ExistingAccountConfiguration {

        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE)
        ApplicationRunner createExistingAccount(StudentRepository studentRepository) {
            return (args) -> {
                if (studentRepository.findByEmail("local.student@test.smu.edu.sg").isEmpty()) {
                    Student existingStudent = new Student();
                    existingStudent.setEmail("local.student@test.smu.edu.sg");
                    existingStudent.setPassword("already-hashed");
                    existingStudent.setName("Existing Local Student");
                    studentRepository.save(existingStudent);
                }
            };
        }
    }
}
