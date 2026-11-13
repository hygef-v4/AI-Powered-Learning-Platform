package edu.aiplatform.groups.controller;

import edu.aiplatform.groupdocs.service.GroupWorkQueryService;
import edu.aiplatform.groupdocs.service.GroupWorkQueryService.GroupAssignment;
import edu.aiplatform.groupdocs.service.GroupWorkQueryService.GroupProgress;
import edu.aiplatform.groups.service.ClassGroupService;
import edu.aiplatform.groups.service.ClassGroupService.DraftView;
import edu.aiplatform.groups.service.ClassGroupService.GroupSetView;
import edu.aiplatform.groups.service.ClassGroupService.RequestView;
import edu.aiplatform.groups.service.ClassGroupService.SaveCommand;
import edu.aiplatform.groups.service.GroupRenameService;
import edu.aiplatform.groups.service.GroupRenameService.Renamed;
import edu.aiplatform.groups.service.LeaderRequestService;
import edu.aiplatform.groups.service.LeaderRequestService.RequestResult;
import edu.aiplatform.groups.service.MyGroupService;
import edu.aiplatform.groups.service.MyGroupService.MyGroupView;
import edu.aiplatform.groups.service.MyGroupService.StudentGroup;
import edu.aiplatform.identity.contract.Actor;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** UC 54, 55, 57: nhóm của lớp, chia ngẫu nhiên, yêu cầu đổi trưởng nhóm, nhóm của tôi, tiến độ bài nhóm. */
@RestController
public class GroupController {

    private final ClassGroupService classGroups;
    private final LeaderRequestService requests;
    private final MyGroupService myGroups;
    private final GroupWorkQueryService work;
    private final GroupRenameService renames;

    public GroupController(ClassGroupService classGroups, LeaderRequestService requests, MyGroupService myGroups,
                           GroupWorkQueryService work, GroupRenameService renames) {
        this.classGroups = classGroups;
        this.requests = requests;
        this.myGroups = myGroups;
        this.work = work;
        this.renames = renames;
    }

    public record RandomRequest(Integer maxSize) {
    }

    public record ApproveRequest(UUID newLeaderId) {
    }

    public record RejectRequest(String reason) {
    }

    public record RenameRequest(String name) {
    }

    public record SubmitLeaderRequest(String reason, UUID proposedLeaderId) {
    }

    @GetMapping("/api/v1/classes/{classId}/groups")
    public GroupSetView groups(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId) {
        return classGroups.view(actor, classId);
    }

    @PutMapping("/api/v1/classes/{classId}/groups")
    public GroupSetView save(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId,
                             @RequestBody SaveCommand command) {
        return classGroups.save(actor, classId, command);
    }

    @PostMapping("/api/v1/classes/{classId}/groups:random-preview")
    public List<DraftView> randomPreview(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId,
                                         @RequestBody RandomRequest request) {
        return classGroups.randomPreview(actor, classId, request.maxSize());
    }

    @GetMapping("/api/v1/classes/{classId}/leader-requests")
    public List<RequestView> pendingRequests(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId) {
        return classGroups.view(actor, classId).pendingRequests();
    }

    @PostMapping("/api/v1/leader-requests/{id}:approve")
    public RequestResult approve(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                 @RequestBody(required = false) ApproveRequest request) {
        return requests.approve(actor, id, request == null ? null : request.newLeaderId());
    }

    @PostMapping("/api/v1/leader-requests/{id}:reject")
    public RequestResult reject(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                @RequestBody RejectRequest request) {
        return requests.reject(actor, id, request.reason());
    }

    @GetMapping("/api/v1/me/classes/{classId}/group")
    public MyGroupView myGroup(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId) {
        return myGroups.myGroup(actor, classId);
    }

    /** Trưởng nhóm đổi tên nhóm của mình, không cần duyệt. */
    @PutMapping("/api/v1/groups/{groupId}/name")
    public Renamed rename(@AuthenticationPrincipal Actor actor, @PathVariable UUID groupId, @RequestBody RenameRequest request) {
        return renames.rename(actor, groupId, request.name());
    }

    @PostMapping("/api/v1/groups/{groupId}/leader-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestResult submitRequest(@AuthenticationPrincipal Actor actor, @PathVariable UUID groupId,
                                       @RequestBody SubmitLeaderRequest request) {
        return requests.submit(actor, groupId, request.reason(), request.proposedLeaderId());
    }

    @PostMapping("/api/v1/leader-requests/{id}:cancel")
    public RequestResult cancel(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return requests.cancel(actor, id);
    }

    @GetMapping("/api/v1/classes/{classId}/students/{studentId}/group")
    public StudentGroup studentGroup(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId,
                                     @PathVariable UUID studentId) {
        return myGroups.studentGroup(actor, classId, studentId);
    }

    @GetMapping("/api/v1/classes/{classId}/group-assignments")
    public List<GroupAssignment> groupAssignments(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId) {
        return work.groupAssignments(actor, classId);
    }

    @GetMapping("/api/v1/classes/{classId}/group-assignments/{assignmentId}/progress")
    public List<GroupProgress> progress(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId,
                                        @PathVariable UUID assignmentId) {
        return work.progress(actor, classId, assignmentId);
    }
}
