package is442t1.studybuddy.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import is442t1.studybuddy.auth.AuthenticatedUser;
import is442t1.studybuddy.auth.CurrentUser;
import is442t1.studybuddy.auth.RequireRole;
import is442t1.studybuddy.model.enums.Role;
import is442t1.studybuddy.group.LedGroupResponse;
import is442t1.studybuddy.group.TransferLeadershipRequest;

@RestController
@RequestMapping("/api/admin/users")
@RequireRole(Role.ADMIN)
public class UserAccountController {

    private final UserAccountService accountService;

    public UserAccountController(UserAccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public List<UserAccountResponse> listAccounts() {
        return accountService.listAccounts();
    }

    @PostMapping
    public ResponseEntity<UserAccountResponse> createAccount(@CurrentUser AuthenticatedUser caller,
            @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(caller.id(), request));
    }

    @PutMapping("/{id}")
    public UserAccountResponse updateAccount(@CurrentUser AuthenticatedUser caller, @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        return accountService.updateAccount(caller.id(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@CurrentUser AuthenticatedUser caller, @PathVariable UUID id) {
        accountService.deleteAccount(caller.id(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/led-groups")
    public List<LedGroupResponse> listLedGroups(@PathVariable UUID id) {
        return accountService.listLedGroups(id);
    }

    @PutMapping("/{id}/led-groups/{groupId}/leader")
    public LedGroupResponse transferLeadership(@CurrentUser AuthenticatedUser caller, @PathVariable UUID id,
            @PathVariable UUID groupId, @Valid @RequestBody TransferLeadershipRequest request) {
        return accountService.transferLeadership(caller.id(), id, groupId, request.studentId());
    }
}
