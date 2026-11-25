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
        assertThat(open.resolveClosedReason(ClassStatus.DRAFT, NOW)).isEqualTo(ClosedReason.CLASS_NOT_OPEN);
        assertThat(open.resolveClosedReason(ClassStatus.OPEN, NOW.plusSeconds(60))).isEqualTo(ClosedReason.DEADLINE_PASSED);
        assertThat(GroupFormationSettings.createDefaults(classId).resolveClosedReason(ClassStatus.OPEN, NOW))
                .isEqualTo(ClosedReason.DISABLED);
        assertThat(new GroupFormationSettings(classId, true, 2, 4, null, null, NOW, 1).isOpen(ClassStatus.OPEN, NOW))
                .as("không có hạn chót").isTrue();
    }

    private ProposalMember member(UUID proposalId, UUID accountId, ProposalMember.Status status, boolean active) {
        return new ProposalMember(proposalId, classId, accountId, status, active, null, NOW, null);
    }
}
