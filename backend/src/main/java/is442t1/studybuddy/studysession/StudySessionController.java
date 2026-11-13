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

    public StudySessionController(StudySessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * Lists the caller's scheduled sessions across their groups: {@code GET /api/sessions/upcoming}
     *     , student only.
     * Returns HTTP 200 with sessions still to finish, including those in progress, soonest first;
     *     cancelled sessions are excluded.
     *
     * @param caller authenticated caller injected from the bearer token
     * @return response described above, with the documented HTTP status and payload
     */
    @GetMapping("/sessions/upcoming")
    public List<StudySessionResponse> listUpcoming(@CurrentUser AuthenticatedUser caller) {
        return sessionService.listUpcomingForStudent(caller.id());
    }

    /**
     * Lists group sessions: {@code GET /api/groups/{groupId} /sessions?scope=upcoming}, members
     *     only.
     * Upcoming (default) means not yet finished, soonest first; past means finished, newest first.
     * Both scopes include cancelled sessions. Returns HTTP 200; invalid scope is 400 and a
     *     non-member is 403.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param groupId study group UUID from the path
     * @param scope upcoming or past; upcoming is the default
     * @return response described above, with the documented HTTP status and payload
     */
    @GetMapping("/groups/{groupId}/sessions")
    public List<StudySessionResponse> list(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @RequestParam(defaultValue = "upcoming") String scope) {
        return sessionService.listForGroup(caller.id(), groupId, scope);
    }

    /**
     * Schedules a meeting: {@code POST /api/groups/{groupId}/sessions}, leader only.
     * The body is {@link StudySessionRequest} : ISO-8601 start/end instants, a concrete meeting
     *     mode and optional meeting details.
     * Start must be future, duration positive and at most eight hours; online options are mutually
     *     exclusive.
     * Returns HTTP 201; invalid details are 400, a non-leader is 403 and a closed group is 409.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param groupId study group UUID from the path
     * @param request JSON body described above
     * @return response described above, with the documented HTTP status and payload
     */
    @PostMapping("/groups/{groupId}/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public StudySessionResponse schedule(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @RequestBody StudySessionRequest request) {
        return sessionService.schedule(caller.id(), groupId, request.toDetails());
    }

    /**
     * Changes a meeting: {@code PUT /api/groups/{groupId}/sessions/{sessionId}}, leader only.
     * The body is {@link StudySessionRequest} . Moving the start clears RSVPs; an unchanged body
     *     leaves the session unchanged.
     * Returns HTTP 200; invalid details are 400, a non-leader is 403 and started/cancelled sessions
     *     are 409.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param groupId study group UUID from the path
     * @param sessionId study session UUID within this group
     * @param request JSON body described above
     * @return response described above, with the documented HTTP status and payload
     */
    @PutMapping("/groups/{groupId}/sessions/{sessionId}")
    public StudySessionResponse update(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @PathVariable UUID sessionId, @RequestBody StudySessionRequest request) {
        return sessionService.update(caller.id(), groupId, sessionId, request.toDetails());
    }

    /**
     * Cancels a meeting: {@code POST /api/groups/{groupId} /sessions/{sessionId}/cancel}, leader
     *     only.
     * Allowed through one hour after its end. Returns HTTP 200 with status CANCELLED;
     * a non-leader is 403 and an already cancelled session or one beyond the cancellation window is
     *     409.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param groupId study group UUID from the path
     * @param sessionId study session UUID within this group
     * @return response described above, with the documented HTTP status and payload
     */
    @PostMapping("/groups/{groupId}/sessions/{sessionId}/cancel")
    public StudySessionResponse cancel(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @PathVariable UUID sessionId) {
        return sessionService.cancel(caller.id(), groupId, sessionId);
    }

    /**
     * Saves an RSVP: {@code PUT /api/groups/{groupId} /sessions/{sessionId}/rsvp}, active members
     *     only.
     * Body {@code {"response":"GOING"} } or NOT_GOING. Returns HTTP 200 with attendance counts and
     *     answers.
     * An invalid response is 400, a non-member is 403 and started/cancelled sessions are 409.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param groupId study group UUID from the path
     * @param sessionId study session UUID within this group
     * @param request JSON body described above
     * @return response described above, with the documented HTTP status and payload
     */
    @PutMapping("/groups/{groupId}/sessions/{sessionId}/rsvp")
    public StudySessionResponse respond(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @PathVariable UUID sessionId, @RequestBody RsvpRequest request) {
        return sessionService.respond(caller.id(), groupId, sessionId, request);
    }

    /**
     * Exports a meeting: {@code GET /api/groups/{groupId} /sessions/{sessionId}/calendar.ics},
     *     members only.
     * Returns HTTP 200 with a text/calendar UTF-8 attachment, stable event UID and update sequence.
     * A non-member is 403 and a missing group/session is 404; application errors use JSON.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param groupId study group UUID from the path
     * @param sessionId study session UUID within this group
     * @return response described above, with the documented HTTP status and payload
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
