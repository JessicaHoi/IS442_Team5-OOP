package is442t1.studybuddy.studysession;

import java.time.Instant;

import is442t1.studybuddy.model.enums.StudyMode;

/** The fields a leader sets when scheduling or changing a study session. */
public record SessionDetails(String title, Instant startsAt, Instant endsAt, StudyMode meetingMode,
        String location, String note) {
}
