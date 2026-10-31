package is442t1.studybuddy.telegram.chat;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupChatRepository extends JpaRepository<GroupChat, UUID> {

    Optional<GroupChat> findByStudyGroup_Id(UUID groupId);

    Optional<GroupChat> findByBotIdAndChatId(long botId, long chatId);
}
