package is442t1.studybuddy.telegram.chat;

import java.time.Instant;

import is442t1.studybuddy.model.enums.SessionStatus;

/** Where a session's card is in its life. {@code ENDED} and {@code CANCELLED} are final. */
enum CardPhase {
    OPEN,
    STARTED,
    ENDED,
    CANCELLED;

    static CardPhase of(SessionStatus status, Instant startsAt, Instant endsAt, Instant now) {
        if (status == SessionStatus.CANCELLED) {
            return CANCELLED;
        }
        if (now.isBefore(startsAt)) {
            return OPEN;
        }
        return now.isBefore(endsAt) ? STARTED : ENDED;
    }
}
