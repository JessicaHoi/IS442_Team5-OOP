package is442t1.studybuddy.matching.preview;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings under {@code studybuddy.matching.preview} in application.yaml.
 *
 * @param size how many top matches each side of the preview shows
 */
@ConfigurationProperties(prefix = "studybuddy.matching.preview")
public record PreviewProperties(int size) {
}
