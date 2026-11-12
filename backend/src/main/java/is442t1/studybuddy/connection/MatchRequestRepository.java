package is442t1.studybuddy.connection;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import is442t1.studybuddy.model.enums.RequestStatus;

/** Persistence access for study buddy requests. */
@Repository
public interface MatchRequestRepository extends JpaRepository<MatchRequest, UUID> {

    /**
     * Finds requests sent or received by a student.
     *
     * @param studentId the student identifier
     * @return the matching requests
     */
    @Query("select r from MatchRequest r where r.sender.id = :studentId or r.receiver.id = :studentId")
    List<MatchRequest> findAllForStudent(@Param("studentId") UUID studentId);

    /**
     * Finds requests received by a student with a given status.
     *
     * @param receiverId the recipient identifier
     * @param status the request status
     * @return the matching requests
     */
    List<MatchRequest> findByReceiver_IdAndStatus(UUID receiverId, RequestStatus status);

    /**
     * Finds requests sent by a student with a given status.
     *
     * @param senderId the sender identifier
     * @param status the request status
     * @return the matching requests
     */
    List<MatchRequest> findBySender_IdAndStatus(UUID senderId, RequestStatus status);

    /**
     * Finds pending requests between two students in either direction.
     *
     * @param firstId the first student identifier
     * @param secondId the second student identifier
     * @return the pending requests
     */
    @Query("""
            select r from MatchRequest r
            where r.status = is442t1.studybuddy.model.enums.RequestStatus.PENDING
              and ((r.sender.id = :firstId and r.receiver.id = :secondId)
                or (r.sender.id = :secondId and r.receiver.id = :firstId))
            """)
    List<MatchRequest> findPendingBetween(@Param("firstId") UUID firstId, @Param("secondId") UUID secondId);
}
