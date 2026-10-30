package is442t1.studybuddy.studysession;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import is442t1.studybuddy.model.enums.RsvpResponse;
import is442t1.studybuddy.model.enums.SessionStatus;
import is442t1.studybuddy.model.enums.StudyMode;

/** A study session as members see it. Only members can reach it, so attendee names are not hidden. */
public record StudySessionResponse(
        UUID id,
        UUID groupId,
        String groupName,
        String title,
        Instant startsAt,
        Instant endsAt,
        StudyMode meetingMode,
        String location,
        String note,
        SessionStatus status,
        RsvpResponse myRsvp,
        long goingCount,
        long notGoingCount,
        List<Attendee> attendees
) {

    public record Attendee(UUID id, String name, RsvpResponse response) {
    }

    static StudySessionResponse from(StudySession session, UUID viewerId) {
        List<Attendee> attendees = session.getRsvps().stream()
                .map(rsvp -> new Attendee(rsvp.getStudent().getId(), rsvp.getStudent().getName(), rsvp.getResponse()))
                .sorted(Comparator.comparing(Attendee::name))
                .toList();
        return new StudySessionResponse(
                session.getId(),
                session.getStudyGroup().getId(),
                session.getStudyGroup().getGroupName(),
                session.getTitle(),
                session.getStartsAt(),
                session.getEndsAt(),
                session.getMeetingMode(),
                session.getLocation(),
                session.getNote(),
                session.getStatus(),
                session.findRsvp(viewerId).map(SessionRsvp::getResponse).orElse(null),
                attendees.stream().filter(attendee -> attendee.response() == RsvpResponse.GOING).count(),
                attendees.stream().filter(attendee -> attendee.response() == RsvpResponse.NOT_GOING).count(),
                attendees);
    }
}
