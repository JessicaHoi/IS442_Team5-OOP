package is442t1.studybuddy.connection;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StudyBuddyConnectionRepository extends JpaRepository<StudyBuddyConnection, UUID> {

    @Query("""
            select c from StudyBuddyConnection c
            where c.status = is442t1.studybuddy.model.enums.ConnectionStatus.ACTIVE
              and ((c.requester.id = :firstId and c.recipient.id = :secondId)
                or (c.requester.id = :secondId and c.recipient.id = :firstId))
            """)
    Optional<StudyBuddyConnection> findActiveBetween(@Param("firstId") UUID firstId, @Param("secondId") UUID secondId);
}
