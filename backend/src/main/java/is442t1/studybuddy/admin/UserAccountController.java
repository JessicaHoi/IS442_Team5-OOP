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

/**
 * Administrator-only account CRUD, membership inspection and leadership transfer endpoints under
 *     /api/admin/users.
 */
@RestController
@RequestMapping("/api/admin/users")
@RequireRole(Role.ADMIN)
public class UserAccountController {

    private final UserAccountService accountService;

    public UserAccountController(UserAccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Lists accounts with usage counts: {@code GET /api/admin/users}, administrator only.
     * Returns HTTP 200; a student caller receives 403.
     *
     * @return response described above, with the documented HTTP status and payload
     */
    @GetMapping
    public List<UserAccountResponse> listAccounts() {
        return accountService.listAccounts();
    }

    /**
     * Creates an account: {@code POST /api/admin/users}, administrator only.
     * The body is {@link CreateUserRequest} . Returns HTTP 201; invalid fields are 400 and
     *     duplicate email is 409.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param request JSON body described above
     * @return response described above, with the documented HTTP status and payload
     */
    @PostMapping
    public ResponseEntity<UserAccountResponse> createAccount(@CurrentUser AuthenticatedUser caller,
            @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(caller.id(), request));
    }

    /**
     * Updates an account: {@code PUT /api/admin/users/{id}}, administrator only.
     * The body is {@link UpdateUserRequest}; role is immutable and password is optional.
     * Returns HTTP 200; changing role or suspending oneself is 400, duplicate email is 409,
     * and a missing account is 404.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param id resource UUID from the path, with its meaning described above
     * @param request JSON body described above
     * @return response described above, with the documented HTTP status and payload
     */
    @PutMapping("/{id}")
    public UserAccountResponse updateAccount(@CurrentUser AuthenticatedUser caller, @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        return accountService.updateAccount(caller.id(), id, request);
    }

    /**
     * Permanently deletes an account: {@code DELETE /api/admin/users/{id}}, administrator only.
     * Returns HTTP 204; self-deletion is 400, remaining group leadership is 409 and a missing
     *     account is 404.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param id resource UUID from the path, with its meaning described above
     * @return response described above, with the documented HTTP status and payload
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@CurrentUser AuthenticatedUser caller, @PathVariable UUID id) {
        accountService.deleteAccount(caller.id(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lists an account's groups, including closed groups and members:
     *     {@code GET /api/admin/users/{id} /groups}.
     * Administrator only. Returns HTTP 200; a missing account is 404.
     *
     * @param id resource UUID from the path, with its meaning described above
     * @return response described above, with the documented HTTP status and payload
     */
    @GetMapping("/{id}/groups")
    public List<UserGroupResponse> listUserGroups(@PathVariable UUID id) {
        return accountService.listUserGroups(id);
    }

    /**
     * Lists groups led by an account and their eligible successors:
     *     {@code GET /api/admin/users/{id} /led-groups}.
     * Administrator only. Returns HTTP 200; a missing account is 404.
     *
     * @param id resource UUID from the path, with its meaning described above
     * @return response described above, with the documented HTTP status and payload
     */
    @GetMapping("/{id}/led-groups")
    public List<LedGroupResponse> listLedGroups(@PathVariable UUID id) {
        return accountService.listLedGroups(id);
    }

    /**
     * Transfers an account's group to another active member: {@code PUT /api/admin/users/{id}
     *     /led-groups/{groupId}/leader}.
     * Administrator only; body {@code {"studentId":"successor-uuid"} }. Works for suspended leaders
     *     and closed groups.
     * Returns HTTP 200 with the leadership view; an invalid successor is 400 and a group not led by
     *     this account is 404.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param id resource UUID from the path, with its meaning described above
     * @param groupId study group UUID from the path
     * @param request JSON body described above
     * @return response described above, with the documented HTTP status and payload
     */
    @PutMapping("/{id}/led-groups/{groupId}/leader")
    public LedGroupResponse transferLeadership(@CurrentUser AuthenticatedUser caller, @PathVariable UUID id,
            @PathVariable UUID groupId, @Valid @RequestBody TransferLeadershipRequest request) {
        return accountService.transferLeadership(caller.id(), id, groupId, request.studentId());
    }
}
