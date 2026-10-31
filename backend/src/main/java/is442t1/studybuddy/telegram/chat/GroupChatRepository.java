package is442t1.studybuddy.telegram.chat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import is442t1.studybuddy.studysession.StudySession;

@Repository
public interface GroupChatRepository extends JpaRepository<GroupChat, UUID> {

    Optional<GroupChat> findByStudyGroup_Id(UUID groupId);

    Optional<GroupChat> findByBotIdAndChatId(long botId, long chatId);

    /** Scheduled sessions starting soon, with no reminder sent, whose group has a working chat with this bot. */
    @Query("""
            select s from StudySession s, GroupChat c
             where c.studyGroup = s.studyGroup and c.botId = :botId
               and c.status = is442t1.studybuddy.telegram.chat.GroupChatStatus.ACTIVE
               and s.status = is442t1.studybuddy.model.enums.SessionStatus.SCHEDULED
               and s.startsAt > :now and s.startsAt <= :until
               and not exists (select 1 from SessionChatCard card where card.studySession = s and card.reminderSentAt is not null)
            """)
    List<StudySession> findSessionsStartingSoon(@Param("botId") long botId, @Param("now") Instant now, @Param("until") Instant until);

    /** Scheduled sessions still to start, with no card posted, whose group has a working chat with this bot. */
    @Query("""
            select s from StudySession s, GroupChat c
             where c.studyGroup = s.studyGroup and c.botId = :botId
               and c.status = is442t1.studybuddy.telegram.chat.GroupChatStatus.ACTIVE
               and s.status = is442t1.studybuddy.model.enums.SessionStatus.SCHEDULED
               and s.startsAt > :now
               and not exists (select 1 from SessionChatCard card where card.studySession = s and card.messageId is not null)
            """)
    List<StudySession> findSessionsWithoutCard(@Param("botId") long botId, @Param("now") Instant now);
}
