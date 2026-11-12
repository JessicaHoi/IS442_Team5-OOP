package is442t1.studybuddy.telegram.api;

/**
 * One person's standing and relevant permissions in a chat. The rights are only meaningful for administrators.
 *
 * @param user the Telegram account
 * @param status Telegram's membership status, such as {@code member}, {@code administrator}, or {@code left}
 * @param isMember Telegram's membership flag, used for restricted users
 * @param canInviteUsers whether this administrator may invite users
 * @param canRestrictMembers whether this administrator may restrict or remove members
 * @param canPinMessages whether this administrator may pin messages
 * @param canChangeInfo whether this administrator may change chat details
 */
public record ChatMember(Update.Sender user, String status, boolean isMember, boolean canInviteUsers,
        boolean canRestrictMembers, boolean canPinMessages, boolean canChangeInfo) {

    /**
     * Returns whether Telegram reports this person as an administrator.
     *
     * @return true when the status is {@code administrator}
     */
    public boolean isAdministrator() {
        return "administrator".equals(status);
    }

    /**
     * Returns whether Telegram reports this person as the owner or an administrator.
     *
     * @return true when the status is {@code creator} or {@code administrator}
     */
    public boolean isOwnerOrAdministrator() {
        return "creator".equals(status) || isAdministrator();
    }

    /**
     * Reports whether this person is currently in the chat; for restricted users this uses {@code is_member}.
     *
     * @return true for owners, administrators, members, or restricted users marked as members
     */
    public boolean isInChat() {
        return "creator".equals(status) || isAdministrator() || "member".equals(status)
                || ("restricted".equals(status) && isMember);
    }
}
