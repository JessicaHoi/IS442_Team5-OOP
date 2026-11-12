package is442t1.studybuddy.connection;

/** How the viewer relates to another student, as shown on profiles and matches. */
public enum BuddyStatus {
    /** No active connection or pending request. */
    NONE,
    /** The viewer sent a pending request. */
    PENDING_SENT,
    /** The viewer received a pending request. */
    PENDING_RECEIVED,
    /** The students have an active connection. */
    CONNECTED
}
