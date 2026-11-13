package edu.aiplatform.groups.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import edu.aiplatform.groups.model.GroupFormationSettings;
import edu.aiplatform.groups.model.GroupProposal;
import edu.aiplatform.groups.model.ProposalMember;
import edu.aiplatform.groups.repository.GroupProposalRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GroupProposalCleanupTest {

    private static final Instant NOW = Instant.parse("2026-11-11T08:00:00Z");
    private final UUID classId = UUID.randomUUID();
    private final GroupProposalRepository proposals = mock(GroupProposalRepository.class);
    private final GroupProposalCleanup cleanup = new GroupProposalCleanup(proposals);

    @Test
    void placedLeaderCancelsTheProposal() {
        UUID leader = UUID.randomUUID();
        GroupProposal proposal = GroupProposal.draft(UUID.randomUUID(), classId, "A", leader, NOW);
        when(proposals.listActiveMemberships(classId, List.of(leader))).thenReturn(List.of(row(proposal, leader, true)));
        when(proposals.lockProposal(proposal.id())).thenReturn(Optional.of(proposal));

        cleanup.releasePlacedStudents(classId, List.of(leader), NOW);

        ArgumentCaptor<GroupProposal> updated = ArgumentCaptor.forClass(GroupProposal.class);
        verify(proposals).updateProposal(updated.capture());
        assertThat(updated.getValue().status()).isEqualTo(GroupProposal.Status.CANCELLED);
        assertThat(updated.getValue().endReason()).isEqualTo(GroupProposal.EndReason.LEADER_PLACED);
        verify(proposals).deactivateMembers(proposal.id());
        verify(proposals).revokeInvitationsOf(classId, List.of(leader), null, NOW);
    }

    @Test
    void placedMemberLeavesAndSubmittedProposalBelowMinimumReturnsToDraft() {
        UUID leader = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        GroupProposal proposal = GroupProposal.draft(UUID.randomUUID(), classId, "A", leader, NOW).submitted(NOW);
        when(proposals.listActiveMemberships(classId, List.of(member))).thenReturn(List.of(row(proposal, member, true)));
        when(proposals.lockProposal(proposal.id())).thenReturn(Optional.of(proposal));
        when(proposals.listMembers(proposal.id())).thenReturn(List.of(row(proposal, leader, true),
                new ProposalMember(proposal.id(), classId, member, ProposalMember.Status.LEFT, false, null, NOW, NOW)));
        when(proposals.findSettings(classId)).thenReturn(Optional.of(
                new GroupFormationSettings(classId, true, 2, 4, null, null, NOW, 1)));

        cleanup.releasePlacedStudents(classId, List.of(member), NOW);

        verify(proposals).updateMember(proposal.id(), member, ProposalMember.Status.LEFT, false, NOW);
        ArgumentCaptor<GroupProposal> updated = ArgumentCaptor.forClass(GroupProposal.class);
        verify(proposals).updateProposal(updated.capture());
        assertThat(updated.getValue().status()).isEqualTo(GroupProposal.Status.DRAFT);
        verify(proposals, never()).deactivateMembers(any());
    }

    @Test
    void nothingPlacedTouchesNothing() {
        cleanup.releasePlacedStudents(classId, List.of(), NOW);

        verifyNoInteractions(proposals);
    }

    private ProposalMember row(GroupProposal proposal, UUID accountId, boolean active) {
        return new ProposalMember(proposal.id(), classId, accountId, ProposalMember.Status.ACCEPTED, active, null, NOW,
                NOW);
    }
}
