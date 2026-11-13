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

    public StudyGroupController(StudyGroupService groupService, MembershipService membershipService,
            GroupLeadershipService leadershipService) {
        this.groupService = groupService;
        this.membershipService = membershipService;
        this.leadershipService = leadershipService;
    }

    /**
     * Browses open groups: {@code GET /api/groups?course=IS442}, student only.
     * The course filter is optional. Returns HTTP 200 with newest-first summaries; member lists and
     *     chat links are omitted.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param course optional course-code filter
     * @return response described above, with the documented HTTP status and payload
     */
    @GetMapping
    public List<StudyGroupResponse> listOpenGroups(@CurrentUser AuthenticatedUser caller,
            @RequestParam(required = false) String course) {
        return groupService.listOpenGroups(caller.id(), course);
    }

    /**
     * Lists the caller's memberships in any status: {@code GET /api/groups/mine}, student only.
     * Returns HTTP 200 with led groups first, then group name; closed groups remain visible.
     *
     * @param caller authenticated caller injected from the bearer token
     * @return response described above, with the documented HTTP status and payload
     */
    @GetMapping("/mine")
    public List<StudyGroupResponse> listMyGroups(@CurrentUser AuthenticatedUser caller) {
        return groupService.listMyGroups(caller.id());
    }

    /**
     * Creates an open group: {@code POST /api/groups}, student only.
     * The body is {@link StudyGroupRequest}; caller becomes leader and first member.
     * Returns HTTP 201 with group details; missing/unknown course or invalid details are 400.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param request JSON body described above
     * @return response described above, with the documented HTTP status and payload
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudyGroupResponse createGroup(@CurrentUser AuthenticatedUser caller,
            @Valid @RequestBody StudyGroupRequest request) {
        return groupService.createGroup(caller.id(), request);
    }

    /**
     * Reads group details: {@code GET /api/groups/{groupId}}, student only.
     * Returns HTTP 200; members see the member list and any manual chat link, others get an empty
     *     member list.
     * A missing group is 404.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param groupId study group UUID from the path
     * @return response described above, with the documented HTTP status and payload
     */
    @GetMapping("/{groupId}")
    public StudyGroupResponse getGroup(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId) {
        return groupService.getGroup(caller.id(), groupId);
    }

    /**
     * Updates group details: {@code PUT /api/groups/{groupId}}, leader only.
     * The body is {@link StudyGroupRequest}; courseCode is ignored. Returns HTTP 200.
     * Invalid details are 400, a non-leader is 403, and a closed group or maximum below member
     *     count is 409.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param groupId study group UUID from the path
     * @param request JSON body described above
     * @return response described above, with the documented HTTP status and payload
     */
    @PutMapping("/{groupId}")
    public StudyGroupResponse updateGroup(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @Valid @RequestBody StudyGroupRequest request) {
        return groupService.updateGroup(caller.id(), groupId, request);
    }

    /**
     * Reads eligible successors: {@code GET /api/groups/{groupId}/leadership}, leader only.
     * Returns HTTP 200 with group status and active members other than the leader; a non-leader is
     *     403.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param groupId study group UUID from the path
     * @return response described above, with the documented HTTP status and payload
     */
    @GetMapping("/{groupId}/leadership")
    public LedGroupResponse getLeadership(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId) {
        return groupService.getLeadership(caller.id(), groupId);
    }

    /**
     * Transfers the group: {@code PUT /api/groups/{groupId}/leader}, current leader only.
     * Body {@code {"studentId":"successor-uuid"} } names another active member. Closed groups may
     *     also be transferred.
     * Returns HTTP 200 with the leadership view; an invalid successor is 400 and a non-leader is
     *     403.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param groupId study group UUID from the path
     * @param request JSON body described above
     * @return response described above, with the documented HTTP status and payload
     */
    @PutMapping("/{groupId}/leader")
    public LedGroupResponse transferLeadership(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @Valid @RequestBody TransferLeadershipRequest request) {
        return leadershipService.transfer(caller.id(), groupId, request.studentId(), false);
    }

    /**
     * Permanently closes a group: {@code POST /api/groups/{groupId}/close}, leader only.
     * Pending requests are rejected, future sessions are cancelled, and membership is preserved.
     * Returns HTTP 200 with group details; a non-leader is 403 and an already closed group is 409.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param groupId study group UUID from the path
     * @return response described above, with the documented HTTP status and payload
     */
    @PostMapping("/{groupId}/close")
    public StudyGroupResponse closeGroup(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId) {
        return groupService.closeGroup(caller.id(), groupId);
    }

    /**
     * Requests membership: {@code POST /api/groups/{groupId}/join-requests}, student only.
     * Body {@code {"message":"Optional introduction"}}. Returns HTTP 201 with a group summary.
     * Closed/full groups, existing members, inactive participants and duplicate pending requests
     *     produce 409.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param groupId study group UUID from the path
     * @param request JSON body described above
     * @return response described above, with the documented HTTP status and payload
     */
    @PostMapping("/{groupId}/join-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public StudyGroupResponse requestToJoin(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @Valid @RequestBody JoinGroupRequest request) {
        return membershipService.requestToJoin(caller.id(), groupId, request.trimmedMessage());
    }

    /**
     * Lists pending applicants, oldest first: {@code GET /api/groups/{groupId} /join-requests},
     *     members only.
     * Returns HTTP 200; a non-member is 403. Only the leader may accept or reject a request.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param groupId study group UUID from the path
     * @return response described above, with the documented HTTP status and payload
     */
    @GetMapping("/{groupId}/join-requests")
    public List<JoinRequestResponse> listJoinRequests(@CurrentUser AuthenticatedUser caller,
            @PathVariable UUID groupId) {
        return membershipService.listPendingRequests(caller.id(), groupId);
    }

    /**
     * Accepts a pending applicant: {@code POST /api/groups/{groupId}
     *     /join-requests/{requestId}/accept}, leader only.
     * Returns HTTP 200 with group details; a non-leader is 403, missing/non-pending requests are
     *     404,
     * and a closed or full group is 409.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param groupId study group UUID from the path
     * @param requestId pending request UUID from the path
     * @return response described above, with the documented HTTP status and payload
     */
    @PostMapping("/{groupId}/join-requests/{requestId}/accept")
    public StudyGroupResponse acceptJoinRequest(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @PathVariable UUID requestId) {
        return membershipService.acceptRequest(caller.id(), groupId, requestId);
    }

    /**
     * Rejects a pending applicant: {@code POST /api/groups/{groupId}
     *     /join-requests/{requestId}/reject}, leader only.
     * Returns HTTP 200 with group details; a non-leader is 403, missing/non-pending requests are
     *     404, and a closed group is 409.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param groupId study group UUID from the path
     * @param requestId pending request UUID from the path
     * @return response described above, with the documented HTTP status and payload
     */
    @PostMapping("/{groupId}/join-requests/{requestId}/reject")
    public StudyGroupResponse rejectJoinRequest(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @PathVariable UUID requestId) {
        return membershipService.rejectRequest(caller.id(), groupId, requestId);
    }

    /**
     * Leaves an open group: {@code POST /api/groups/{groupId}/leave}, non-leader member only.
     * Returns HTTP 204. Closed groups, leaders and non-members produce 409.
     * A leader must transfer leadership before leaving; closing the group prevents leaving.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param groupId study group UUID from the path
     * @return response described above, with the documented HTTP status and payload
     */
    @PostMapping("/{groupId}/leave")
    public ResponseEntity<Void> leave(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId) {
        membershipService.leaveGroup(caller.id(), groupId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Saves a manual chat URL: {@code PUT /api/groups/{groupId}/chat-link}, leader only.
     * Body {@code {"url":"https://t.me/example"}}. This does not set up a bot-managed chat.
     * Returns HTTP 200 with group details; invalid links are 400, a non-leader is 403 and a closed
     *     group is 409.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param groupId study group UUID from the path
     * @param request JSON body described above
     * @return response described above, with the documented HTTP status and payload
     */
    @PutMapping("/{groupId}/chat-link")
    public StudyGroupResponse setChatLink(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @RequestBody ChatLinkRequest request) {
        return groupService.setChatLink(caller.id(), groupId, request.url());
    }

    /**
     * Clears the manual URL: {@code DELETE /api/groups/{groupId}/chat-link}, leader only.
     * Returns HTTP 204; a non-leader is 403 and a closed group is 409.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param groupId study group UUID from the path
     * @return response described above, with the documented HTTP status and payload
     */
    @DeleteMapping("/{groupId}/chat-link")
    public ResponseEntity<Void> clearChatLink(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId) {
        groupService.clearChatLink(caller.id(), groupId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Removes a member: {@code DELETE /api/groups/{groupId}/members/{studentId}}, leader only.
     * Returns HTTP 204; a non-leader is 403, while a closed group, removal of the leader or a
     *     non-member is 409.
     *
     * @param caller authenticated caller injected from the bearer token
     * @param groupId study group UUID from the path
     * @param studentId student UUID from the path
     * @return response described above, with the documented HTTP status and payload
     */
    @DeleteMapping("/{groupId}/members/{studentId}")
    public ResponseEntity<Void> removeMember(@CurrentUser AuthenticatedUser caller, @PathVariable UUID groupId,
            @PathVariable UUID studentId) {
        membershipService.removeMember(caller.id(), groupId, studentId);
        return ResponseEntity.noContent().build();
    }
}
