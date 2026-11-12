package is442t1.studybuddy.matching;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Persistence access for matching configurations. */
@Repository
public interface MatchingConfigurationRepository extends JpaRepository<MatchingConfiguration, UUID> {
}
