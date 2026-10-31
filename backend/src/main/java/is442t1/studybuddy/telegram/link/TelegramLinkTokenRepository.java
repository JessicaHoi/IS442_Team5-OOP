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

    /** A new link replaces the student's earlier ones, which also clears out expired tokens as it goes. */
    @Modifying
    @Query("delete from TelegramLinkToken token where token.botId = :botId and (token.student.id = :studentId or token.expiresAt <= :now)")
    void deleteReplacedOrExpired(@Param("botId") long botId, @Param("studentId") UUID studentId, @Param("now") Instant now);
}
