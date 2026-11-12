package is442t1.studybuddy.studysession;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Persistence access and queries for study sessions. */
@Repository
public interface StudySessionRepository extends JpaRepository<StudySession, UUID> {

    /**
     * Finds a session within a group.
     *
     * @param id the session identifier
     * @param groupId the group identifier
     * @return the matching session, if present
     */
    Optional<StudySession> findByIdAndStudyGroup_Id(UUID id, UUID groupId);

    /**
     * Finds group sessions ending after a given time, earliest start first.
     *
     * @param groupId the group identifier
     * @param now the cutoff time
     * @return matching sessions
     */
    List<StudySession> findByStudyGroup_IdAndEndsAtAfterOrderByStartsAtAsc(UUID groupId, Instant now);

    /**
     * Finds group sessions ending at or before a given time, latest start first.
     *
     * @param groupId the group identifier
     * @param now the cutoff time
     * @return matching sessions
     */
    List<StudySession> findByStudyGroup_IdAndEndsAtLessThanEqualOrderByStartsAtDesc(UUID groupId, Instant now);

    /**
     * Finds group sessions starting after a given time.
     *
     * @param groupId the group identifier
     * @param now the cutoff time
     * @return matching sessions
     */
    List<StudySession> findByStudyGroup_IdAndStartsAtAfter(UUID groupId, Instant now);

    /**
     * Finds scheduled sessions still to finish in the student's groups, soonest first.
     *
     * @param studentId the student identifier
     * @param now the cutoff time
     * @return matching sessions
     */
    @Query("""
            select s from StudySession s join s.studyGroup g join g.members m
             where m.id = :studentId and s.status = is442t1.studybuddy.model.enums.SessionStatus.SCHEDULED
               and s.endsAt > :now
             order by s.startsAt asc
            """)
    List<StudySession> findUpcomingForStudent(@Param("studentId") UUID studentId, @Param("now") Instant now);

    /**
     * Finds scheduled video chat sessions in a group that have not ended and start by a cutoff.
     *
     * @param groupId the group identifier
     * @param startingBy the latest start time to include
     * @param now the cutoff for the end time
     * @return matching sessions, soonest first
     */
    @Query("""
            select s from StudySession s
             where s.studyGroup.id = :groupId and s.groupVideoChat = true
               and s.status = is442t1.studybuddy.model.enums.SessionStatus.SCHEDULED
               and s.startsAt <= :startingBy and s.endsAt > :now
             order by s.startsAt asc
            """)
    List<StudySession> findVideoChatSessions(@Param("groupId") UUID groupId, @Param("startingBy") Instant startingBy, @Param("now") Instant now);
}
