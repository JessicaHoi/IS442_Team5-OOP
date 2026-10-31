package is442t1.studybuddy.telegram.chat;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import is442t1.studybuddy.model.enums.RsvpResponse;
import is442t1.studybuddy.model.enums.StudyMode;
import is442t1.studybuddy.telegram.api.MessageEntity;
import is442t1.studybuddy.telegram.api.ReplyMarkup;

/** The wording of study session messages in a group chat. Sent as plain text, so nothing in them needs escaping. */
final class SessionMessages {

    /** A session's details, read in a transaction and safe to use outside one. */
    record Details(String title, Instant startsAt, Instant endsAt, StudyMode meetingMode, String location, String note,
            String meetingLink, boolean groupVideoChat, long goingCount) {
    }

    /** Someone going to a session whom the bot can tag, because it knows their account in this chat. */
    record Attendee(String name, long telegramUserId) {
    }

    /** Text with the people tagged in it. */
    record Tagged(String text, List<MessageEntity> entities) {
    }

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

    private SessionMessages() {
    }

    static String scheduled(Details session, ZoneId zone) {
        return withDetails("📅 New study session: " + session.title(), session, zone);
    }

    static String changed(Details session, ZoneId zone) {
        return withDetails("✏️ Study session updated: " + session.title(), session, zone);
    }

    static String cancelled(Details session, ZoneId zone) {
        return "❌ Study session cancelled: " + session.title() + "\n" + when(session, zone);
    }

    static String reminder(Details session, ZoneId zone) {
        return withDetails("⏰ Starting soon: " + session.title(), session, zone) + "\n" + session.goingCount() + " going so far";
    }

    /** A video chat session's reminder tags the people going, because a bot can't invite them to the call itself. */
    static Tagged videoChatReminder(Details session, ZoneId zone, List<Attendee> going) {
        return tag(reminder(session, zone) + "\nA chat admin needs to start the video chat here when it's time.", going);
    }

    static Tagged videoChatStarted(String title, List<Attendee> going) {
        return tag("🎥 The video chat for " + title + " has started. Join from the bar at the top of this chat.", going);
    }

    private static Tagged tag(String text, List<Attendee> going) {
        if (going.isEmpty()) {
            return new Tagged(text, List.of());
        }
        StringBuilder tagged = new StringBuilder(text).append("\nGoing: ");
        List<MessageEntity> entities = new ArrayList<>();
        for (Attendee attendee : going) {
            if (!entities.isEmpty()) {
                tagged.append(", ");
            }
            entities.add(MessageEntity.mention(tagged.length(), attendee.name().length(), attendee.telegramUserId(), attendee.name()));
            tagged.append(attendee.name());
        }
        return new Tagged(tagged.toString(), List.copyOf(entities));
    }

    static ReplyMarkup rsvpButtons(UUID sessionId) {
        return ReplyMarkup.ActionButtons.inARow(
                new ReplyMarkup.ActionButtons.Action("Going ✅", new RsvpPress(sessionId, RsvpResponse.GOING).data()),
                new ReplyMarkup.ActionButtons.Action("Can't ❌", new RsvpPress(sessionId, RsvpResponse.NOT_GOING).data()));
    }

    /** The brief notice someone sees after pressing a button. */
    static String answered(RsvpResponse response, long goingCount) {
        return (response == RsvpResponse.GOING ? "Going" : "Can't make it") + " · " + goingCount + " going";
    }

    private static String withDetails(String heading, Details session, ZoneId zone) {
        List<String> lines = new ArrayList<>(List.of(heading, when(session, zone), where(session)));
        if (session.meetingLink() != null) {
            lines.add("Join: " + session.meetingLink());
        }
        if (session.groupVideoChat()) {
            lines.add("Video chat in this Telegram group");
        }
        if (session.note() != null) {
            lines.add(session.note());
        }
        return String.join("\n", lines);
    }

    private static String when(Details session, ZoneId zone) {
        ZonedDateTime start = session.startsAt().atZone(zone);
        ZonedDateTime end = session.endsAt().atZone(zone);
        String startText = DAY.format(start) + ", " + TIME.format(start);
        return start.toLocalDate().equals(end.toLocalDate())
                ? startText + "–" + TIME.format(end)
                : startText + " – " + DAY.format(end) + ", " + TIME.format(end);
    }

    private static String where(Details session) {
        String mode = session.meetingMode() == StudyMode.ONLINE ? "Online" : "In person";
        return session.location() == null ? mode : mode + " · " + session.location();
    }
}
