package is442t1.studybuddy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Entry point for the Study Buddy Matcher backend. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class StudybuddyApplication {

	/** Creates the application configuration. */
	public StudybuddyApplication() {
	}

	/**
	 * Starts the Spring Boot application.
	 *
	 * @param args the application arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(StudybuddyApplication.class, args);
	}

}
