package is442t1.studybuddy.group;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import is442t1.studybuddy.auth.AuthenticatedUser;
import is442t1.studybuddy.auth.CurrentUser;
import is442t1.studybuddy.auth.RequireRole;
import is442t1.studybuddy.model.enums.Role;

/** Study groups: browsing and joining for students, management for group leaders. */
@RestController
@RequestMapping("/api/groups")
@RequireRole(Role.STUDENT)
public class StudyGroupController {

    private final StudyGroupService groupService;
    private final MembershipService membershipService;

    public StudyGroupController(StudyGroupService groupService, MembershipService membershipService) {
        this.groupService = groupService;
        this.membershipService = membershipService;
    }

    @GetMapping
    public List<StudyGroupResponse> listOpenGroups(@CurrentUser AuthenticatedUser caller,
            @RequestParam(required = false) String course) {
        return groupService.listOpenGroups(caller.id(), course);
    }

    @GetMapping("/mine")
    public List<StudyGroupResponse> listMyGroups(@CurrentUser AuthenticatedUser caller) {
        return groupService.listMyGroups(caller.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudyGroupResponse createGroup(@CurrentUser AuthenticatedUser caller,
            @Valid @RequestBody StudyGroupRequest request) {
        return groupService.createGroup(caller.id(), request);
    }

    @GetMapping("/{groupId}")
    public StudyGroupResponse getGroup(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId) {
        return groupService.getGroup(caller.id(), groupId);
    }

    @PutMapping("/{groupId}")
    public StudyGroupResponse updateGroup(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @Valid @RequestBody StudyGroupRequest request) {
        return groupService.updateGroup(caller.id(), groupId, request);
    }

    @PostMapping("/{groupId}/close")
    public StudyGroupResponse closeGroup(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId) {
        return groupService.closeGroup(caller.id(), groupId);
    }

    @PostMapping("/{groupId}/join-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public StudyGroupResponse requestToJoin(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @Valid @RequestBody JoinGroupRequest request) {
        return membershipService.requestToJoin(caller.id(), groupId, request.trimmedMessage());
    }

    @GetMapping("/{groupId}/join-requests")
    public List<JoinRequestResponse> listJoinRequests(@CurrentUser AuthenticatedUser caller,
            @PathVariable UUID groupId) {
        return membershipService.listPendingRequests(caller.id(), groupId);
    }

    @PostMapping("/{groupId}/join-requests/{requestId}/accept")
    public StudyGroupResponse acceptJoinRequest(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @PathVariable UUID requestId) {
        return membershipService.acceptRequest(caller.id(), groupId, requestId);
    }

    @PostMapping("/{groupId}/join-requests/{requestId}/reject")
    public StudyGroupResponse rejectJoinRequest(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @PathVariable UUID requestId) {
        return membershipService.rejectRequest(caller.id(), groupId, requestId);
    }

    @DeleteMapping("/{groupId}/members/{studentId}")
    public ResponseEntity<Void> removeMember(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @PathVariable UUID studentId) {
        membershipService.removeMember(caller.id(), groupId, studentId);
        return ResponseEntity.noContent().build();
    }
}
