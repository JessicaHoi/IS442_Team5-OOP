package is442t1.studybuddy.admin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<SystemAdministrator, UUID> {
    Optional<SystemAdministrator> findByEmail(String email);
    boolean existsByEmail(String email);
}