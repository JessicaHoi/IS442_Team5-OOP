package is442t1.studybuddy.telegram.chat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionChatCardRepository extends JpaRepository<SessionChatCard, UUID> {

    Optional<SessionChatCard> findByStudySession_Id(UUID studySessionId);

    /** Posted cards in this chat, with their sessions. */
    @Query("""
            select card from SessionChatCard card join fetch card.studySession
             where card.groupChat.id = :groupChatId and card.messageId is not null
            """)
    List<SessionChatCard> findPosted(@Param("groupChatId") UUID groupChatId);

    /** Posted cards that could still change phase, in chats where this bot can still edit them. */
    @Query("""
            select card from SessionChatCard card join fetch card.studySession
             where card.groupChat.botId = :botId and card.messageId is not null
               and card.groupChat.status in (is442t1.studybuddy.telegram.chat.GroupChatStatus.ACTIVE, is442t1.studybuddy.telegram.chat.GroupChatStatus.CLOSED)
               and (card.shownPhase is null or card.shownPhase in (is442t1.studybuddy.telegram.chat.CardPhase.OPEN, is442t1.studybuddy.telegram.chat.CardPhase.STARTED))
            """)
    List<SessionChatCard> findUnfinished(@Param("botId") long botId);

    /** Posted cards of scheduled sessions starting soon, with no nudge sent, in working chats with this bot. */
    @Query("""
            select card from SessionChatCard card join fetch card.studySession session
             where card.groupChat.botId = :botId and card.messageId is not null and card.nudgeSentAt is null
               and card.groupChat.status = is442t1.studybuddy.telegram.chat.GroupChatStatus.ACTIVE
               and session.status = is442t1.studybuddy.model.enums.SessionStatus.SCHEDULED
               and session.startsAt > :now and session.startsAt <= :until
            """)
    List<SessionChatCard> findAwaitingNudge(@Param("botId") long botId, @Param("now") Instant now, @Param("until") Instant until);
}
