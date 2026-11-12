package is442t1.studybuddy.telegram.chat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A group's chat as one member sees it. Its own endpoint, so group responses never carry Telegram data.
 *
 * @param status             NOT_LINKED, MANAGED_BY_OTHER_BOT, or a {@link GroupChatStatus}
 * @param title              chat title, or null when no managed chat is linked
 * @param chatType           Telegram chat type, or null when no managed chat is linked
 * @param missingRights      leader only; null for anyone else
 * @param ownedByFormerLeader leader only: the chat was set up by someone else, who probably still owns it
 * @param me                 null unless a chat this bot manages is linked
 * @param members            leader only; null for anyone else
 */
public record GroupChatResponse(String status, String title, String chatType, List<String> missingRights,
        boolean ownedByFormerLeader, Me me, List<Member> members) {

    /**
     * The requesting member's view of their own chat membership.
     *
     * @param state this student's current membership state
     * @param telegramName name recorded when they joined, or null when not joined
     * @param inviteUrl their current personal invite URL, or null if none is available
     * @param inviteExpiresAt expiration of the current invite, or null if none is available
     */
    public record Me(ChatMemberState state, String telegramName, String inviteUrl, Instant inviteExpiresAt) {
    }

    /**
     * A group member's chat state, included in a leader's response.
     *
     * @param studentId StudyBuddy student identifier
     * @param name student's StudyBuddy name
     * @param state student's current membership state
     * @param telegramName Telegram name recorded at joining, or null when not joined
     */
    public record Member(UUID studentId, String name, ChatMemberState state, String telegramName) {
    }

    static GroupChatResponse notLinked() {
        return new GroupChatResponse("NOT_LINKED", null, null, null, false, null, null);
    }

    static GroupChatResponse managedByOtherBot() {
        return new GroupChatResponse("MANAGED_BY_OTHER_BOT", null, null, null, false, null, null);
    }
}
