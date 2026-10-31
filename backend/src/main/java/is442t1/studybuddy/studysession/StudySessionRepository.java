package is442t1.studybuddy.studysession;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StudySessionRepository extends JpaRepository<StudySession, UUID> {

    Optional<StudySession> findByIdAndStudyGroup_Id(UUID id, UUID groupId);

    List<StudySession> findByStudyGroup_IdAndEndsAtAfterOrderByStartsAtAsc(UUID groupId, Instant now);

    List<StudySession> findByStudyGroup_IdAndEndsAtLessThanEqualOrderByStartsAtDesc(UUID groupId, Instant now);

    List<StudySession> findByStudyGroup_IdAndStartsAtAfter(UUID groupId, Instant now);

    /** Sessions still to finish in every group the student belongs to, soonest first. */
    @Query("""
            select s from StudySession s join s.studyGroup g join g.members m
             where m.id = :studentId and s.status = is442t1.studybuddy.model.enums.SessionStatus.SCHEDULED
               and s.endsAt > :now
             order by s.startsAt asc
            """)
    List<StudySession> findUpcomingForStudent(@Param("studentId") UUID studentId, @Param("now") Instant now);

    /** Video chat sessions in this group that are running or about to, soonest first. */
    @Query("""
            select s from StudySession s
             where s.studyGroup.id = :groupId and s.groupVideoChat = true
               and s.status = is442t1.studybuddy.model.enums.SessionStatus.SCHEDULED
               and s.startsAt <= :startingBy and s.endsAt > :now
             order by s.startsAt asc
            """)
    List<StudySession> findVideoChatSessions(@Param("groupId") UUID groupId, @Param("startingBy") Instant startingBy, @Param("now") Instant now);

    /** Returns 1 for the one instance that gets to send this reminder. A claim that never finished expires. */
    @Modifying
    @Query("""
            update StudySession s set s.reminderClaimedAt = :now
             where s.id = :id and s.reminderSentAt is null
               and (s.reminderClaimedAt is null or s.reminderClaimedAt < :staleBefore)
            """)
    int claimReminder(@Param("id") UUID id, @Param("now") Instant now, @Param("staleBefore") Instant staleBefore);

    /** Does nothing if the session was changed since the claim, so the new time still gets its reminder. */
    @Modifying
    @Query("update StudySession s set s.reminderSentAt = :now where s.id = :id and s.reminderClaimedAt = :claimedAt")
    int markReminderSent(@Param("id") UUID id, @Param("claimedAt") Instant claimedAt, @Param("now") Instant now);
}
