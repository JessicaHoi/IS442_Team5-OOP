package is442t1.studybuddy.telegram.api;

/** One person's standing in a chat. The rights are only meaningful for administrators. */
public record ChatMember(Update.Sender user, String status, boolean isMember, boolean canInviteUsers,
        boolean canRestrictMembers, boolean canPinMessages, boolean canChangeInfo) {

    public boolean isAdministrator() {
        return "administrator".equals(status);
    }

    public boolean isOwnerOrAdministrator() {
        return "creator".equals(status) || isAdministrator();
    }

    /** "restricted" people may be in or out of the chat, which {@code is_member} says. */
    public boolean isInChat() {
        return "creator".equals(status) || isAdministrator() || "member".equals(status)
                || ("restricted".equals(status) && isMember);
    }
}
