package is442t1.studybuddy.studysession;

import java.time.Instant;

import is442t1.studybuddy.model.enums.StudyMode;

/**
 * Body of creating or changing a session. Checked after leadership, so a non-leader always gets 403.
 *
 * @param title the session title
 * @param startsAt the session start time
 * @param endsAt the session end time
 * @param meetingMode the meeting mode
 * @param location the meeting location
 * @param note the session note
 * @param meetingLink the online meeting link
 * @param groupVideoChat whether to use the group's video chat
 */
public record StudySessionRequest(String title, Instant startsAt, Instant endsAt, StudyMode meetingMode,
        String location, String note, String meetingLink, Boolean groupVideoChat) {

    SessionDetails toDetails() {
        return new SessionDetails(title, startsAt, endsAt, meetingMode, location, note, meetingLink, Boolean.TRUE.equals(groupVideoChat));
    }
}
