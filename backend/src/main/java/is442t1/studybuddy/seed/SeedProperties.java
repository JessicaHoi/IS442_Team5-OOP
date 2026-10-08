package is442t1.studybuddy.seed;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings under {@code studybuddy.seed} in application.yaml.
 *
 * @param enabled      whether to load the demo data into an empty database
 * @param location     resource holding the demo data
 * @param demoPassword password given to every seeded account
 */
@ConfigurationProperties(prefix = "studybuddy.seed")
public record SeedProperties(boolean enabled, String location, String demoPassword) {
}
