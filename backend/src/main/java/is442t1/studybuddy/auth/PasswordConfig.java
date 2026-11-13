package is442t1.studybuddy.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Shared BCrypt password encoder configuration for account creation, password changes and
 *     credential checks.
 */
@Configuration
public class PasswordConfig {

    /** Use this bean whenever a password is stored or checked. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
