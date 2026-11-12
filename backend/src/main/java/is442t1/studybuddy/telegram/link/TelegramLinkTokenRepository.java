package is442t1.studybuddy.telegram.link;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Persistence queries and atomic operations for short-lived Telegram links. */
@Repository
public interface TelegramLinkTokenRepository extends JpaRepository<TelegramLinkToken, UUID> {

    /**
     * Finds a token by its stored hash and owning bot.
     *
     * @param tokenHash SHA-256 hash of the presented token
     * @param botId Telegram bot identifier
     * @return matching token, if one exists
     */
    Optional<TelegramLinkToken> findByTokenHashAndBotId(String tokenHash, long botId);

    /**
     * Atomically marks a valid token as used.
     *
     * @param tokenHash SHA-256 hash of the presented token
     * @param botId bot that owns the token
     * @param now current time used for expiration checking
     * @return one if this call claimed the token, otherwise zero
     */
    @Modifying
    @Query("""
            update TelegramLinkToken token set token.usedAt = :now
            where token.tokenHash = :tokenHash and token.botId = :botId
              and token.usedAt is null and token.expiresAt > :now
            """)
    int claim(@Param("tokenHash") String tokenHash, @Param("botId") long botId, @Param("now") Instant now);

    /**
     * Removes expired tokens for the bot and existing tokens of the requested kind for the student.
     *
     * @param botId bot whose tokens are being cleaned
     * @param studentId student creating the replacement link
     * @param purpose kind of token being replaced
     * @param now current time used to identify expired tokens
     */
    @Modifying
    @Query("""
            delete from TelegramLinkToken token where token.botId = :botId and token.expiresAt <= :now
               or (token.botId = :botId and token.student.id = :studentId and token.purpose = :purpose)
            """)
    void deleteReplacedOrExpired(@Param("botId") long botId, @Param("studentId") UUID studentId,
            @Param("purpose") TelegramLinkPurpose purpose, @Param("now") Instant now);

    /**
     * Finds the active group-setup token owning either of its two consecutive button identifiers.
     *
     * @param botId bot that owns the setup token
     * @param telegramUserId user who claimed the setup token
     * @param requestId identifier supplied by a chat-selection button
     * @param now current time used for expiration checking
     * @return the active setup token, if it belongs to this user and button
     */
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
