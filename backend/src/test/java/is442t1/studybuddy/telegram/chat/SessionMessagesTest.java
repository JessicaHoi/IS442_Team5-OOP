package is442t1.studybuddy.telegram.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import is442t1.studybuddy.model.enums.RsvpResponse;
import is442t1.studybuddy.model.enums.StudyMode;

class SessionMessagesTest {

    private static final ZoneId SINGAPORE = ZoneId.of("Asia/Singapore");
    private static final Instant START = Instant.parse("2026-11-04T11:00:00Z");

    private static SessionMessages.Details details(Instant end, StudyMode mode, String location, String note) {
        return new SessionMessages.Details("Mock exam", START, end, mode, location, note, null, false, 3);
    }

    @Test
    void writesTheTimeInTheAppsZoneWithTheDetails() {
        assertThat(SessionMessages.scheduled(details(START.plusSeconds(7200), StudyMode.IN_PERSON, "SCIS SR 2-1", "Bring notes"), SINGAPORE))
                .isEqualTo("📅 New study session: Mock exam\nWed 4 Nov, 19:00–21:00\nIn person · SCIS SR 2-1\nBring notes");
    }

    @Test
    void leavesOutWhatIsNotSet() {
        assertThat(SessionMessages.changed(details(START.plusSeconds(7200), StudyMode.ONLINE, null, null), SINGAPORE))
                .isEqualTo("✏️ Study session updated: Mock exam\nWed 4 Nov, 19:00–21:00\nOnline");
    }

    @Test
    void namesBothDaysWhenASessionRunsPastMidnight() {
        assertThat(SessionMessages.cancelled(details(Instant.parse("2026-11-04T17:00:00Z"), StudyMode.ONLINE, null, null), SINGAPORE))
                .isEqualTo("❌ Study session cancelled: Mock exam\nWed 4 Nov, 19:00 – Thu 5 Nov, 01:00");
    }

    @Test
    void aReminderSaysHowManyAreGoing() {
        assertThat(SessionMessages.reminder(details(START.plusSeconds(7200), StudyMode.ONLINE, null, null), SINGAPORE))
                .startsWith("⏰ Starting soon: Mock exam").endsWith("3 going so far");
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
    void writesTheVideoChatAndMeetingLinkIntoThePost() {
        Instant end = START.plusSeconds(3600);

        assertThat(SessionMessages.scheduled(new SessionMessages.Details("Mock", START, end, StudyMode.ONLINE, null, null, "https://m.example.com", false, 0), SINGAPORE))
                .contains("Join: https://m.example.com");
        assertThat(SessionMessages.scheduled(new SessionMessages.Details("Mock", START, end, StudyMode.ONLINE, null, null, null, true, 0), SINGAPORE))
                .contains("Video chat in this Telegram group");
    }
}
