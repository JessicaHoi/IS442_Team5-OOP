package is442t1.studybuddy.studysession;

import is442t1.studybuddy.model.enums.RsvpResponse;

/**
 * Body of {@code PUT /api/groups/{id}/sessions/{sessionId}/rsvp}.
 *
 * @param response the member's response to the session
 */
public record RsvpRequest(RsvpResponse response) {
}
