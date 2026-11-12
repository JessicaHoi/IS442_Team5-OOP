package is442t1.studybuddy.studysession;

import java.time.Instant;

import is442t1.studybuddy.model.enums.StudyMode;

/**
 * The fields a leader sets when scheduling or changing a study session.
 *
 * @param title the session title
 * @param startsAt the session start time
 * @param endsAt the session end time
 * @param meetingMode the meeting mode
 * @param location the meeting location
 * @param note the session note
 * @param meetingLink    where to join an online session
 * @param groupVideoChat whether an online session meets in the video chat of the group's Telegram chat instead
 */
public record SessionDetails(String title, Instant startsAt, Instant endsAt, StudyMode meetingMode,
        String location, String note, String meetingLink, boolean groupVideoChat) {
}
