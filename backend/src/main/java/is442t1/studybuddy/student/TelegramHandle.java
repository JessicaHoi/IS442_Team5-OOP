package is442t1.studybuddy.student;

import java.util.regex.Pattern;

import is442t1.studybuddy.common.exception.ValidationException;

/** Turns what a student typed ({@code @name} or a {@code t.me} link) into a bare Telegram username. */
final class TelegramHandle {

    private static final Pattern LINK_PREFIX = Pattern.compile("^(https?://)?(www\\.)?t\\.me/", Pattern.CASE_INSENSITIVE);
    private static final Pattern VALID = Pattern.compile("[A-Za-z][A-Za-z0-9_]{3,30}[A-Za-z0-9]");

    private TelegramHandle() {
    }

    /** The bare username, or {@code null} if the student left the field empty. */
    static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String handle = LINK_PREFIX.matcher(raw.trim()).replaceFirst("");
        handle = handle.startsWith("@") ? handle.substring(1) : handle;
        if (!VALID.matcher(handle).matches()) {
            throw new ValidationException("Enter a valid Telegram username, e.g. @aisha_r.");
        }
        return handle;
    }
}
