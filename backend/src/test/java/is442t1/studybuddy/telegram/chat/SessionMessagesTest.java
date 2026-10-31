package is442t1.studybuddy.telegram.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import is442t1.studybuddy.model.enums.RsvpResponse;
import is442t1.studybuddy.model.enums.SessionStatus;
import is442t1.studybuddy.model.enums.StudyMode;

class SessionMessagesTest {

    private static final ZoneId SINGAPORE = ZoneId.of("Asia/Singapore");
    private static final Instant START = Instant.parse("2026-11-04T11:00:00Z");

    private static SessionMessages.Details details(Instant end, StudyMode mode, String location, String note) {
        return new SessionMessages.Details("Mock exam", START, end, mode, location, note, null, false,
                List.of("Aisha Rahman", "Marcus Lim"), List.of("Wei Jie"), List.of("Priya Nair"));
    }

    @Test
    void aCardShowsTheDetailsAndWhoIsComingInTheAppsZone() {
        assertThat(SessionMessages.card(details(START.plusSeconds(7200), StudyMode.IN_PERSON, "SCIS SR 2-1", "Bring notes"), CardPhase.OPEN, SINGAPORE))
                .isEqualTo("""
                        📅 Mock exam
                        Wed 4 Nov, 19:00–21:00 · In person · SCIS SR 2-1
                        Bring notes

                        ✅ Going (2): Aisha Rahman, Marcus Lim
                        ❌ Can't (1): Wei Jie
                        ❓ No answer (1): Priya Nair""");
    }

    @Test
    void aCardLeavesOutWhatIsNotSet() {
        assertThat(SessionMessages.card(new SessionMessages.Details("Mock exam", START, START.plusSeconds(7200), StudyMode.ONLINE, null, null, null, false,
                List.of(), List.of(), List.of("Priya Nair")), CardPhase.OPEN, SINGAPORE))
                .isEqualTo("""
                        📅 Mock exam
                        Wed 4 Nov, 19:00–21:00 · Online

                        ✅ Going (0)
                        ❌ Can't (0)
                        ❓ No answer (1): Priya Nair""");
    }

    @Test
    void aFinishedCardNoLongerAsksForAnswers() {
        assertThat(SessionMessages.card(details(START.plusSeconds(7200), StudyMode.ONLINE, null, null), CardPhase.ENDED, SINGAPORE))
                .startsWith("✔️ Finished: Mock exam").doesNotContain("No answer");
    }

    @Test
    void aCancelledCardIsShortAndNamesBothDaysPastMidnight() {
        assertThat(SessionMessages.card(details(Instant.parse("2026-11-04T17:00:00Z"), StudyMode.ONLINE, null, null), CardPhase.CANCELLED, SINGAPORE))
                .isEqualTo("❌ Cancelled: Mock exam\nWed 4 Nov, 19:00 – Thu 5 Nov, 01:00 · Online");
    }

    @Test
    void aReminderCountsWhoIsGoingAndWhoHasNotAnswered() {
        assertThat(SessionMessages.reminder(details(START.plusSeconds(7200), StudyMode.ONLINE, null, null), SINGAPORE))
                .isEqualTo("⏰ Starting soon: Mock exam at 19:00\n2 going, 1 hasn't answered");
    }

    @Test
    void aSessionsPhaseFollowsItsStatusAndTheClock() {
        Instant end = START.plusSeconds(7200);
        assertThat(CardPhase.of(SessionStatus.SCHEDULED, START, end, START.minusSeconds(1))).isEqualTo(CardPhase.OPEN);
        assertThat(CardPhase.of(SessionStatus.SCHEDULED, START, end, START)).isEqualTo(CardPhase.STARTED);
        assertThat(CardPhase.of(SessionStatus.SCHEDULED, START, end, end)).isEqualTo(CardPhase.ENDED);
        assertThat(CardPhase.of(SessionStatus.CANCELLED, START, end, START.minusSeconds(1))).isEqualTo(CardPhase.CANCELLED);
    }

    @Test
    void aPressFitsTelegramsLimitAndReadsBack() {
        RsvpPress press = new RsvpPress(UUID.randomUUID(), RsvpResponse.NOT_GOING);

        assertThat(press.data().getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(64);
        assertThat(RsvpPress.parse(press.data())).contains(press);
    }

    @Test
    void anythingElseIsNotAPress() {
        String id = UUID.randomUUID().toString();

        assertThat(RsvpPress.parse(null)).isEmpty();
        assertThat(RsvpPress.parse("rsvp:" + id + ":X")).isEmpty();
        assertThat(RsvpPress.parse("nope:" + id + ":G")).isEmpty();
        assertThat(RsvpPress.parse("rsvp:not-a-uuid-not-a-uuid-not-a-uuid-1234:G")).isEmpty();
    }

    @Test
    void countsTagPositionsTheWayTelegramDoes() {
        SessionMessages.Tagged tagged = SessionMessages.videoChatStarted("Mock 🎓", List.of(
                new SessionMessages.Attendee("Aisha", 1), new SessionMessages.Attendee("Marcus Lim", 2)));

        assertThat(tagged.entities()).hasSize(2);
        assertThat(tagged.entities()).allSatisfy(entity ->
                assertThat(tagged.text().substring(entity.offset(), entity.offset() + entity.length())).isEqualTo(entity.user().firstName()));
    }

    @Test
    void writesTheVideoChatAndMeetingLinkOntoTheCard() {
        Instant end = START.plusSeconds(3600);

        assertThat(SessionMessages.card(new SessionMessages.Details("Mock", START, end, StudyMode.ONLINE, null, null, "https://m.example.com", false, List.of(), List.of(), List.of()), CardPhase.OPEN, SINGAPORE))
                .contains("Join: https://m.example.com");
        assertThat(SessionMessages.card(new SessionMessages.Details("Mock", START, end, StudyMode.ONLINE, null, null, null, true, List.of(), List.of(), List.of()), CardPhase.OPEN, SINGAPORE))
                .contains("Video chat in this Telegram group");
    }
}
