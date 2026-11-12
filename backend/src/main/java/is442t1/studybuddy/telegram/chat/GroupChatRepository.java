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

/** Persistence queries for linked Telegram chats and sessions posted to them. */
@Repository
public interface GroupChatRepository extends JpaRepository<GroupChat, UUID> {

    /**
     * Finds the Telegram chat linked to a study group, if any.
     *
     * @param groupId StudyBuddy group identifier
     * @return linked chat, if one exists
     */
    Optional<GroupChat> findByStudyGroup_Id(UUID groupId);

    /**
     * Finds the chat associated with a bot and Telegram chat identifier.
     *
     * @param botId Telegram bot identifier
     * @param chatId Telegram chat identifier
     * @return linked chat, if one exists
     */
    Optional<GroupChat> findByBotIdAndChatId(long botId, long chatId);

    /**
     * Finds scheduled sessions starting in the supplied interval that have no reminder and an active chat.
     *
     * @param botId Telegram bot identifier
     * @param now beginning of the interval
     * @param until end of the interval
     * @return sessions needing a reminder
     */
    @Query("""
            select s from StudySession s, GroupChat c
             where c.studyGroup = s.studyGroup and c.botId = :botId
               and c.status = is442t1.studybuddy.telegram.chat.GroupChatStatus.ACTIVE
               and s.status = is442t1.studybuddy.model.enums.SessionStatus.SCHEDULED
               and s.startsAt > :now and s.startsAt <= :until
               and not exists (select 1 from SessionChatCard card where card.studySession = s and card.reminderSentAt is not null)
            """)
    List<StudySession> findSessionsStartingSoon(@Param("botId") long botId, @Param("now") Instant now, @Param("until") Instant until);

    /**
     * Finds scheduled future sessions without a posted card in an active chat.
     *
     * @param botId Telegram bot identifier
     * @param now current time; only sessions starting later are considered
     * @return sessions needing an initial card
     */
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
