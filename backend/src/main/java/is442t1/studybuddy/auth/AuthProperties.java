package is442t1.studybuddy.auth;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings under {@code studybuddy.auth} in application.yaml.
 *
 * @param tokenTtl how long a sign-in token stays valid
 */
@ConfigurationProperties(prefix = "studybuddy.auth")
public record AuthProperties(Duration tokenTtl) {
}
