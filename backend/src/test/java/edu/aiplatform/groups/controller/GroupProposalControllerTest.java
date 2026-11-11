package edu.aiplatform.groups.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.aiplatform.groups.service.GroupFormationService;
import edu.aiplatform.groups.service.GroupFormationService.FormationView;
import edu.aiplatform.groups.service.GroupFormationService.UpdateFormationCommand;
import edu.aiplatform.groups.service.GroupProposalReviewService;
import edu.aiplatform.groups.service.GroupProposalReviewService.ApproveCommand;
import edu.aiplatform.groups.service.GroupProposalReviewService.ApproveResult;
import edu.aiplatform.groups.service.GroupProposalReviewService.Failed;
import edu.aiplatform.groups.service.GroupProposalService;
import edu.aiplatform.groups.service.GroupProposalService.InviteMembersCommand;
import edu.aiplatform.groups.service.GroupProposalService.VersionCommand;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.shared.error.GlobalExceptionHandler;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Mapping, tuần tự hóa và mã lỗi của API đề xuất nhóm; quyền thật nằm ở service. */
class GroupProposalControllerTest {

    private final Actor actor = new Actor(UUID.randomUUID(), Role.STUDENT);
    private final GroupFormationService formations = mock(GroupFormationService.class);
    private final GroupProposalService proposals = mock(GroupProposalService.class);
    private final GroupProposalReviewService reviews = mock(GroupProposalReviewService.class);
    private final UUID classId = UUID.randomUUID();
    private final UUID proposalId = UUID.randomUUID();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, null));
        mvc = MockMvcBuilders.standaloneSetup(new GroupProposalController(formations, proposals, reviews))
                .setControllerAdvice(new GroupExceptionHandler(), new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void formationSettingsAreReadAndSavedWithVersion() throws Exception {
        Instant deadline = Instant.parse("2026-11-20T10:00:00Z");
        when(formations.updateFormation(eq(actor), eq(classId), any())).thenReturn(
                new FormationView(classId, true, 2, 4, deadline, 1, true, null));

        mvc.perform(put("/api/v1/classes/" + classId + "/group-formation").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true,\"minSize\":2,\"maxSize\":4,"
                                + "\"deadline\":\"2026-11-20T10:00:00Z\",\"version\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.open").value(true))
                .andExpect(jsonPath("$.version").value(1));
        verify(formations).updateFormation(actor, classId, new UpdateFormationCommand(true, 2, 4, deadline, 0L));

        mvc.perform(get("/api/v1/classes/" + classId + "/group-formation")).andExpect(status().isOk());
        verify(formations).loadFormation(actor, classId);
    }

    @Test
    void studentActionsAreMapped() throws Exception {
        UUID invitee = UUID.randomUUID();
        mvc.perform(get("/api/v1/me/classes/" + classId + "/group-proposal")).andExpect(status().isOk());
        verify(proposals).loadMyProposal(actor, classId);

        mvc.perform(post("/api/v1/classes/" + classId + "/group-proposals").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Nhóm Mây\"}")).andExpect(status().isCreated());
        mvc.perform(post("/api/v1/group-proposals/" + proposalId + "/invitations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountIds\":[\"" + invitee + "\"],\"version\":2}")).andExpect(status().isOk());
        verify(proposals).inviteMembers(actor, proposalId, new InviteMembersCommand(List.of(invitee), 2L));
        mvc.perform(post("/api/v1/group-proposals/" + proposalId + "/invitations/" + invitee + ":revoke")
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":3}")).andExpect(status().isOk());
        verify(proposals).revokeInvitation(actor, proposalId, invitee, new VersionCommand(3L));
        for (String action : List.of(":accept", ":decline", ":leave")) {
            mvc.perform(post("/api/v1/group-proposals/" + proposalId + action)).andExpect(status().isOk());
        }
        verify(proposals).acceptInvitation(actor, proposalId);
        verify(proposals).declineInvitation(actor, proposalId);
        verify(proposals).leaveProposal(actor, proposalId);
        for (String action : List.of(":submit", ":withdraw", ":cancel")) {
            mvc.perform(post("/api/v1/group-proposals/" + proposalId + action).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"version\":4}")).andExpect(status().isOk());
        }
        verify(proposals).submitProposal(actor, proposalId, new VersionCommand(4L));
        verify(proposals).withdrawProposal(actor, proposalId, new VersionCommand(4L));
        verify(proposals).cancelProposal(actor, proposalId, new VersionCommand(4L));
    }

    @Test
    void approveReturnsPerProposalResultsAndRejectAcceptsAnEmptyBody() throws Exception {
        when(reviews.approveProposals(eq(actor), eq(classId), any())).thenReturn(new ApproveResult(List.of(),
                List.of(new Failed(proposalId, "GROUP_NAME_DUPLICATE", "Tên nhóm bị trùng")), 5));

        mvc.perform(post("/api/v1/classes/" + classId + "/group-proposals:approve")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"proposalIds\":[\"" + proposalId + "\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.failed[0].code").value("GROUP_NAME_DUPLICATE"))
                .andExpect(jsonPath("$.groupSetVersion").value(5));
        verify(reviews).approveProposals(actor, classId, new ApproveCommand(List.of(proposalId)));

        mvc.perform(post("/api/v1/group-proposals/" + proposalId + ":reject")).andExpect(status().isOk());
        verify(reviews).rejectProposal(actor, proposalId, null);
        mvc.perform(get("/api/v1/classes/" + classId + "/group-proposals")).andExpect(status().isOk());
        verify(reviews).listClassProposals(actor, classId);
    }

    @Test
    void serviceErrorsAndUnreadableBodiesKeepTheirCodes() throws Exception {
        when(proposals.acceptInvitation(actor, proposalId))
                .thenThrow(ApiException.conflict("STUDENT_IN_PROPOSAL", "Bạn đang ở một đề xuất khác"));
        mvc.perform(post("/api/v1/group-proposals/" + proposalId + ":accept"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("STUDENT_IN_PROPOSAL"));

        when(proposals.leaveProposal(actor, proposalId)).thenThrow(ApiException.notFound("GROUP_PROPOSAL_NOT_FOUND"));
        mvc.perform(post("/api/v1/group-proposals/" + proposalId + ":leave")).andExpect(status().isNotFound());

        mvc.perform(post("/api/v1/group-proposals/not-a-uuid:accept")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/classes/" + classId + "/group-formation").contentType(MediaType.APPLICATION_JSON)
                .content("{\"minSize\":\"hai\"}")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
}
