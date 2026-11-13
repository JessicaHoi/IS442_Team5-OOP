package is442t1.studybuddy.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence access for student accounts, including StudyGroupLeader subtypes in the joined user
 *     hierarchy.
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {
    Optional<Student> findByEmail(String email);
    boolean existsByEmail(String email);
}