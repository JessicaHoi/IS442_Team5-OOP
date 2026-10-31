package is442t1.studybuddy.telegram.chat;

import java.util.Optional;
import java.util.UUID;

import is442t1.studybuddy.model.enums.RsvpResponse;

/** What a Going or Can't button says, packed into Telegram's 64-byte callback data: {@code rsvp:<uuid>:G}. */
record RsvpPress(UUID sessionId, RsvpResponse response) {

    private static final String PREFIX = "rsvp:";
    private static final int ID_LENGTH = 36;

    String data() {
        return PREFIX + sessionId + ":" + (response == RsvpResponse.GOING ? "G" : "N");
    }

    /** Empty for anything StudyBuddy's buttons don't send. */
    static Optional<RsvpPress> parse(String data) {
        if (data == null || data.length() != PREFIX.length() + ID_LENGTH + 2 || !data.startsWith(PREFIX)) {
            return Optional.empty();
        }
        RsvpResponse response = switch (data.substring(data.length() - 2)) {
            case ":G" -> RsvpResponse.GOING;
            case ":N" -> RsvpResponse.NOT_GOING;
            default -> null;
        };
        if (response == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new RsvpPress(UUID.fromString(data.substring(PREFIX.length(), PREFIX.length() + ID_LENGTH)), response));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
