package edu.aiplatform.grading.repository;

import edu.aiplatform.grading.model.GradingProposal;
import edu.aiplatform.grading.repository.IGradingProposalRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class GradingProposalRepositoryAdapter implements IGradingProposalRepository {
    private final IGradingProposalJpaRepository repo;

    public GradingProposalRepositoryAdapter(IGradingProposalJpaRepository repo) {
        this.repo = repo;
    }

    public Optional<UUID> findEvaluationId(UUID id) {
        return repo.findEvaluationIdById(id);
    }

    public Optional<GradingProposal> findById(UUID id) {
        return repo.findById(id).map(GradingProposalJpaEntity::view);
    }

    public Optional<GradingProposal> findByRequestRef(String ref) {
        return repo.findByRequestRef(ref).map(GradingProposalJpaEntity::view);
    }

    public List<GradingProposal> findForEvaluation(UUID id) {
        return repo.findByEvaluationIdOrderByVersionNoDesc(id).stream()
                .map(GradingProposalJpaEntity::view)
                .toList();
    }

    public GradingProposal save(GradingProposal p) {
        var e = repo.findById(p.id).orElseGet(GradingProposalJpaEntity::new);
        e.apply(p);
        return repo.saveAndFlush(e).view();
    }
}
