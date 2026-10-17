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

@RestController
@RequestMapping("/api/connections")
@RequireRole(Role.STUDENT)
public class ConnectionController {

    private final ConnectionService connectionService;

    public ConnectionController(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConnectionResponse sendRequest(@CurrentUser AuthenticatedUser caller,
            @Valid @RequestBody CreateMatchRequest request) {
        return connectionService.sendRequest(caller.id(), request);
    }

    @GetMapping
    public List<ConnectionResponse> list(@CurrentUser AuthenticatedUser caller,
            @RequestParam(defaultValue = "active") String view) {
        return connectionService.list(caller.id(), view);
    }

    @PostMapping("/{requestId}/accept")
    public ConnectionResponse accept(@CurrentUser AuthenticatedUser caller, @PathVariable UUID requestId) {
        return connectionService.accept(caller.id(), requestId);
    }

    @PostMapping("/{requestId}/decline")
    public ConnectionResponse decline(@CurrentUser AuthenticatedUser caller, @PathVariable UUID requestId) {
        return connectionService.decline(caller.id(), requestId);
    }

    @DeleteMapping("/{connectionId}")
    public ResponseEntity<Void> end(@CurrentUser AuthenticatedUser caller, @PathVariable UUID connectionId) {
        connectionService.end(caller.id(), connectionId);
        return ResponseEntity.noContent().build();
    }
}
