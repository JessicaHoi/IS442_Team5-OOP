package is442t1.studybuddy.telegram.chat;

import java.time.Instant;

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

import is442t1.studybuddy.model.BaseEntity;
import is442t1.studybuddy.student.Student;

/**
 * One student's place in a group's chat. Joining stores who joined here and never creates a Telegram account link,
 * so someone holding a forwarded invite can't take over the student's private messages.
 */
@Entity
@Table(name = "chat_membership", uniqueConstraints = @UniqueConstraint(columnNames = {"group_chat_id", "student_id"}))
public class ChatMembership extends BaseEntity {

    static final String NOT_MEMBER = "This invite is no longer valid, because you aren't a member of that study group.";
    static final String LINK_NOT_VALID = "This invite has expired or was replaced. Open the group in StudyBuddy to get a new one.";
    static final String WRONG_ACCOUNT = "This invite belongs to another student's Telegram account. Open the group in StudyBuddy to get your own.";
    static final String ALREADY_USED_BY_ANOTHER = "Your Telegram account has already joined this chat as another StudyBuddy student.";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_chat_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private GroupChat groupChat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatMemberState state = ChatMemberState.NOT_INVITED;

    @Column(unique = true)
    private String inviteLink;

    @Column(length = 32)
    private String inviteName;

    private Instant inviteExpiresAt;

    private Long telegramUserId;

    private String telegramName;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected ChatMembership() {
    }

    ChatMembership(GroupChat groupChat, Student student, Instant now) {
        this.groupChat = groupChat;
        this.student = student;
        this.updatedAt = now;
    }

    /**
     * The rules for one join request. The link must be this student's current one, the student must still be an active
     * member, and the person asking must not be someone else as far as StudyBuddy can tell.
     *
     * @param linkedTelegramUserId the Telegram user this student connected through this bot, or null
     * @param usedByAnotherStudent whether the person asking has already joined this chat as another student
     */
    JoinDecision decideJoin(boolean activeMember, Long linkedTelegramUserId, boolean usedByAnotherStudent, long askingUserId,
            Instant now) {
        if (!activeMember) {
            return new JoinDecision.Decline(NOT_MEMBER);
        }
        if (state != ChatMemberState.INVITED || inviteExpiresAt == null || !inviteExpiresAt.isAfter(now)) {
            return new JoinDecision.Decline(LINK_NOT_VALID);
        }
        if (linkedTelegramUserId != null && linkedTelegramUserId != askingUserId) {
            return new JoinDecision.Decline(WRONG_ACCOUNT);
        }
        if (usedByAnotherStudent) {
            return new JoinDecision.Decline(ALREADY_USED_BY_ANOTHER);
        }
        return new JoinDecision.Approve();
    }

    void invite(String link, String name, Instant expiresAt, Instant now) {
        inviteLink = link;
        inviteName = name;
        inviteExpiresAt = expiresAt;
        state = ChatMemberState.INVITED;
        updatedAt = now;
    }

    /** Records who is in the chat for this student. The invite is finished with. */
    void markJoined(long userId, String name, Instant now) {
        telegramUserId = userId;
        telegramName = name;
        clearInvite();
        state = ChatMemberState.JOINED;
        updatedAt = now;
    }

    void markLeft(Instant now) {
        state = ChatMemberState.LEFT;
        updatedAt = now;
    }

    void markRemoved(Instant now) {
        clearInvite();
        telegramUserId = null;
        telegramName = null;
        state = ChatMemberState.REMOVED;
        updatedAt = now;
    }

    void markRemovalFailed(Instant now) {
        state = ChatMemberState.REMOVAL_FAILED;
        updatedAt = now;
    }

    /** A student who rejoins the study group starts again, even if they were removed from the chat before. */
    void reset(Instant now) {
        clearInvite();
        telegramUserId = null;
        telegramName = null;
        state = ChatMemberState.NOT_INVITED;
        updatedAt = now;
    }

    private void clearInvite() {
        inviteLink = null;
        inviteName = null;
        inviteExpiresAt = null;
    }

    boolean hasValidInvite(Instant now) {
        return state == ChatMemberState.INVITED && inviteLink != null && inviteExpiresAt.isAfter(now);
    }

    public GroupChat getGroupChat() {
        return groupChat;
    }

    public Student getStudent() {
        return student;
    }

    public ChatMemberState getState() {
        return state;
    }

    public String getInviteLink() {
        return inviteLink;
    }

    public Instant getInviteExpiresAt() {
        return inviteExpiresAt;
    }

    public Long getTelegramUserId() {
        return telegramUserId;
    }

    public String getTelegramName() {
        return telegramName;
    }
}
