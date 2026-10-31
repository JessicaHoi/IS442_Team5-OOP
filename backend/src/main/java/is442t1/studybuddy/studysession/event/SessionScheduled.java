package is442t1.studybuddy.studysession.event;

import java.util.UUID;

/** A study session was scheduled. Published inside the transaction that made the change. */
public record SessionScheduled(UUID sessionId) {
}
