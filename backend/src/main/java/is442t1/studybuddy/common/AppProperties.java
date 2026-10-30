package is442t1.studybuddy.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings under {@code studybuddy.app} in application.yaml.
 *
 * @param url where students open the app, used for links in calendar files; blank leaves the links out
 */
@ConfigurationProperties(prefix = "studybuddy.app")
public record AppProperties(String url) {
}
