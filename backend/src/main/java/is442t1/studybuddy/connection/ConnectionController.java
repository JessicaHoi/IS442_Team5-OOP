package is442t1.studybuddy.connection;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import is442t1.studybuddy.auth.AuthenticatedUser;
import is442t1.studybuddy.auth.CurrentUser;
import is442t1.studybuddy.auth.RequireRole;
import is442t1.studybuddy.model.enums.Role;

/**
 * Student-only buddy-request and connection endpoints. Accept and decline use request IDs; ending a
 *     connection uses the separate connection ID.
 */
@RestController
@RequestMapping("/api/connections")
@RequireRole(Role.STUDENT)
public class ConnectionController {

    private final ConnectionService connectionService;

    public ConnectionController(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    /**
     * Sends a buddy invitation: {@code POST /api/connections}, student only.
     * Body {@code {"toStudentId":"student-uuid","message":"Optional introduction"}}.
     * Returns HTTP 201 with a request ID; self-invitation is 400, a missing student is 404,
     * and a pending/active relationship or inactive participant is 409.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param request JSON body described above
     * @return response described above, with the documented HTTP status and payload
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConnectionResponse sendRequest(@CurrentUser AuthenticatedUser caller,
            @Valid @RequestBody CreateMatchRequest request) {
        return connectionService.sendRequest(caller.id(), request);
    }

    /**
     * Lists caller relationships: {@code GET /api/connections?view=active}, student only.
     * The view is incoming, sent or active (default). Returns HTTP 200, newest first.
     * Pending lists use request IDs; active entries use connection IDs and status ACCEPTED. An
     *     invalid view is 400.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param view incoming, sent or active; active is the default
     * @return response described above, with the documented HTTP status and payload
     */
    @GetMapping
    public List<ConnectionResponse> list(@CurrentUser AuthenticatedUser caller,
            @RequestParam(defaultValue = "active") String view) {
        return connectionService.list(caller.id(), view);
    }

    /**
     * Accepts an invitation: {@code POST /api/connections/{requestId}/accept}, recipient only.
     * Returns HTTP 200 with the answered request ID and visible buddy contact details.
     * Fetch the active list for the separate connection ID. A different caller is 403,
     * a missing request is 404 and an answered request or inactive participant is 409.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param requestId pending request UUID from the path
     * @return response described above, with the documented HTTP status and payload
     */
    @PostMapping("/{requestId}/accept")
    public ConnectionResponse accept(@CurrentUser AuthenticatedUser caller, @PathVariable UUID requestId) {
        return connectionService.accept(caller.id(), requestId);
    }

    /**
     * Declines an invitation: {@code POST /api/connections/{requestId}/decline}, recipient only.
     * Returns HTTP 200 with the request ID and status DECLINED. A different caller is 403,
     * a missing request is 404 and an already answered request is 409.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param requestId pending request UUID from the path
     * @return response described above, with the documented HTTP status and payload
     */
    @PostMapping("/{requestId}/decline")
    public ConnectionResponse decline(@CurrentUser AuthenticatedUser caller, @PathVariable UUID requestId) {
        return connectionService.decline(caller.id(), requestId);
    }

    /**
     * Ends a buddy connection: {@code DELETE /api/connections/{connectionId}}, either participant.
     * Use a connection ID from the active list, not an invitation ID. Returns HTTP 204 and hides
     *     contact details again.
     * A non-participant is 403, a missing connection is 404 and an ended connection is 409.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param connectionId persisted connection UUID from the active connection list
     * @return response described above, with the documented HTTP status and payload
     */
    @DeleteMapping("/{connectionId}")
    public ResponseEntity<Void> end(@CurrentUser AuthenticatedUser caller, @PathVariable UUID connectionId) {
        connectionService.end(caller.id(), connectionId);
        return ResponseEntity.noContent().build();
    }
}
