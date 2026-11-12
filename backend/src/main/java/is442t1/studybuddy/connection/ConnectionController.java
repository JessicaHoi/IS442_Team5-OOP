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

/** Student endpoints for buddy requests and connections. */
@RestController
@RequestMapping("/api/connections")
@RequireRole(Role.STUDENT)
public class ConnectionController {

    private final ConnectionService connectionService;

    /**
     * Creates the connection controller.
     *
     * @param connectionService the connection service
     */
    public ConnectionController(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    /**
     * Sends a buddy request.
     *
     * @param caller the authenticated student
     * @param request the recipient and message
     * @return the created request
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConnectionResponse sendRequest(@CurrentUser AuthenticatedUser caller,
            @Valid @RequestBody CreateMatchRequest request) {
        return connectionService.sendRequest(caller.id(), request);
    }

    /**
     * Lists the caller's connections or requests.
     *
     * @param caller the authenticated student
     * @param view the requested view
     * @return matching connections or requests
     */
    @GetMapping
    public List<ConnectionResponse> list(@CurrentUser AuthenticatedUser caller,
            @RequestParam(defaultValue = "active") String view) {
        return connectionService.list(caller.id(), view);
    }

    /**
     * Accepts a buddy request.
     *
     * @param caller the authenticated student
     * @param requestId the request identifier
     * @return the accepted request
     */
    @PostMapping("/{requestId}/accept")
    public ConnectionResponse accept(@CurrentUser AuthenticatedUser caller, @PathVariable UUID requestId) {
        return connectionService.accept(caller.id(), requestId);
    }

    /**
     * Declines a buddy request.
     *
     * @param caller the authenticated student
     * @param requestId the request identifier
     * @return the declined request
     */
    @PostMapping("/{requestId}/decline")
    public ConnectionResponse decline(@CurrentUser AuthenticatedUser caller, @PathVariable UUID requestId) {
        return connectionService.decline(caller.id(), requestId);
    }

    /**
     * Ends a buddy connection.
     *
     * @param caller the authenticated student
     * @param connectionId the connection identifier
     * @return an empty HTTP 204 response
     */
    @DeleteMapping("/{connectionId}")
    public ResponseEntity<Void> end(@CurrentUser AuthenticatedUser caller, @PathVariable UUID connectionId) {
        connectionService.end(caller.id(), connectionId);
        return ResponseEntity.noContent().build();
    }
}
