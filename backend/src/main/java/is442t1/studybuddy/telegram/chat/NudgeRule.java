package is442t1.studybuddy.telegram.chat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

/** When a chat can be asked for missing answers, and when a leader should hear that few are going. Plain rules, tested without Spring. */
final class NudgeRule {

    private static final LocalTime DAY_STARTS = LocalTime.of(8, 0);
    private static final LocalTime DAY_ENDS = LocalTime.of(22, 0);
    /** Closer to the start than this, the reminder is coming and a nudge would only add noise. */
    private static final Duration LAST_CALL = Duration.ofHours(3);
    private static final int ENOUGH_GOING = 2;

    private NudgeRule() {
    }

    /**
     * Whether a session's one nudge can go out now. The window opens {@code lead} before the start and closes at the last
     * call. A session announced after it opened has only just asked people to answer, so it gets no nudge as well.
     *
     * @param announcedAt when the chat last learnt the start time, or null if that isn't known
     */
    static boolean isDue(Instant startsAt, Instant announcedAt, Duration lead, ZoneId zone, Instant now) {
        Instant opens = startsAt.minus(lead);
        boolean insideWindow = !now.isBefore(opens) && now.isBefore(startsAt.minus(LAST_CALL));
        boolean announcedInTime = announcedAt == null || !announcedAt.isAfter(opens);
        return insideWindow && announcedInTime && isDaytime(now, zone);
    }

    private static boolean isDaytime(Instant now, ZoneId zone) {
        LocalTime time = now.atZone(zone).toLocalTime();
        return !time.isBefore(DAY_STARTS) && time.isBefore(DAY_ENDS);
    }

    /** A group of one has nobody else to wait for, so there's nothing to warn its leader about. */
    static boolean isLowTurnout(int going, int members) {
        return members >= ENOUGH_GOING && going < ENOUGH_GOING;
    }
}
