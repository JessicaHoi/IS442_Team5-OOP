package is442t1.studybuddy.connection;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchRequestRepository extends JpaRepository<MatchRequest, UUID> {
}
