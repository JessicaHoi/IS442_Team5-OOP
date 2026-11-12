package is442t1.studybuddy.telegram.chat;

/** The state of a study group's linked Telegram chat. */
public enum GroupChatStatus {
    /** The chat is linked and its required permissions are available. */
    ACTIVE,
    /** The bot is in the chat but lacks a right it needs. Nobody can be invited until that's fixed. */
    NEEDS_ATTENTION,
    /** The bot has left or been removed, but the record can be replaced during setup. */
    DETACHED,
    /** The study group closed, so the chat is read-only. */
    CLOSED
}
