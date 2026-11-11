package edu.aiplatform.grading.service;


import edu.aiplatform.ai.contract.AiTaskRequest;
import edu.aiplatform.ai.contract.AiTaskState;
import edu.aiplatform.ai.contract.AiTaskView;
import edu.aiplatform.ai.contract.ApplyOutcome;
import edu.aiplatform.ai.contract.IAiTask;
import edu.aiplatform.grading.service.GradingCommands.AiBatch;
import edu.aiplatform.grading.service.GradingCommands.AiRequest;
import edu.aiplatform.grading.service.GradingCommands.Batch;
import edu.aiplatform.grading.dto.GradingViews.AiEstimate;
import edu.aiplatform.grading.dto.GradingViews.BatchResult;
import edu.aiplatform.grading.dto.GradingViews.ItemResult;
import edu.aiplatform.grading.model.GradingProposal;
import edu.aiplatform.grading.repository.IEvaluationRepository;
import edu.aiplatform.grading.repository.IGradingProposalRepository;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GradingProposalService {
    private final GradingProposalTransactions gradingProposalTransactions;
    private final IAiTask aiTask;
    private final IEvaluationRepository evaluationRepository;
    private final IGradingProposalRepository gradingProposalRepository;
    private final GradingAccess gradingAccess;

    /** Creates the proposal service with AI task, repository, and grading access dependencies. */
    public GradingProposalService(GradingProposalTransactions gradingProposalTransactions,
            IAiTask aiTask, IEvaluationRepository evaluationRepository,
            IGradingProposalRepository gradingProposalRepository, GradingAccess gradingAccess) {
        this.gradingProposalTransactions = gradingProposalTransactions;
        this.aiTask = aiTask;
        this.evaluationRepository = evaluationRepository;
        this.gradingProposalRepository = gradingProposalRepository;
        this.gradingAccess = gradingAccess;
    }

    /** Prepares and dispatches an AI grading proposal for an evaluation. */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public GradingProposal requestProposal(Actor actor, UUID evaluationId, AiRequest input) {
        // Step 1: Validate evaluation state, prepare prompt and output schema, and persist PENDING proposal
        var prepared = gradingProposalTransactions.prepareProposal(actor, evaluationId, input);
        var proposal = prepared.proposal();

        // Step 2: Check idempotency - return existing proposal if already dispatched or pending
        if (!prepared.dispatch()) {
            if (proposal.safeError != null)
                proposal.safeError = GradingProposalCallbackSupport.formatTaskError(proposal.safeError);
            return proposal;
        }

        // Step 3: Submit asynchronous task to background AI engine (U06)
        AiTaskView task;
        try {
            task = aiTask.submit(new AiTaskRequest(actor, input.source(), input.provider(),
                    "GRADING_PROPOSAL", "evaluation:" + evaluationId, proposal.versionNo,
                    input.requestRef(), prepared.prompt(), prepared.outputSchema(),
                    prepared.maxOutputTokens()));
        } catch (ApiException ex) {
            // Record failure in database if AI task submission failed with domain error
            return gradingProposalTransactions.recordDispatch(proposal.id, null, ex.code());
        } catch (RuntimeException ex) {
            // Record failure in database if AI service is unreachable or network timed out
            return gradingProposalTransactions.recordDispatch(proposal.id, null,
                    "AI_SERVICE_UNAVAILABLE");
        }

        // Step 4: Record dispatched task ID, keep PENDING status, and return proposal to caller
        return gradingProposalTransactions.recordDispatch(proposal.id, task, null);
    }

    List<GradingProposal> listStoredProposals(List<UUID> authorizedEvaluationIds) {
        return gradingProposalRepository.findForEvaluations(authorizedEvaluationIds);
    }

    /** Lists an evaluation's proposals and reconciles their current AI task state. */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<GradingProposal> listProposals(Actor actor, UUID evaluationId) {
        // 1. Verify that the evaluation exists and actor has teacher review permission
        var evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> ApiException.notFound("GRADE_NOT_FOUND"));
        gradingAccess.requireTeacher(actor, evaluation.classId, Actions.GRADING_REVIEW);

        // 2. Mark existing proposals as STALE if the submission snapshot or rubric changed
        gradingProposalTransactions.invalidateChangedSnapshot(evaluationId);

        // 3. Load proposals and reconcile terminal/in-flight AI task states
        var current = gradingProposalRepository.findForEvaluation(evaluationId);
        boolean reconciled = false;
        for (var proposal : current)
            if (("PENDING".equals(proposal.status)
                    || GradingProposalTransactions.isLegacyFailure(proposal))
                    && proposal.taskId != null) {
                // Poll background AI task state to handle completed, failed, or cancelled tasks
                Optional<AiTaskView> maybeTask = Optional.ofNullable(aiTask.get(proposal.taskId))
                        .flatMap(o -> o);
                if (maybeTask.isPresent()) {
                    var task = maybeTask.get();
                    if (Set.of(AiTaskState.FAILED, AiTaskState.CANCELLED, AiTaskState.UNKNOWN)
                            .contains(task.state())
                            || ("PENDING".equals(proposal.status)
                                    && task.state() == AiTaskState.READY
                                    && task.applyResult() != null
                                    && task.applyResult() != ApplyOutcome.APPLIED)) {
                        // Reconcile terminal state in database if callback was delayed or dropped
                        gradingProposalTransactions.recordDispatch(proposal.id, task, null);
                        reconciled = true;
                    } else if ("PENDING".equals(proposal.status)) {
                        // Expose live in-flight task state (e.g. QUEUED, RUNNING) to the client
                        proposal.taskState = task.state().name();
                    }
                }
            }

        // 4. Reload if reconciled, and expose live task state for pending proposals
        var listed = reconciled ? gradingProposalRepository.findForEvaluation(evaluationId)
                : current;
        if (reconciled) {
            for (var proposal : listed) {
                if ("PENDING".equals(proposal.status) && proposal.taskId != null) {
                    Optional.ofNullable(aiTask.get(proposal.taskId)).flatMap(o -> o)
                            .ifPresent(task -> proposal.taskState = task.state().name());
                }
            }
        }
        for (var proposal : listed) {
            if (proposal.safeError != null)
                proposal.safeError = GradingProposalCallbackSupport.formatTaskError(proposal.safeError);
        }
        return listed;
    }

    /** Requests AI grading proposals for each valid, unique evaluation in the batch. */
    public BatchResult requestProposals(Actor actor, AiBatch batch) {
        // Step 1: Validate batch size (1 to 200 items)
        GradingCommands.validateBatchItems(batch.items());
        List<ItemResult> outcomes = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        boolean sourceBlocked = false;

        // Step 2: Iterate over each evaluation item in the batch
        for (var item : batch.items()) {
            // Deduplicate items in the batch payload
            if (!seen.add(item.evaluationId())) {
                outcomes.add(
                        new ItemResult(item.evaluationId(), "SKIPPED", "DUPLICATE_ITEM", null));
                continue;
            }
            // Skip remaining items if AI source/provider was blocked by quota or failure
            if (sourceBlocked) {
                outcomes.add(new ItemResult(item.evaluationId(), "SKIPPED", "AI_SOURCE_UNAVAILABLE",
                        null));
                continue;
            }
            try {
                // Verify evaluation existence, teacher permission, and version concurrency
                var evaluation = evaluationRepository.findById(item.evaluationId())
                        .orElseThrow(() -> ApiException.notFound("GRADE_NOT_FOUND"));
                gradingAccess.requireTeacher(actor, evaluation.classId, Actions.GRADING_WRITE);
                evaluation.checkVersion(item.version());

                // Generate a deterministic per-item requestRef to ensure idempotency across retries
                var request = batch.request();
                var proposal = requestProposal(actor, evaluation.id, new AiRequest(request.source(),
                        request.provider(),
                        "u09-batch:" + UUID
                                .nameUUIDFromBytes((request.requestRef() + ":" + evaluation.id)
                                        .getBytes(java.nio.charset.StandardCharsets.UTF_8))));
                outcomes.add(
                        new ItemResult(evaluation.id, proposal.status, proposal.safeError, null));
                if ("FAILED".equals(proposal.status))
                    sourceBlocked = true;
            } catch (ApiException ex) {
                outcomes.add(new ItemResult(item.evaluationId(), "FAILED", ex.code(), null));
            } catch (RuntimeException ex) {
                outcomes.add(new ItemResult(item.evaluationId(), "FAILED", "AI_SERVICE_UNAVAILABLE",
                        null));
                sourceBlocked = true;
            }
        }
        return new BatchResult(outcomes);
    }

    /** Estimates the total AI token usage for the unique evaluations in a batch. */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AiEstimate estimateProposalTokens(Actor actor, Batch batch) {
        // Step 1: Validate batch size bounds
        GradingCommands.validateBatchItems(batch.items());
        Set<UUID> seen = new HashSet<>();
        long estimatedTokens = 0;
        int items = 0;

        // Step 2: Sum token estimates across all unique evaluations
        for (var item : batch.items()) {
            if (!seen.add(item.evaluationId()))
                continue;
            long itemEstimate = gradingProposalTransactions.estimateTokens(actor,
                    item.evaluationId(), item.version());
            estimatedTokens = Math.addExact(estimatedTokens, itemEstimate);
            items++;
        }
        return new AiEstimate(estimatedTokens, items);
    }
}
