package is442t1.studybuddy.telegram.link;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelegramAccountRepository extends JpaRepository<TelegramAccount, UUID> {

    Optional<TelegramAccount> findByBotIdAndStudent_Id(long botId, UUID studentId);

    Optional<TelegramAccount> findByBotIdAndTelegramUserId(long botId, long telegramUserId);
}
