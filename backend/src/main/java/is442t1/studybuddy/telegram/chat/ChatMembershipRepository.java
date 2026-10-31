package is442t1.studybuddy.telegram.chat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMembershipRepository extends JpaRepository<ChatMembership, UUID> {

    Optional<ChatMembership> findByGroupChat_IdAndStudent_Id(UUID groupChatId, UUID studentId);

    Optional<ChatMembership> findByGroupChat_IdAndInviteName(UUID groupChatId, String inviteName);

    Optional<ChatMembership> findByGroupChat_IdAndInviteLink(UUID groupChatId, String inviteLink);

    Optional<ChatMembership> findByGroupChat_IdAndTelegramUserId(UUID groupChatId, long telegramUserId);

    List<ChatMembership> findByGroupChat_Id(UUID groupChatId);

    List<ChatMembership> findByStudent_Id(UUID studentId);
}
