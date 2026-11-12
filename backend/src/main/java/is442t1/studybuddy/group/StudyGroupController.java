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
    private final GroupLeadershipService leadershipService;

    /**
     * Creates the controller.
     *
     * @param groupService study group operations
     * @param membershipService join request and membership operations
     * @param leadershipService leadership transfers
     */
    public StudyGroupController(StudyGroupService groupService, MembershipService membershipService,
            GroupLeadershipService leadershipService) {
        this.groupService = groupService;
        this.membershipService = membershipService;
        this.leadershipService = leadershipService;
    }

    /**
     * Lists open groups, optionally filtered by course.
     *
     * @param caller the authenticated student
     * @param course optional course code filter
     * @return the caller-personalized summaries of matching groups
     */
    @GetMapping
    public List<StudyGroupResponse> listOpenGroups(@CurrentUser AuthenticatedUser caller,
            @RequestParam(required = false) String course) {
        return groupService.listOpenGroups(caller.id(), course);
    }

    /**
     * Lists groups the caller leads or belongs to, including closed groups.
     *
     * @param caller the authenticated student
     * @return the caller's group summaries
     */
    @GetMapping("/mine")
    public List<StudyGroupResponse> listMyGroups(@CurrentUser AuthenticatedUser caller) {
        return groupService.listMyGroups(caller.id());
    }

    /**
     * Creates a study group led by the caller.
     *
     * @param caller the authenticated student
     * @param request the requested group settings
     * @return the created group's detail response
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudyGroupResponse createGroup(@CurrentUser AuthenticatedUser caller,
            @Valid @RequestBody StudyGroupRequest request) {
        return groupService.createGroup(caller.id(), request);
    }

    /**
     * Gets a group's detail response.
     *
     * @param caller the authenticated student
     * @param groupId the group's identifier
     * @return the group detail visible to the caller
     */
    @GetMapping("/{groupId}")
    public StudyGroupResponse getGroup(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId) {
        return groupService.getGroup(caller.id(), groupId);
    }

    /**
     * Updates a group's editable settings; its course cannot be changed.
     *
     * @param caller the authenticated student, who must lead the group
     * @param groupId the group's identifier
     * @param request the replacement group settings
     * @return the updated group's detail response
     */
    @PutMapping("/{groupId}")
    public StudyGroupResponse updateGroup(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @Valid @RequestBody StudyGroupRequest request) {
        return groupService.updateGroup(caller.id(), groupId, request);
    }

    /**
     * Gets leadership details for a group led by the caller.
     *
     * @param caller the authenticated student
     * @param groupId the group's identifier
     * @return the group's current leader and eligible successors
     */
    @GetMapping("/{groupId}/leadership")
    public LedGroupResponse getLeadership(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId) {
        return groupService.getLeadership(caller.id(), groupId);
    }

    /**
     * Transfers leadership to an eligible group member.
     *
     * @param caller the authenticated student, who must lead the group
     * @param groupId the group's identifier
     * @param request the selected successor
     * @return the leadership details after transfer
     */
    @PutMapping("/{groupId}/leader")
    public LedGroupResponse transferLeadership(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @Valid @RequestBody TransferLeadershipRequest request) {
        return leadershipService.transfer(caller.id(), groupId, request.studentId(), false);
    }

    /**
     * Closes a group led by the caller.
     *
     * @param caller the authenticated student
     * @param groupId the group's identifier
     * @return the closed group's detail response
     */
    @PostMapping("/{groupId}/close")
    public StudyGroupResponse closeGroup(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId) {
        return groupService.closeGroup(caller.id(), groupId);
    }

    /**
     * Submits the caller's request to join a group.
     *
     * @param caller the authenticated student
     * @param groupId the group's identifier
     * @param request the optional message to the leader
     * @return the group summary from the caller's perspective
     */
    @PostMapping("/{groupId}/join-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public StudyGroupResponse requestToJoin(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @Valid @RequestBody JoinGroupRequest request) {
        return membershipService.requestToJoin(caller.id(), groupId, request.trimmedMessage());
    }

    /**
     * Lists pending requests for a group of which the caller is a member.
     *
     * @param caller the authenticated student
     * @param groupId the group's identifier
     * @return pending requests, oldest first
     */
    @GetMapping("/{groupId}/join-requests")
    public List<JoinRequestResponse> listJoinRequests(@CurrentUser AuthenticatedUser caller,
            @PathVariable UUID groupId) {
        return membershipService.listPendingRequests(caller.id(), groupId);
    }

    /**
     * Accepts a pending join request.
     *
     * @param caller the authenticated student, who must lead the group
     * @param groupId the group's identifier
     * @param requestId the request's identifier
     * @return the updated group's detail response
     */
    @PostMapping("/{groupId}/join-requests/{requestId}/accept")
    public StudyGroupResponse acceptJoinRequest(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @PathVariable UUID requestId) {
        return membershipService.acceptRequest(caller.id(), groupId, requestId);
    }

    /**
     * Rejects a pending join request.
     *
     * @param caller the authenticated student, who must lead the group
     * @param groupId the group's identifier
     * @param requestId the request's identifier
     * @return the updated group's detail response
     */
    @PostMapping("/{groupId}/join-requests/{requestId}/reject")
    public StudyGroupResponse rejectJoinRequest(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @PathVariable UUID requestId) {
        return membershipService.rejectRequest(caller.id(), groupId, requestId);
    }

    /**
     * Removes the caller from a group.
     *
     * @param caller the authenticated student
     * @param groupId the group's identifier
     * @return an empty 204 response
     */
    @PostMapping("/{groupId}/leave")
    public ResponseEntity<Void> leave(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId) {
        membershipService.leaveGroup(caller.id(), groupId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Sets the group's Telegram chat link.
     *
     * @param caller the authenticated student, who must lead the group
     * @param groupId the group's identifier
     * @param request the new chat URL
     * @return the updated group's detail response
     */
    @PutMapping("/{groupId}/chat-link")
    public StudyGroupResponse setChatLink(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @RequestBody ChatLinkRequest request) {
        return groupService.setChatLink(caller.id(), groupId, request.url());
    }

    /**
     * Clears a group's Telegram chat link.
     *
     * @param caller the authenticated student, who must lead the group
     * @param groupId the group's identifier
     * @return an empty 204 response
     */
    @DeleteMapping("/{groupId}/chat-link")
    public ResponseEntity<Void> clearChatLink(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId) {
        groupService.clearChatLink(caller.id(), groupId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Removes a member from a group led by the caller.
     *
     * @param caller the authenticated student, who must lead the group
     * @param groupId the group's identifier
     * @param studentId the member's identifier
     * @return an empty 204 response
     */
    @DeleteMapping("/{groupId}/members/{studentId}")
    public ResponseEntity<Void> removeMember(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @PathVariable UUID studentId) {
        membershipService.removeMember(caller.id(), groupId, studentId);
        return ResponseEntity.noContent().build();
    }
}
