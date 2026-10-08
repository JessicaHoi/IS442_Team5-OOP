package is442t1.studybuddy.connection;

import java.util.UUID;

/**
 * The viewer's relationship with another student.
 *
 * @param status       relationship from the viewer's side
 * @param connectionId the active connection or pending request, or null when there is none
 */
public record BuddyState(BuddyStatus status, UUID connectionId) {

    public static final BuddyState NONE = new BuddyState(BuddyStatus.NONE, null);

    public boolean isConnected() {
        return status == BuddyStatus.CONNECTED;
    }
}
