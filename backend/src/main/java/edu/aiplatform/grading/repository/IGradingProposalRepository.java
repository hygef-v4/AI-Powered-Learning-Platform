package edu.aiplatform.grading.repository;

import edu.aiplatform.grading.model.GradingProposal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IGradingProposalRepository {
    /** Finds a grading proposal by its identifier. */
    Optional<GradingProposal> findById(UUID id);

    /** Finds the evaluation identifier linked to a proposal. */
    Optional<UUID> findEvaluationId(UUID proposalId);

    /** Finds a proposal by its request reference. */
    Optional<GradingProposal> findByRequestRef(String ref);

    /** Lists proposals for an evaluation. */
    List<GradingProposal> findForEvaluation(UUID evaluationId);

    /** Persists the proposal and returns its stored state. */
    GradingProposal save(GradingProposal proposal);
}
