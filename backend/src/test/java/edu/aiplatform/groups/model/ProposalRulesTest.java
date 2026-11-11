package edu.aiplatform.groups.model;

import static org.assertj.core.api.Assertions.assertThat;

import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.groups.model.GroupFormationSettings.ClosedReason;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProposalRulesTest {

    private static final Instant NOW = Instant.parse("2026-11-11T08:00:00Z");
    private final UUID classId = UUID.randomUUID();

    @Test
    void nameIsTrimmedAndBoundedToHundredCharacters() {
        assertThat(ProposalRules.cleanName("  Nhóm Mây  ")).isEqualTo("Nhóm Mây");
        assertThat(ProposalRules.cleanName("   ")).isNull();
        assertThat(ProposalRules.cleanName(null)).isNull();
        assertThat(ProposalRules.cleanName("a".repeat(100))).hasSize(100);
        assertThat(ProposalRules.cleanName("a".repeat(101))).isNull();
        assertThat(ProposalRules.nameKey(" Nhóm A ")).isEqualTo(ProposalRules.nameKey("nhóm a"));
    }

    @Test
    void blankReasonBecomesNull() {
        assertThat(ProposalRules.cleanReason("  ")).isNull();
        assertThat(ProposalRules.cleanReason(" Thiếu người ")).isEqualTo("Thiếu người");
    }

    @Test
    void freeSeatsCountMembersAndPendingInvitations() {
        assertThat(ProposalRules.countFreeSeats(5, 2, 1)).isEqualTo(2);
        assertThat(ProposalRules.countFreeSeats(3, 2, 2)).isZero();
    }

    @Test
    void onlyActiveUngroupedAcceptedMembersAreEffective() {
        UUID kept = UUID.randomUUID();
        UUID leftClass = UUID.randomUUID();
        UUID grouped = UUID.randomUUID();
        UUID invited = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();
        List<ProposalMember> members = List.of(
                member(proposalId, kept, ProposalMember.Status.ACCEPTED, true),
                member(proposalId, leftClass, ProposalMember.Status.ACCEPTED, true),
                member(proposalId, grouped, ProposalMember.Status.ACCEPTED, true),
                member(proposalId, invited, ProposalMember.Status.INVITED, false));

        long effective = ProposalRules.countEffectiveMembers(members, Set.of(kept, grouped, invited), Set.of(grouped));

        assertThat(effective).isEqualTo(1);
    }

    @Test
    void windowIsOpenOnlyWhenEnabledClassOpenAndBeforeDeadline() {
        GroupFormationSettings open = new GroupFormationSettings(classId, true, 2, 4, NOW.plusSeconds(60), null, NOW, 1);

        assertThat(open.isOpen(ClassStatus.OPEN, NOW)).isTrue();
        assertThat(open.closedReason(ClassStatus.DRAFT, NOW)).isEqualTo(ClosedReason.CLASS_NOT_OPEN);
        assertThat(open.closedReason(ClassStatus.OPEN, NOW.plusSeconds(60))).isEqualTo(ClosedReason.DEADLINE_PASSED);
        assertThat(GroupFormationSettings.defaults(classId).closedReason(ClassStatus.OPEN, NOW))
                .isEqualTo(ClosedReason.DISABLED);
        assertThat(new GroupFormationSettings(classId, true, 2, 4, null, null, NOW, 1).isOpen(ClassStatus.OPEN, NOW))
                .as("không có hạn chót").isTrue();
    }

    @Test
    void sizeMustBeBetweenTwoAndTwenty() {
        assertThat(GroupFormationSettings.isValidSize(1)).isFalse();
        assertThat(GroupFormationSettings.isValidSize(2)).isTrue();
        assertThat(GroupFormationSettings.isValidSize(20)).isTrue();
        assertThat(GroupFormationSettings.isValidSize(21)).isFalse();
    }

    @Test
    void stateTransitionsBumpVersionAndClearDecision() {
        GroupProposal draft = GroupProposal.draft(UUID.randomUUID(), classId, "Nhóm A", UUID.randomUUID(), NOW);
        GroupProposal submitted = draft.submitted(NOW);
        GroupProposal back = submitted.backToDraft(NOW);

        assertThat(submitted.status()).isEqualTo(GroupProposal.Status.SUBMITTED);
        assertThat(submitted.submittedAt()).isEqualTo(NOW);
        assertThat(back.status()).isEqualTo(GroupProposal.Status.DRAFT);
        assertThat(back.submittedAt()).isNull();
        assertThat(back.version()).isEqualTo(2);
        assertThat(draft.cancelled(GroupProposal.EndReason.LEADER_CANCELLED, NOW).status().isActive()).isFalse();
    }

    private ProposalMember member(UUID proposalId, UUID accountId, ProposalMember.Status status, boolean active) {
        return new ProposalMember(proposalId, classId, accountId, status, active, null, NOW, null);
    }
}
