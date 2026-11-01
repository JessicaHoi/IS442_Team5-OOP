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
            String meetingLink, boolean groupVideoChat, List<String> going, List<String> notGoing, List<String> noAnswer) {
    }

    /** Someone the bot can tag, because it knows their account in this chat. */
    record Attendee(String name, long telegramUserId) {
    }

    /** Text with the people tagged in it. */
    record Tagged(String text, List<MessageEntity> entities) {
    }

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

    private SessionMessages() {
    }

    /** The session's card: everything about it in one message, updated in place. */
    static String card(Details session, CardPhase phase, ZoneId zone) {
        List<String> lines = new ArrayList<>();
        lines.add(switch (phase) {
            case OPEN -> "📅 " + session.title();
            case STARTED -> "🟢 Happening now: " + session.title();
            case ENDED -> "✔️ Finished: " + session.title();
            case CANCELLED -> "❌ Cancelled: " + session.title();
        });
        lines.add(when(session, zone) + " · " + where(session));
        if (phase == CardPhase.CANCELLED) {
            return String.join("\n", lines);
        }
        if (session.meetingLink() != null) {
            lines.add("Join: " + session.meetingLink());
        }
        if (session.groupVideoChat()) {
            lines.add("Video chat in this Telegram group");
        }
        if (session.note() != null) {
            lines.add(session.note());
        }
        lines.add("");
        lines.add(names("✅ Going", session.going()));
        lines.add(names("❌ Can't", session.notGoing()));
        if (phase != CardPhase.ENDED) {
            lines.add(names("❓ No answer", session.noAnswer()));
        }
        return String.join("\n", lines);
    }

    private static String names(String label, List<String> people) {
        return label + " (" + people.size() + ")" + (people.isEmpty() ? "" : ": " + String.join(", ", people));
    }

    /** A short reply to the card, so people who follow notifications hear about the new time. */
    static String startMoved(Details session, ZoneId zone) {
        return "🕒 " + session.title() + " has moved to " + when(session, zone) + ". Please answer again.";
    }

    static String cancelled(Details session) {
        return "❌ " + session.title() + " has been cancelled.";
    }

    static String reminder(Details session, ZoneId zone) {
        return "⏰ Starting soon: " + session.title() + " at " + TIME.format(session.startsAt().atZone(zone)) + "\n"
                + session.going().size() + " going, " + notAnswered(session);
    }

    private static String notAnswered(Details session) {
        int silent = session.noAnswer().size();
        return silent + (silent == 1 ? " hasn't" : " haven't") + " answered";
    }

    /** A reply to the card that tags only the people it still needs an answer from. */
    static Tagged nudge(Details session, ZoneId zone, List<Attendee> silent) {
        return tag("👋 Can you make " + session.title() + "? " + when(session, zone) + "\nTap Going or Can't on the card above.",
                "Still to answer", silent);
    }

    /** A private message, so the leader can act while there's still time and the chat isn't told the session looks thin. */
    static String lowTurnout(Details session, ZoneId zone) {
        return "⚠️ " + session.title() + " (" + when(session, zone) + ") has " + session.going().size() + " going so far.\n"
                + session.notGoing().size() + " can't, " + notAnswered(session) + ". You can move or cancel it in StudyBuddy.";
    }

    /** A video chat session's reminder tags the people going, because a bot can't invite them to the call itself. */
    static Tagged videoChatReminder(Details session, ZoneId zone, List<Attendee> going) {
        return tag(reminder(session, zone) + "\nA chat admin needs to start the video chat here when it's time.", "Going", going);
    }

    static Tagged videoChatStarted(String title, List<Attendee> going) {
        return tag("🎥 The video chat for " + title + " has started. Join from the bar at the top of this chat.", "Going", going);
    }

    private static Tagged tag(String text, String label, List<Attendee> people) {
        if (people.isEmpty()) {
            return new Tagged(text, List.of());
        }
        StringBuilder tagged = new StringBuilder(text).append("\n").append(label).append(": ");
        List<MessageEntity> entities = new ArrayList<>();
        for (Attendee attendee : people) {
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
