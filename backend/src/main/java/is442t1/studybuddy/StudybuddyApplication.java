package is442t1.studybuddy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class StudybuddyApplication {

	public static void main(String[] args) {
		SpringApplication.run(StudybuddyApplication.class, args);
	}

}
