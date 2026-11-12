package is442t1.studybuddy.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Persistence access for student accounts. */
@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {
    /**
     * Finds a student by email.
     *
     * @param email the email address
     * @return the matching student, if present
     */
    Optional<Student> findByEmail(String email);
    /**
     * Checks whether a student has the supplied email.
     *
     * @param email the email address
     * @return whether the student exists
     */
    boolean existsByEmail(String email);
}