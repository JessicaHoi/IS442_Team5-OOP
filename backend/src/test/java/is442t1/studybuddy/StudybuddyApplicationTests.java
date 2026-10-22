package is442t1.studybuddy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Uses the in-memory test database, so running the tests never touches the developer's ./data file. */
@SpringBootTest
@ActiveProfiles("test")
class StudybuddyApplicationTests {

	@Test
	void contextLoads() {
	}

}
