package is442t1.studybuddy.studysession;

/** What saving a session's form actually changed. Moving the start clears answers, because they were for the old time. */
public enum SessionChange {
    /** No session details changed. */
    NONE,
    /** Details changed without moving the start. */
    DETAILS,
    /** The start time changed and existing answers were cleared. */
    START_MOVED
}
