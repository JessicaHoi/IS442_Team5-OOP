package is442t1.studybuddy.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import is442t1.studybuddy.auth.SessionUser;
import is442t1.studybuddy.group.StudyGroupLeader;
import is442t1.studybuddy.model.User;
import is442t1.studybuddy.model.UserRepository;
import is442t1.studybuddy.model.enums.AccountStatus;
import is442t1.studybuddy.model.enums.Role;
import is442t1.studybuddy.student.Student;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserAccountPersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void defaultsApplyToEveryAccountSubclassWithoutRequiringAcademicDetails() {
        Student student = new Student();
        student.setName("New Student");
        StudyGroupLeader leader = new StudyGroupLeader();
        leader.setName("Existing Leader");

        for (User account : new User[] {student, leader, new SystemAdministrator()}) {
            account.setEmail(UUID.randomUUID() + "@test.studybuddy.example");
            account.setPassword("existing-password-hash");
            userRepository.saveAndFlush(account);
            UUID id = account.getId();
            entityManager.clear();

            User saved = userRepository.findById(id).orElseThrow();
            assertThat(saved.getClass()).isEqualTo(account.getClass());
            assertThat(saved.getStatus()).isEqualTo(AccountStatus.ACTIVE);
            assertThat(saved.isActive()).isTrue();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getLastLogin()).isNull();
            assertThat(saved.getPassword()).isEqualTo("existing-password-hash");
        }

        Student savedStudent = (Student) userRepository.findById(student.getId()).orElseThrow();
        assertThat(savedStudent.getSchool()).isNull();
        assertThat(savedStudent.getProgram()).isNull();
        assertThat(savedStudent.getYearOfStudy()).isNull();
        assertThat(savedStudent.getCoursesTaken()).isEmpty();
        assertThat(savedStudent.getStudyPreferences()).isEmpty();
    }

    @Test
    void persistsCustomAdministratorNameStatusAndLoginWithoutChangingCreationTime() {
        SystemAdministrator administrator = new SystemAdministrator();
        administrator.setEmail("  " + UUID.randomUUID() + "@TEST.STUDYBUDDY.EXAMPLE  ");
        administrator.setPassword("unchanged-password-hash");
        administrator.setName("Jane Tan");
        userRepository.saveAndFlush(administrator);
        UUID id = administrator.getId();
        Instant createdAt = administrator.getCreatedAt();
        Instant lastLogin = Instant.parse("2026-10-28T01:00:00Z");
        administrator.setStatus(AccountStatus.SUSPENDED);
        administrator.setLastLogin(lastLogin);
        entityManager.flush();
        entityManager.clear();

        SystemAdministrator saved = (SystemAdministrator) userRepository.findById(id).orElseThrow();
        assertThat(saved.getName()).isEqualTo("Jane Tan");
        assertThat(saved.getDisplayName()).isEqualTo("Jane Tan");
        assertThat(SessionUser.from(saved).name()).isEqualTo("Jane Tan");
        assertThat(saved.getRole()).isEqualTo(Role.ADMIN);
        assertThat(saved.isActive()).isFalse();
        assertThat(saved.getStatus()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(saved.getLastLogin()).isEqualTo(lastLogin);
        assertThat(saved.getCreatedAt()).isEqualTo(createdAt);
        assertThat(saved.getPassword()).isEqualTo("unchanged-password-hash");
        assertThat(saved.getEmail()).endsWith("@test.studybuddy.example").isEqualTo(saved.getEmail().trim());

        saved.setStatus(AccountStatus.ACTIVE);
        entityManager.flush();
        entityManager.clear();
        assertThat(userRepository.findById(id).orElseThrow().isActive()).isTrue();
    }
}
