package is442t1.studybuddy.telegram.link;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TelegramLinkTokenRepository extends JpaRepository<TelegramLinkToken, UUID> {

    Optional<TelegramLinkToken> findByTokenHashAndBotId(String tokenHash, long botId);

    /** Uses the token if it is still valid. Returns 1 for the one caller that gets it, even if two arrive at once. */
    @Modifying
    @Query("""
            update TelegramLinkToken token set token.usedAt = :now
            where token.tokenHash = :tokenHash and token.botId = :botId
              and token.usedAt is null and token.expiresAt > :now
            """)
    int claim(@Param("tokenHash") String tokenHash, @Param("botId") long botId, @Param("now") Instant now);

    /** A new link replaces the student's earlier ones of that kind, which also clears out expired tokens as it goes. */
    @Modifying
    @Query("""
            delete from TelegramLinkToken token where token.botId = :botId and token.expiresAt <= :now
               or (token.botId = :botId and token.student.id = :studentId and token.purpose = :purpose)
            """)
    void deleteReplacedOrExpired(@Param("botId") long botId, @Param("studentId") UUID studentId,
            @Param("purpose") TelegramLinkPurpose purpose, @Param("now") Instant now);

    /** The setup token this Telegram user is working through. Each token owns two consecutive button ids. */
    @Query("""
            select token from TelegramLinkToken token
            where token.botId = :botId and token.claimedTelegramUserId = :telegramUserId
              and token.purpose = is442t1.studybuddy.telegram.link.TelegramLinkPurpose.GROUP_SETUP
              and (token.requestId = :requestId or token.requestId + 1 = :requestId)
              and token.usedAt is null and token.expiresAt > :now
            """)
    Optional<TelegramLinkToken> findSetupInProgress(@Param("botId") long botId, @Param("telegramUserId") long telegramUserId,
            @Param("requestId") int requestId, @Param("now") Instant now);
}
