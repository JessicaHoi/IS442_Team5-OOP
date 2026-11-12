package is442t1.studybuddy.connection;

import java.util.UUID;

/**
 * The viewer's relationship with another student.
 *
 * @param status       relationship from the viewer's side
 * @param connectionId the active connection or pending request, or null when there is none
 */
public record BuddyState(BuddyStatus status, UUID connectionId) {

    /** A relationship with no connection or pending request. */
    public static final BuddyState NONE = new BuddyState(BuddyStatus.NONE, null);

    /**
     * Checks whether the students have an active connection.
     *
     * @return {@code true} when connected
     */
    public boolean isConnected() {
        return status == BuddyStatus.CONNECTED;
    }
}
