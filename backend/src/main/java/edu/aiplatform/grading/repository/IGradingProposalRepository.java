package edu.aiplatform.grading.repository;

import edu.aiplatform.grading.model.GradingProposal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IGradingProposalRepository {
    Optional<GradingProposal> findById(UUID id);

    Optional<UUID> findEvaluationId(UUID proposalId);

    Optional<GradingProposal> findByRequestRef(String ref);

    List<GradingProposal> findForEvaluation(UUID evaluationId);

    GradingProposal save(GradingProposal proposal);
}
