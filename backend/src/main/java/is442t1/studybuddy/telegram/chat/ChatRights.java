package is442t1.studybuddy.telegram.chat;

import java.util.ArrayList;
import java.util.List;

import is442t1.studybuddy.telegram.api.ChatMember;

/** Which of the rights StudyBuddy cares about the bot is missing in a chat. */
record ChatRights(boolean admin, boolean inviteUsers, boolean restrictMembers, boolean pinMessages, boolean changeInfo) {

    static ChatRights of(ChatMember bot) {
        return new ChatRights(bot.isAdministrator(), bot.canInviteUsers(), bot.canRestrictMembers(), bot.canPinMessages(),
                bot.canChangeInfo());
    }

    /** Invites and removals are the feature. Pinning and renaming are nice to have. */
    boolean hasRequired() {
        return admin && inviteUsers && restrictMembers;
    }

    /** Names the frontend turns into the setting to switch on. */
    String missingNames() {
        List<String> missing = new ArrayList<>();
        if (!inviteUsers) {
            missing.add("INVITE_USERS");
        }
        if (!restrictMembers) {
            missing.add("RESTRICT_MEMBERS");
        }
        if (!pinMessages) {
            missing.add("PIN_MESSAGES");
        }
        if (!changeInfo) {
            missing.add("CHANGE_INFO");
        }
        return String.join(",", missing);
    }
}
