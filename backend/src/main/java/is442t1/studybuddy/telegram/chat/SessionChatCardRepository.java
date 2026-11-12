package is442t1.studybuddy.telegram.chat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Persistence queries for Telegram cards representing study sessions. */
@Repository
public interface SessionChatCardRepository extends JpaRepository<SessionChatCard, UUID> {

    /**
     * Finds the chat card for a study session, if one has been recorded.
     *
     * @param studySessionId StudyBuddy session identifier
     * @return associated chat card, if one exists
     */
    Optional<SessionChatCard> findByStudySession_Id(UUID studySessionId);

    /**
     * Finds posted cards in a chat together with their sessions.
     *
     * @param groupChatId linked-chat entity identifier
     * @return posted cards for that chat
     */
    @Query("""
            select card from SessionChatCard card join fetch card.studySession
             where card.groupChat.id = :groupChatId and card.messageId is not null
            """)
    List<SessionChatCard> findPosted(@Param("groupChatId") UUID groupChatId);

    /**
     * Finds posted cards that may change phase in chats where this bot can still edit them.
     *
     * @param botId Telegram bot identifier
     * @return unfinished cards belonging to the bot
     */
    @Query("""
            select card from SessionChatCard card join fetch card.studySession
             where card.groupChat.botId = :botId and card.messageId is not null
               and card.groupChat.status in (is442t1.studybuddy.telegram.chat.GroupChatStatus.ACTIVE, is442t1.studybuddy.telegram.chat.GroupChatStatus.CLOSED)
               and (card.shownPhase is null or card.shownPhase in (is442t1.studybuddy.telegram.chat.CardPhase.OPEN, is442t1.studybuddy.telegram.chat.CardPhase.STARTED))
            """)
    List<SessionChatCard> findUnfinished(@Param("botId") long botId);

    /**
     * Finds posted cards of scheduled sessions in the supplied time interval that have not been nudged.
     *
     * @param botId Telegram bot identifier
     * @param now beginning of the interval
     * @param until end of the interval
     * @return cards awaiting a nudge
     */
    @Query("""
            select card from SessionChatCard card join fetch card.studySession session
             where card.groupChat.botId = :botId and card.messageId is not null and card.nudgeSentAt is null
               and card.groupChat.status = is442t1.studybuddy.telegram.chat.GroupChatStatus.ACTIVE
               and session.status = is442t1.studybuddy.model.enums.SessionStatus.SCHEDULED
               and session.startsAt > :now and session.startsAt <= :until
            """)
    List<SessionChatCard> findAwaitingNudge(@Param("botId") long botId, @Param("now") Instant now, @Param("until") Instant until);
}
