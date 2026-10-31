package is442t1.studybuddy.telegram.chat;

/** The state of a study group's linked Telegram chat. */
public enum GroupChatStatus {
    ACTIVE,
    /** The bot is in the chat but lacks a right it needs. Nobody can be invited until that's fixed. */
    NEEDS_ATTENTION,
    /** The bot was removed from the chat. */
    DETACHED,
    /** The study group closed, so the chat is read-only. */
    CLOSED
}
