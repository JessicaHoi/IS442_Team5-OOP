package is442t1.studybuddy.studysession;

import java.time.Instant;

import is442t1.studybuddy.model.enums.StudyMode;

/** Body of creating or changing a session. Checked after leadership, so a non-leader always gets 403. */
public record StudySessionRequest(String title, Instant startsAt, Instant endsAt, StudyMode meetingMode,
        String location, String note, String meetingLink, Boolean groupVideoChat) {

    SessionDetails toDetails() {
        return new SessionDetails(title, startsAt, endsAt, meetingMode, location, note, meetingLink, Boolean.TRUE.equals(groupVideoChat));
    }
}
