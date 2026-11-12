package is442t1.studybuddy.telegram.link;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Persistence queries for student Telegram accounts connected through a bot. */
@Repository
public interface TelegramAccountRepository extends JpaRepository<TelegramAccount, UUID> {

    /**
     * Finds a student's connected Telegram account for a bot.
     *
     * @param botId Telegram bot identifier
     * @param studentId StudyBuddy student identifier
     * @return matching account, if one exists
     */
    Optional<TelegramAccount> findByBotIdAndStudent_Id(long botId, UUID studentId);

    /**
     * Finds the StudyBuddy account linked to a Telegram user for a bot.
     *
     * @param botId Telegram bot identifier
     * @param telegramUserId Telegram user identifier
     * @return matching account, if one exists
     */
    Optional<TelegramAccount> findByBotIdAndTelegramUserId(long botId, long telegramUserId);
}
