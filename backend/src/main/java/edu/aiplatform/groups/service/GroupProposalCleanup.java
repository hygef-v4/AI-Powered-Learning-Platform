package edu.aiplatform.groups.service;

import edu.aiplatform.groups.model.GroupFormationSettings;
import edu.aiplatform.groups.model.GroupProposal;
import edu.aiplatform.groups.model.GroupProposal.EndReason;
import edu.aiplatform.groups.model.GroupProposal.Status;
import edu.aiplatform.groups.model.ProposalMember;
import edu.aiplatform.groups.repository.GroupProposalRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Giữ đề xuất khớp với bộ nhóm chính thức (F23, BR-U12-88): sinh viên vừa có nhóm rời đề xuất, lời mời của họ bị thu
 * hồi; đề xuất mất trưởng nhóm bị hủy, đề xuất chờ duyệt dưới sĩ số tối thiểu về nháp. Chạy trong transaction của
 * người gọi và không phát thông báo (BR-U12-95).
 */
@Component
public class GroupProposalCleanup {

    private final GroupProposalRepository proposalRepository;

    public GroupProposalCleanup(GroupProposalRepository proposalRepository) {
        this.proposalRepository = proposalRepository;
    }

    public void releasePlacedStudents(UUID classId, Collection<UUID> placedStudents, Instant now) {
        if (placedStudents.isEmpty()) {
            return;
        }
        List<ProposalMember> memberships = proposalRepository.listActiveMemberships(classId, placedStudents);
        Set<UUID> touched = new LinkedHashSet<>();
        memberships.stream().map(ProposalMember::proposalId).distinct().sorted().forEach(proposalId ->
                proposalRepository.lockProposal(proposalId).ifPresent(proposal -> {
                    if (placedStudents.contains(proposal.leaderId())) {
                        cancelProposal(proposal, EndReason.LEADER_PLACED, now);
                    } else {
                        touched.add(proposalId);
                    }
                }));
        for (ProposalMember membership : memberships) {
            if (touched.contains(membership.proposalId())) {
                proposalRepository.updateMember(membership.proposalId(), membership.accountId(),
                        ProposalMember.Status.LEFT, false, now);
            }
        }
        proposalRepository.revokeInvitationsOf(classId, placedStudents, null, now);
        int minSize = proposalRepository.findSettings(classId).map(GroupFormationSettings::minSize)
                .orElse(GroupFormationSettings.DEFAULT_MIN_SIZE);
        for (UUID proposalId : touched) {
            proposalRepository.lockProposal(proposalId).ifPresent(proposal -> {
                long members = proposalRepository.listMembers(proposalId).stream()
                        .filter(ProposalMember::isActiveMember).count();
                GroupProposal next = proposal.status() == Status.SUBMITTED && members < minSize
                        ? proposal.backToDraft(now) : proposal.touched(now);
                proposalRepository.updateProposal(next);
            });
        }
    }

    /** Hủy đề xuất: thành viên thôi hoạt động, lời mời chờ bị thu hồi. */
    void cancelProposal(GroupProposal proposal, EndReason reason, Instant now) {
        proposalRepository.updateProposal(proposal.cancelled(reason, now));
        proposalRepository.deactivateMembers(proposal.id());
        proposalRepository.closeInvitations(proposal.id(), ProposalMember.Status.REVOKED, now);
    }
}
