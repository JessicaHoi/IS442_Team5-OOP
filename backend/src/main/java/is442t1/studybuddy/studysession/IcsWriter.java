package is442t1.studybuddy.studysession;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Writes one calendar event as an RFC 5545 file by hand, to avoid a dependency:
 * CRLF line ends, text escaping, and folding at 75 octets without splitting a character.
 */
final class IcsWriter {

    private static final DateTimeFormatter UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final int MAX_OCTETS = 75;

    /** {@code description}, {@code location} and {@code url} may be null. */
    record Event(String uid, int sequence, String title, String description, String location, Instant startsAt,
            Instant endsAt, boolean cancelled, String url) {
    }

    private IcsWriter() {
    }

    static String write(Event event, Instant now) {
        StringBuilder out = new StringBuilder();
        line(out, "BEGIN:VCALENDAR");
        line(out, "VERSION:2.0");
        line(out, "PRODID:-//StudyBuddy//Study Sessions//EN");
        line(out, "CALSCALE:GREGORIAN");
        line(out, "METHOD:PUBLISH");
        line(out, "BEGIN:VEVENT");
        line(out, "UID:" + event.uid());
        line(out, "SEQUENCE:" + event.sequence());
        line(out, "DTSTAMP:" + UTC.format(now));
        line(out, "DTSTART:" + UTC.format(event.startsAt()));
        line(out, "DTEND:" + UTC.format(event.endsAt()));
        line(out, "SUMMARY:" + escape(event.title()));
        if (event.description() != null) {
            line(out, "DESCRIPTION:" + escape(event.description()));
        }
        if (event.location() != null) {
            line(out, "LOCATION:" + escape(event.location()));
        }
        line(out, "STATUS:" + (event.cancelled() ? "CANCELLED" : "CONFIRMED"));
        if (event.url() != null) {
            line(out, "URL:" + event.url());
        }
        if (!event.cancelled()) {
            line(out, "BEGIN:VALARM");
            line(out, "ACTION:DISPLAY");
            line(out, "DESCRIPTION:" + escape(event.title()));
            line(out, "TRIGGER:-PT30M");
            line(out, "END:VALARM");
        }
        line(out, "END:VEVENT");
        line(out, "END:VCALENDAR");
        return out.toString();
    }

    /** Line breaks become the two characters \n, so user text can never start a calendar property of its own. */
    private static String escape(String text) {
        return text.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replaceAll("\\r\\n|\\r|\\n", "\\\\n")
                .replaceAll("[\\p{Cntrl}&&[^\\t]]", "");
    }

    private static void line(StringBuilder out, String content) {
        int octets = 0;
        for (int i = 0; i < content.length(); ) {
            int codePoint = content.codePointAt(i);
            int charCount = Character.charCount(codePoint);
            int size = content.substring(i, i + charCount).getBytes(StandardCharsets.UTF_8).length;
            if (octets + size > MAX_OCTETS) {
                out.append("\r\n ");
                octets = 1;
            }
            out.appendCodePoint(codePoint);
            octets += size;
            i += charCount;
        }
        out.append("\r\n");
    }
}
