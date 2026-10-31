package is442t1.studybuddy.telegram.chat;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionChatCardRepository extends JpaRepository<SessionChatCard, UUID> {

    Optional<SessionChatCard> findByStudySession_Id(UUID sessionId);
}
