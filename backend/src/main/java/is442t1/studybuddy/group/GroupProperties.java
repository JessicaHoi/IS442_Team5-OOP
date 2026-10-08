package is442t1.studybuddy.group;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings under {@code studybuddy.group} in application.yaml.
 *
 * @param minSize smallest maximum size a leader may choose
 * @param maxSize largest maximum size a leader may choose
 */
@ConfigurationProperties(prefix = "studybuddy.group")
public record GroupProperties(int minSize, int maxSize) {
}
