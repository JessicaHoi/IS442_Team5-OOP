package is442t1.studybuddy.telegram.chat;

/** Where one student stands with their group's Telegram chat. */
public enum ChatMemberState {
    NOT_INVITED,
    INVITED,
    JOINED,
    LEFT,
    REMOVED,
    /** They should be out, but Telegram refused, usually because they own the chat. The leader has to remove them. */
    REMOVAL_FAILED
}
