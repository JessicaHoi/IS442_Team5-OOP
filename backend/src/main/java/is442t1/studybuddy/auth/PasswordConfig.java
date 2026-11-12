package is442t1.studybuddy.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Password hashing configuration. */
@Configuration
public class PasswordConfig {

    /** Creates the password configuration. */
    public PasswordConfig() {
    }

    /**
     * Provides the encoder used to store and check passwords.
     *
     * @return a BCrypt password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
