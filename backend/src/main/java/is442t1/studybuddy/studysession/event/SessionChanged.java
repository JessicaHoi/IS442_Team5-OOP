package is442t1.studybuddy.studysession.event;

import java.util.UUID;

/** A study session's details were changed. Published inside the transaction that made the change. */
public record SessionChanged(UUID sessionId) {
}
