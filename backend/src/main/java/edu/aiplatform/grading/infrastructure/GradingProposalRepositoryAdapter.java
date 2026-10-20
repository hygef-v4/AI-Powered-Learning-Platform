package edu.aiplatform.grading.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.grading.domain.GradingProposal;
import edu.aiplatform.grading.domain.IGradingProposalRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class GradingProposalRepositoryAdapter implements IGradingProposalRepository {
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();

    private static GradingProposal copy(GradingProposal p) {
        return JSON.convertValue(p, GradingProposal.class);
    }

    private final IGradingProposalJpaRepository repo;

    public GradingProposalRepositoryAdapter(IGradingProposalJpaRepository repo) {
        this.repo = repo;
    }

    public Optional<UUID> findEvaluationId(UUID id) {
        return repo.findEvaluationIdById(id);
    }

    public Optional<GradingProposal> findById(UUID id) {
        return repo.findById(id).map(e -> copy(e.document));
    }

    public Optional<GradingProposal> findByRequestRef(String ref) {
        return repo.findByRequestRef(ref).map(e -> copy(e.document));
    }

    public List<GradingProposal> findForEvaluation(UUID id) {
        return repo.findByEvaluationIdOrderByVersionNoDesc(id).stream().map(e -> copy(e.document)).toList();
    }

    public GradingProposal save(GradingProposal p) {
        var e = repo.findById(p.id).orElseGet(GradingProposalJpaEntity::new);
        e.id = p.id;
        e.evaluationId = p.evaluationId;
        e.versionNo = p.versionNo;
        e.requestRef = p.requestRef;
        e.status = p.status;
        e.document = copy(p);
        return copy(repo.saveAndFlush(e).document);
    }
}
