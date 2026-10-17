package is442t1.studybuddy.connection;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import is442t1.studybuddy.model.enums.RequestStatus;

@Repository
public interface MatchRequestRepository extends JpaRepository<MatchRequest, UUID> {

    List<MatchRequest> findByReceiver_IdAndStatus(UUID receiverId, RequestStatus status);

    List<MatchRequest> findBySender_IdAndStatus(UUID senderId, RequestStatus status);

    @Query("""
            select r from MatchRequest r
            where r.status = is442t1.studybuddy.model.enums.RequestStatus.PENDING
              and ((r.sender.id = :firstId and r.receiver.id = :secondId)
                or (r.sender.id = :secondId and r.receiver.id = :firstId))
            """)
    List<MatchRequest> findPendingBetween(@Param("firstId") UUID firstId, @Param("secondId") UUID secondId);
}
