package is442t1.studybuddy.telegram.chat;

/** Where one student stands with their group's Telegram chat. */
public enum ChatMemberState {
    /** No invite has been issued for this student. */
    NOT_INVITED,
    /** A personal invite is available or awaiting a join request. */
    INVITED,
    /** The student joined the linked Telegram chat. */
    JOINED,
    /** The student left the Telegram chat. */
    LEFT,
    /** The student was removed by StudyBuddy or an administrator. */
    REMOVED,
    /** They should be out, but Telegram refused, usually because they own the chat. The leader has to remove them. */
    REMOVAL_FAILED
}
