package is442t1.studybuddy.telegram.chat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A group's chat as one member sees it. Its own endpoint, so group responses never carry Telegram data.
 *
 * @param status             NOT_LINKED, MANAGED_BY_OTHER_BOT, or a {@link GroupChatStatus}
 * @param missingRights      leader only; null for anyone else
 * @param ownedByFormerLeader leader only: the chat was set up by someone else, who probably still owns it
 * @param me                 null unless a chat this bot manages is linked
 * @param members            leader only; null for anyone else
 */
public record GroupChatResponse(String status, String title, String chatType, List<String> missingRights,
        boolean ownedByFormerLeader, Me me, List<Member> members) {

    public record Me(ChatMemberState state, String telegramName, String inviteUrl, Instant inviteExpiresAt) {
    }

    public record Member(UUID studentId, String name, ChatMemberState state, String telegramName) {
    }

    static GroupChatResponse notLinked() {
        return new GroupChatResponse("NOT_LINKED", null, null, null, false, null, null);
    }

    static GroupChatResponse managedByOtherBot() {
        return new GroupChatResponse("MANAGED_BY_OTHER_BOT", null, null, null, false, null, null);
    }
}
