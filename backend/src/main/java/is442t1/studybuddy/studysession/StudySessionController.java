package is442t1.studybuddy.studysession;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import is442t1.studybuddy.auth.AuthenticatedUser;
import is442t1.studybuddy.auth.CurrentUser;
import is442t1.studybuddy.auth.RequireRole;
import is442t1.studybuddy.model.enums.Role;

/** Study sessions: leaders schedule them, members answer them and add them to a calendar. */
@RestController
@RequestMapping("/api")
@RequireRole(Role.STUDENT)
public class StudySessionController {

    private static final MediaType CALENDAR = MediaType.parseMediaType("text/calendar;charset=UTF-8");

    private final StudySessionService sessionService;

    /**
     * Creates the session controller.
     *
     * @param sessionService the study session service
     */
    public StudySessionController(StudySessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * Lists upcoming sessions across the caller's groups.
     *
     * @param caller the authenticated student
     * @return the upcoming sessions
     */
    @GetMapping("/sessions/upcoming")
    public List<StudySessionResponse> listUpcoming(@CurrentUser AuthenticatedUser caller) {
        return sessionService.listUpcomingForStudent(caller.id());
    }

    /**
     * Lists sessions for a group.
     *
     * @param caller the authenticated student
     * @param groupId the group identifier
     * @param scope the requested session scope
     * @return the matching sessions
     */
    @GetMapping("/groups/{groupId}/sessions")
    public List<StudySessionResponse> list(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @RequestParam(defaultValue = "upcoming") String scope) {
        return sessionService.listForGroup(caller.id(), groupId, scope);
    }

    /**
     * Schedules a group session.
     *
     * @param caller the authenticated student
     * @param groupId the group identifier
     * @param request the session details
     * @return the scheduled session
     */
    @PostMapping("/groups/{groupId}/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public StudySessionResponse schedule(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @RequestBody StudySessionRequest request) {
        return sessionService.schedule(caller.id(), groupId, request.toDetails());
    }

    /**
     * Updates a group session.
     *
     * @param caller the authenticated student
     * @param groupId the group identifier
     * @param sessionId the session identifier
     * @param request the replacement session details
     * @return the updated session
     */
    @PutMapping("/groups/{groupId}/sessions/{sessionId}")
    public StudySessionResponse update(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @PathVariable UUID sessionId, @RequestBody StudySessionRequest request) {
        return sessionService.update(caller.id(), groupId, sessionId, request.toDetails());
    }

    /**
     * Cancels a group session.
     *
     * @param caller the authenticated student
     * @param groupId the group identifier
     * @param sessionId the session identifier
     * @return the cancelled session
     */
    @PostMapping("/groups/{groupId}/sessions/{sessionId}/cancel")
    public StudySessionResponse cancel(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @PathVariable UUID sessionId) {
        return sessionService.cancel(caller.id(), groupId, sessionId);
    }

    /**
     * Records the caller's response to a session.
     *
     * @param caller the authenticated student
     * @param groupId the group identifier
     * @param sessionId the session identifier
     * @param request the response
     * @return the updated session
     */
    @PutMapping("/groups/{groupId}/sessions/{sessionId}/rsvp")
    public StudySessionResponse respond(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @PathVariable UUID sessionId, @RequestBody RsvpRequest request) {
        return sessionService.respond(caller.id(), groupId, sessionId, request);
    }

    /**
     * Downloads a session as a calendar file.
     *
     * @param caller the authenticated student
     * @param groupId the group identifier
     * @param sessionId the session identifier
     * @return the calendar file response
     */
    @GetMapping("/groups/{groupId}/sessions/{sessionId}/calendar.ics")
    public ResponseEntity<String> calendar(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @PathVariable UUID sessionId) {
        return ResponseEntity.ok()
                .contentType(CALENDAR)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("study-session.ics").build().toString())
                .body(sessionService.calendarFile(caller.id(), groupId, sessionId));
    }
}
