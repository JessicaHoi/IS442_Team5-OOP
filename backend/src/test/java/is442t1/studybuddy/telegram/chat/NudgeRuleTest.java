package is442t1.studybuddy.telegram.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class NudgeRuleTest {

    private static final ZoneId SINGAPORE = ZoneId.of("Asia/Singapore");
    private static final Duration LEAD = Duration.ofHours(24);
    /** 19:00 in Singapore on Wed 4 Nov. */
    private static final Instant START = Instant.parse("2026-11-04T11:00:00Z");
    private static final Instant LONG_BEFORE = Instant.parse("2026-10-20T00:00:00Z");

    private static Instant singapore(String localTime) {
        return Instant.parse(localTime + "+08:00");
    }

    private static boolean dueAt(Instant now) {
        return NudgeRule.isDue(START, LONG_BEFORE, LEAD, SINGAPORE, now);
    }

    @Test
    void theWindowOpensAtTheLeadAndClosesThreeHoursBeforeTheStart() {
        assertThat(dueAt(START.minus(LEAD).minusSeconds(1))).isFalse();
        assertThat(dueAt(START.minus(LEAD))).isTrue();
        assertThat(dueAt(START.minus(Duration.ofHours(3)).minusSeconds(1))).isTrue();
        assertThat(dueAt(START.minus(Duration.ofHours(3)))).isFalse();
    }

    @Test
    void nothingGoesOutDuringQuietHours() {
        assertThat(dueAt(singapore("2026-11-03T21:59:59"))).isTrue();
        assertThat(dueAt(singapore("2026-11-03T22:00:00"))).isFalse();
        assertThat(dueAt(singapore("2026-11-04T07:59:59"))).isFalse();
        assertThat(dueAt(singapore("2026-11-04T08:00:00"))).isTrue();
    }

    @Test
    void aSessionWhoseWindowOpensOvernightIsNudgedInTheMorning() {
        Instant earlyStart = singapore("2026-11-05T02:00:00");

        assertThat(NudgeRule.isDue(earlyStart, LONG_BEFORE, LEAD, SINGAPORE, singapore("2026-11-04T07:00:00"))).isFalse();
        assertThat(NudgeRule.isDue(earlyStart, LONG_BEFORE, LEAD, SINGAPORE, singapore("2026-11-04T08:00:00"))).isTrue();
    }

    @Test
    void aSessionAnnouncedInsideTheWindowIsNotNudged() {
        Instant opens = START.minus(LEAD);
        Instant now = START.minus(Duration.ofHours(10));

        assertThat(NudgeRule.isDue(START, opens, LEAD, SINGAPORE, now)).isTrue();
        assertThat(NudgeRule.isDue(START, opens.plusSeconds(1), LEAD, SINGAPORE, now)).isFalse();
    }

    @Test
    void anUnknownAnnouncementDoesNotBlockANudge() {
        assertThat(NudgeRule.isDue(START, null, LEAD, SINGAPORE, START.minus(Duration.ofHours(10)))).isTrue();
    }

    @Test
    void fewerThanTwoGoingIsLowTurnoutUnlessTheLeaderIsAlone() {
        assertThat(NudgeRule.isLowTurnout(0, 4)).isTrue();
        assertThat(NudgeRule.isLowTurnout(1, 2)).isTrue();
        assertThat(NudgeRule.isLowTurnout(2, 4)).isFalse();
        assertThat(NudgeRule.isLowTurnout(0, 1)).isFalse();
        assertThat(NudgeRule.isLowTurnout(1, 1)).isFalse();
    }
}
