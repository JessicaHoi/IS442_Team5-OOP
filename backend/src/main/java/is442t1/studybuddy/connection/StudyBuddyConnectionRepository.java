package is442t1.studybuddy.connection;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import is442t1.studybuddy.model.AccountUsageCount;

/** Persistence access for study buddy connections. */
@Repository
public interface StudyBuddyConnectionRepository extends JpaRepository<StudyBuddyConnection, UUID> {

    /**
     * Finds all connections involving a student.
     *
     * @param studentId the student identifier
     * @return the matching connections
     */
    @Query("select c from StudyBuddyConnection c where c.requester.id = :studentId or c.recipient.id = :studentId")
    List<StudyBuddyConnection> findAllForStudent(@Param("studentId") UUID studentId);

    /**
     * Counts active connections grouped by student.
     *
     * @return the per-student counts
     */
    @Query("""
            select s.id as userId, count(c) as usageCount
            from Student s join StudyBuddyConnection c
              on c.requester.id = s.id or c.recipient.id = s.id
            where c.status = is442t1.studybuddy.model.enums.ConnectionStatus.ACTIVE
            group by s.id
            """)
    List<AccountUsageCount> countActiveByStudent();

    /**
     * Finds a student's active connections.
     *
     * @param studentId the student identifier
     * @return the active connections
     */
    @Query("""
            select c from StudyBuddyConnection c
            where c.status = is442t1.studybuddy.model.enums.ConnectionStatus.ACTIVE
              and (c.requester.id = :studentId or c.recipient.id = :studentId)
            """)
    List<StudyBuddyConnection> findActiveForStudent(@Param("studentId") UUID studentId);

    /**
     * Finds an active connection between two students in either direction.
     *
     * @param firstId the first student identifier
     * @param secondId the second student identifier
     * @return the active connection, if present
     */
    @Query("""
            select c from StudyBuddyConnection c
            where c.status = is442t1.studybuddy.model.enums.ConnectionStatus.ACTIVE
              and ((c.requester.id = :firstId and c.recipient.id = :secondId)
                or (c.requester.id = :secondId and c.recipient.id = :firstId))
            """)
    Optional<StudyBuddyConnection> findActiveBetween(@Param("firstId") UUID firstId, @Param("secondId") UUID secondId);
}
