package edu.aiplatform.grading.repository;

import edu.aiplatform.grading.model.GradingProposal;
import edu.aiplatform.grading.repository.IGradingProposalRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class GradingProposalRepositoryAdapter implements IGradingProposalRepository {
    private final IGradingProposalJpaRepository gradingProposalJpaRepository;

    public GradingProposalRepositoryAdapter(IGradingProposalJpaRepository gradingProposalJpaRepository) {
        this.gradingProposalJpaRepository = gradingProposalJpaRepository;
    }

    public Optional<UUID> findEvaluationId(UUID id) {
        return gradingProposalJpaRepository.findEvaluationIdById(id);
    }

    public Optional<GradingProposal> findById(UUID id) {
        return gradingProposalJpaRepository.findById(id).map(GradingProposalJpaEntity::mapProposal);
    }

    public Optional<GradingProposal> findByRequestRef(String ref) {
        return gradingProposalJpaRepository.findByRequestRef(ref).map(GradingProposalJpaEntity::mapProposal);
    }

    public List<GradingProposal> findForEvaluation(UUID id) {
        return gradingProposalJpaRepository.findByEvaluationIdOrderByVersionNoDesc(id).stream()
                .map(GradingProposalJpaEntity::mapProposal)
                .toList();
    }

    public GradingProposal save(GradingProposal p) {
        var e = gradingProposalJpaRepository.findById(p.id).orElseGet(GradingProposalJpaEntity::new);
        e.applyProposal(p);
        return gradingProposalJpaRepository.saveAndFlush(e).mapProposal();
    }
}
