package is442t1.studybuddy.telegram.chat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Persistence queries for student membership in linked Telegram chats. */
@Repository
public interface ChatMembershipRepository extends JpaRepository<ChatMembership, UUID> {

    /**
     * Finds the membership row for one student in one linked chat.
     *
     * @param groupChatId linked-chat entity identifier
     * @param studentId StudyBuddy student identifier
     * @return matching membership, if one exists
     */
    Optional<ChatMembership> findByGroupChat_IdAndStudent_Id(UUID groupChatId, UUID studentId);

    /**
     * Finds the membership associated with an invite label in one chat.
     *
     * @param groupChatId linked-chat entity identifier
     * @param inviteName Telegram invite label
     * @return matching membership, if one exists
     */
    Optional<ChatMembership> findByGroupChat_IdAndInviteName(UUID groupChatId, String inviteName);

    /**
     * Finds the membership associated with an invite URL in one chat.
     *
     * @param groupChatId linked-chat entity identifier
     * @param inviteLink Telegram invite URL
     * @return matching membership, if one exists
     */
    Optional<ChatMembership> findByGroupChat_IdAndInviteLink(UUID groupChatId, String inviteLink);

    /**
     * Finds the student membership associated with a Telegram account in one chat.
     *
     * @param groupChatId linked-chat entity identifier
     * @param telegramUserId Telegram user identifier
     * @return matching membership, if one exists
     */
    Optional<ChatMembership> findByGroupChat_IdAndTelegramUserId(UUID groupChatId, long telegramUserId);

    /**
     * Lists all student memberships for a linked chat.
     *
     * @param groupChatId linked-chat entity identifier
     * @return memberships recorded for the chat
     */
    List<ChatMembership> findByGroupChat_Id(UUID groupChatId);

    /**
     * Lists every chat membership for a StudyBuddy student.
     *
     * @param studentId StudyBuddy student identifier
     * @return memberships recorded for the student
     */
    List<ChatMembership> findByStudent_Id(UUID studentId);
}
