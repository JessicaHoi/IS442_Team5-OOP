package is442t1.studybuddy.studysession.event;

import java.util.UUID;

/**
 * A leader cancelled a study session. Sessions cancelled by closing their
 * group don't publish this. Published inside the transaction that made the change.
 *
 * @param sessionId the cancelled session's identifier
 */
public record SessionCancelled(UUID sessionId) {
}
