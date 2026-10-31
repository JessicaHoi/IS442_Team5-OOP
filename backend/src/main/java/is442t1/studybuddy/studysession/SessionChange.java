package is442t1.studybuddy.studysession;

/** What saving a session's form actually changed. Moving the start clears answers, because they were for the old time. */
public enum SessionChange {
    NONE,
    DETAILS,
    START_MOVED
}
