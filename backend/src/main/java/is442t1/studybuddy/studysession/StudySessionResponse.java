package is442t1.studybuddy.studysession;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import is442t1.studybuddy.model.enums.RsvpResponse;
import is442t1.studybuddy.model.enums.SessionStatus;
import is442t1.studybuddy.model.enums.StudyMode;

/**
 * A study session as members see it. Only members can reach it, so attendee names are not hidden.
 *
 * @param id the session identifier
 * @param groupId the group's identifier
 * @param groupName the group's name
 * @param title the session title
 * @param startsAt the session start time
 * @param endsAt the session end time
 * @param meetingMode the meeting mode
 * @param location the meeting location
 * @param note the session note
 * @param meetingLink the meeting link
 * @param groupVideoChat whether the group video chat is used
 * @param status the session status
 * @param myRsvp the viewer's response, if any
 * @param goingCount the number of members going
 * @param notGoingCount the number of members not going
 * @param attendees the members who responded
 */
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
        String meetingLink,
        boolean groupVideoChat,
        SessionStatus status,
        RsvpResponse myRsvp,
        long goingCount,
        long notGoingCount,
        List<Attendee> attendees
) {

    /**
     * A member's response to a session.
     *
     * @param id the student's identifier
     * @param name the student's name
     * @param response the student's response
     */
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
                session.getMeetingLink(),
                session.isGroupVideoChat(),
                session.getStatus(),
                session.findRsvp(viewerId).map(SessionRsvp::getResponse).orElse(null),
                attendees.stream().filter(attendee -> attendee.response() == RsvpResponse.GOING).count(),
                attendees.stream().filter(attendee -> attendee.response() == RsvpResponse.NOT_GOING).count(),
                attendees);
    }
}
