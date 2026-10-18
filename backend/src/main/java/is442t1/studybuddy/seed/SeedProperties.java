package is442t1.studybuddy.seed;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings under {@code studybuddy.seed} in application.yaml.
 *
 * @param enabled                 whether demo seeding is enabled
 * @param addStudentsToExisting   whether missing demo students may be added to a non-empty database
 * @param location                resource holding the demo data
 * @param demoPassword            password given to every seeded account
 */
@ConfigurationProperties(prefix = "studybuddy.seed")
public record SeedProperties(boolean enabled, boolean addStudentsToExisting, String location, String demoPassword) {
}
