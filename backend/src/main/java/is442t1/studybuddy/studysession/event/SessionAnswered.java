package is442t1.studybuddy.studysession.event;

import java.util.UUID;

/** Someone answered a study session, in the app or in Telegram. Published inside the transaction that saved it. */
public record SessionAnswered(UUID sessionId) {
}
