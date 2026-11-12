package is442t1.studybuddy.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Persistence access for courses keyed by course code. */
@Repository
public interface CourseRepository extends JpaRepository<Course, String> {
}