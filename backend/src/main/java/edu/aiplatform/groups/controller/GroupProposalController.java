package edu.aiplatform.groups.controller;

import edu.aiplatform.groups.service.GroupFormationService;
import edu.aiplatform.groups.service.GroupFormationService.FormationView;
import edu.aiplatform.groups.service.GroupFormationService.UpdateFormationCommand;
import edu.aiplatform.groups.service.GroupProposalReviewService;
import edu.aiplatform.groups.service.GroupProposalReviewService.ApproveCommand;
import edu.aiplatform.groups.service.GroupProposalReviewService.ApproveResult;
import edu.aiplatform.groups.service.GroupProposalReviewService.ClassProposalsView;
import edu.aiplatform.groups.service.GroupProposalReviewService.RejectProposalCommand;
import edu.aiplatform.groups.service.GroupProposalService;
import edu.aiplatform.groups.service.GroupProposalService.CreateProposalCommand;
import edu.aiplatform.groups.service.GroupProposalService.InviteMembersCommand;
import edu.aiplatform.groups.service.GroupProposalService.MyProposalView;
import edu.aiplatform.groups.service.GroupProposalService.RenameProposalCommand;
import edu.aiplatform.groups.service.GroupProposalService.VersionCommand;
import edu.aiplatform.identity.contract.Actor;
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

/**
 * US-GRP-007…010: đợt tự chia nhóm, đề xuất nhóm của sinh viên, duyệt và từ chối của giảng viên. Kiểm quyền, trạng
 * thái và giá trị đều ở service (BR-U12-91, 93).
 */
@RestController
public class GroupProposalController {

    private final GroupFormationService groupFormationService;
    private final GroupProposalService groupProposalService;
    private final GroupProposalReviewService groupProposalReviewService;

    public GroupProposalController(GroupFormationService groupFormationService,
                                   GroupProposalService groupProposalService,
                                   GroupProposalReviewService groupProposalReviewService) {
        this.groupFormationService = groupFormationService;
        this.groupProposalService = groupProposalService;
        this.groupProposalReviewService = groupProposalReviewService;
    }

    // ---- giảng viên ------------------------------------------------------------------------------------------------

    @GetMapping("/api/v1/classes/{classId}/group-formation")
    public FormationView loadFormation(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId) {
        return groupFormationService.loadFormation(actor, classId);
    }

    @PutMapping("/api/v1/classes/{classId}/group-formation")
    public FormationView updateFormation(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId,
                                         @RequestBody UpdateFormationCommand command) {
        return groupFormationService.updateFormation(actor, classId, command);
    }

    @GetMapping("/api/v1/classes/{classId}/group-proposals")
    public ClassProposalsView listClassProposals(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId) {
        return groupProposalReviewService.listClassProposals(actor, classId);
    }

    @PostMapping("/api/v1/classes/{classId}/group-proposals:approve")
    public ApproveResult approveProposals(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId,
                                          @RequestBody ApproveCommand command) {
        return groupProposalReviewService.approveProposals(actor, classId, command);
    }

    @PostMapping("/api/v1/group-proposals/{proposalId}:reject")
    public ClassProposalsView rejectProposal(@AuthenticationPrincipal Actor actor, @PathVariable UUID proposalId,
                                             @RequestBody(required = false) RejectProposalCommand command) {
        return groupProposalReviewService.rejectProposal(actor, proposalId, command);
    }

    // ---- sinh viên -------------------------------------------------------------------------------------------------

    @GetMapping("/api/v1/me/classes/{classId}/group-proposal")
    public MyProposalView loadMyProposal(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId) {
        return groupProposalService.loadMyProposal(actor, classId);
    }

    @PostMapping("/api/v1/classes/{classId}/group-proposals")
    @ResponseStatus(HttpStatus.CREATED)
    public MyProposalView createProposal(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId,
                                         @RequestBody CreateProposalCommand command) {
        return groupProposalService.createProposal(actor, classId, command);
    }

    @PutMapping("/api/v1/group-proposals/{proposalId}/name")
    public MyProposalView renameProposal(@AuthenticationPrincipal Actor actor, @PathVariable UUID proposalId,
                                         @RequestBody RenameProposalCommand command) {
        return groupProposalService.renameProposal(actor, proposalId, command);
    }

    @PostMapping("/api/v1/group-proposals/{proposalId}/invitations")
    public MyProposalView inviteMembers(@AuthenticationPrincipal Actor actor, @PathVariable UUID proposalId,
                                        @RequestBody InviteMembersCommand command) {
        return groupProposalService.inviteMembers(actor, proposalId, command);
    }

    @PostMapping("/api/v1/group-proposals/{proposalId}/invitations/{accountId}:revoke")
    public MyProposalView revokeInvitation(@AuthenticationPrincipal Actor actor, @PathVariable UUID proposalId,
                                           @PathVariable UUID accountId, @RequestBody VersionCommand command) {
        return groupProposalService.revokeInvitation(actor, proposalId, accountId, command);
    }

    @PostMapping("/api/v1/group-proposals/{proposalId}:accept")
    public MyProposalView acceptInvitation(@AuthenticationPrincipal Actor actor, @PathVariable UUID proposalId) {
        return groupProposalService.acceptInvitation(actor, proposalId);
    }

    @PostMapping("/api/v1/group-proposals/{proposalId}:decline")
    public MyProposalView declineInvitation(@AuthenticationPrincipal Actor actor, @PathVariable UUID proposalId) {
        return groupProposalService.declineInvitation(actor, proposalId);
    }

    @PostMapping("/api/v1/group-proposals/{proposalId}:leave")
    public MyProposalView leaveProposal(@AuthenticationPrincipal Actor actor, @PathVariable UUID proposalId) {
        return groupProposalService.leaveProposal(actor, proposalId);
    }

    @PostMapping("/api/v1/group-proposals/{proposalId}:submit")
    public MyProposalView submitProposal(@AuthenticationPrincipal Actor actor, @PathVariable UUID proposalId,
                                         @RequestBody VersionCommand command) {
        return groupProposalService.submitProposal(actor, proposalId, command);
    }

    @PostMapping("/api/v1/group-proposals/{proposalId}:withdraw")
    public MyProposalView withdrawProposal(@AuthenticationPrincipal Actor actor, @PathVariable UUID proposalId,
                                           @RequestBody VersionCommand command) {
        return groupProposalService.withdrawProposal(actor, proposalId, command);
    }

    @PostMapping("/api/v1/group-proposals/{proposalId}:cancel")
    public MyProposalView cancelProposal(@AuthenticationPrincipal Actor actor, @PathVariable UUID proposalId,
                                         @RequestBody VersionCommand command) {
        return groupProposalService.cancelProposal(actor, proposalId, command);
    }
}
