package is442t1.studybuddy.studysession.event;

import java.util.UUID;

/**
 * A study session's details were changed. Published inside the transaction that made the change.
 *
 * @param startMoved the start time changed, so everyone's answers were cleared
 */
public record SessionChanged(UUID sessionId, boolean startMoved) {
}
