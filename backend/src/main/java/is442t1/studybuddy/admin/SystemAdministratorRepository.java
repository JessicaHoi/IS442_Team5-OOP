package is442t1.studybuddy.admin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Persistence access for administrator accounts. */
@Repository
public interface SystemAdministratorRepository extends JpaRepository<SystemAdministrator, UUID> {
    /**
     * Finds an administrator by email.
     *
     * @param email the email address
     * @return the matching account, if present
     */
    Optional<SystemAdministrator> findByEmail(String email);
    /**
     * Checks for an administrator with the supplied email.
     *
     * @param email the email address
     * @return whether an account exists
     */
    boolean existsByEmail(String email);
}