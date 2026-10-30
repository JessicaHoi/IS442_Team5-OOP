package is442t1.studybuddy.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import is442t1.studybuddy.model.UserRepository;
import is442t1.studybuddy.model.enums.AccountStatus;

/** Verifies that Hibernate adds account fields to an existing H2 schema without losing account data. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:studybuddy-account-upgrade-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:legacy-account-schema.sql"
})
@ActiveProfiles("test")
class UserAccountSchemaUpdateTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void backfillsExistingAdministratorWithoutReplacingIdentityOrPassword() {
        SystemAdministrator administrator = (SystemAdministrator) userRepository.findById(
                UUID.fromString("00000000-0000-0000-0000-000000000001")).orElseThrow();

        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(administrator.getEmail()).isEqualTo("legacy.admin@test.smu.edu.sg");
        assertThat(administrator.getPassword()).isEqualTo("legacy-password-hash");
        assertThat(administrator.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(administrator.getCreatedAt()).isNotNull();
        assertThat(administrator.getLastLogin()).isNull();
        assertThat(administrator.getDisplayName()).isEqualTo("System Administrator");
    }
}
