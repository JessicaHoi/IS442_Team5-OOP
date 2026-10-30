package is442t1.studybuddy.studysession;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

/** The calendar file format, tested without Spring. */
class IcsWriterTest {

    private static final Instant START = Instant.parse("2026-11-04T11:00:00Z");
    private static final Instant END = Instant.parse("2026-11-04T13:00:00Z");
    private static final Instant NOW = Instant.parse("2026-11-01T00:00:00Z");

    private static IcsWriter.Event event(String title, String description, boolean cancelled, String url) {
        return new IcsWriter.Event("abc@studybuddy", 2, title, description, null, START, END, cancelled, url);
    }

    private static String unfolded(String file) {
        return file.replace("\r\n ", "");
    }

    @Test
    void writesAConfirmedEventWithUtcTimesAndAReminder() {
        String file = IcsWriter.write(event("Paper drill", null, false, "https://app.example/groups/1"), NOW);

        assertThat(file).startsWith("BEGIN:VCALENDAR\r\n").endsWith("END:VCALENDAR\r\n");
        assertThat(file).contains("METHOD:PUBLISH\r\n", "UID:abc@studybuddy\r\n", "SEQUENCE:2\r\n",
                "DTSTAMP:20261101T000000Z\r\n", "DTSTART:20261104T110000Z\r\n", "DTEND:20261104T130000Z\r\n",
                "SUMMARY:Paper drill\r\n", "STATUS:CONFIRMED\r\n", "URL:https://app.example/groups/1\r\n",
                "TRIGGER:-PT30M\r\n");
    }

    @Test
    void everyLineEndsInCrlf() {
        String file = IcsWriter.write(event("Paper drill", "Line one\nLine two", false, null), NOW);

        assertThat(file.replace("\r\n", "")).doesNotContain("\n").doesNotContain("\r");
    }

    @Test
    void aCancelledEventHasNoReminder() {
        String file = IcsWriter.write(event("Paper drill", null, true, null), NOW);

        assertThat(file).contains("STATUS:CANCELLED\r\n").doesNotContain("VALARM");
    }

    @Test
    void escapesTextSoItCannotAddCalendarProperties() {
        String file = IcsWriter.write(event("a,b;c\\d", "one\r\nATTENDEE:evil@example.com", false, null), NOW);

        assertThat(file).contains("SUMMARY:a\\,b\\;c\\\\d\r\n");
        assertThat(file).contains("DESCRIPTION:one\\nATTENDEE:evil@example.com\r\n");
        assertThat(file).doesNotContain("\r\nATTENDEE:");
    }

    @Test
    void foldsAt75OctetsWithoutSplittingMultibyteCharacters() {
        String title = "复习".repeat(30) + "😀".repeat(30);

        String file = IcsWriter.write(event(title, null, false, null), NOW);

        assertThat(Arrays.stream(file.split("\r\n")))
                .allSatisfy(line -> assertThat(line.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(75));
        assertThat(new String(file.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)).doesNotContain("�");
        assertThat(unfolded(file)).contains("SUMMARY:" + title + "\r\n");
    }
}
