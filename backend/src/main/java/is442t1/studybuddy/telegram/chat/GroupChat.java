package is442t1.studybuddy.telegram.chat;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import is442t1.studybuddy.group.StudyGroup;
import is442t1.studybuddy.model.BaseEntity;

/** The Telegram chat linked to a study group, and the bot that manages it. */
@Entity
@Table(name = "group_chat", uniqueConstraints = {
        @UniqueConstraint(columnNames = "study_group_id"),
        @UniqueConstraint(columnNames = {"bot_id", "chat_id"})
})
public class GroupChat extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_group_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private StudyGroup studyGroup;

    @Column(nullable = false)
    private long botId;

    @Column(nullable = false)
    private long chatId;

    @Column(nullable = false, length = 20)
    private String chatType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupChatStatus status;

    private String title;

    /** Comma-separated names from {@link ChatRights}. Blank when nothing is missing. */
    @Column(nullable = false)
    private String missingRights = "";

    /** Who linked the chat, and so probably owns it. Telegram can't hand ownership over for a new leader. */
    @Column(nullable = false)
    private UUID linkedByStudentId;

    @Column(nullable = false)
    private Instant linkedAt;

    @Version
    private long version;

    protected GroupChat() {
    }

    GroupChat(StudyGroup studyGroup, long botId, long chatId, String chatType, String title, UUID linkedByStudentId,
            ChatRights rights, Instant now) {
        this.studyGroup = studyGroup;
        this.botId = botId;
        this.chatId = chatId;
        this.chatType = chatType;
        this.title = title;
        this.linkedByStudentId = linkedByStudentId;
        this.linkedAt = now;
        applyRights(rights);
    }

    /** Updates what's missing. Returns true if this made the chat usable, so setup can finish. */
    boolean applyRights(ChatRights rights) {
        boolean wasReady = status == GroupChatStatus.ACTIVE;
        missingRights = rights.missingNames();
        if (status != GroupChatStatus.CLOSED) {
            status = rights.hasRequired() ? GroupChatStatus.ACTIVE : GroupChatStatus.NEEDS_ATTENTION;
        }
        return !wasReady && status == GroupChatStatus.ACTIVE;
    }

    /** A basic group becomes a supergroup with a new id when it's upgraded. */
    void migrateTo(long newChatId) {
        chatId = newChatId;
        chatType = "supergroup";
    }

    void markDetached() {
        status = GroupChatStatus.DETACHED;
    }

    void markClosed() {
        status = GroupChatStatus.CLOSED;
    }

    public StudyGroup getStudyGroup() {
        return studyGroup;
    }

    public long getBotId() {
        return botId;
    }

    public long getChatId() {
        return chatId;
    }

    public String getChatType() {
        return chatType;
    }

    public GroupChatStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public String getMissingRights() {
        return missingRights;
    }

    public UUID getLinkedByStudentId() {
        return linkedByStudentId;
    }
}
