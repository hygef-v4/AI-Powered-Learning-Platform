package edu.aiplatform.grading.service;

import edu.aiplatform.grading.dto.GradingViews;

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
    private final GradingProposalTransactions tx;
    private final IAiTask ai;
    private final IEvaluationRepository evaluations;
    private final IGradingProposalRepository proposals;
    private final GradingAccess access;

    public GradingProposalService(GradingProposalTransactions tx, IAiTask ai, IEvaluationRepository evaluations,
            IGradingProposalRepository proposals, GradingAccess access) {
        this.tx = tx;
        this.ai = ai;
        this.evaluations = evaluations;
        this.proposals = proposals;
        this.access = access;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public GradingProposal request(Actor actor, UUID evaluationId, AiRequest input) {
        var prepared = tx.prepare(actor, evaluationId, input);
        var proposal = prepared.proposal();
        if (!prepared.dispatch())
            return proposal;
        AiTaskView task;
        try {
            task = ai.submit(new AiTaskRequest(actor, input.source(), input.provider(), "GRADING_PROPOSAL",
                    "evaluation:" + evaluationId, proposal.versionNo, input.requestRef(), prepared.prompt(),
                    prepared.outputSchema(), prepared.maxOutputTokens()));
        } catch (ApiException ex) {
            return tx.dispatched(proposal.id, null, ex.code());
        } catch (RuntimeException ex) {
            return tx.dispatched(proposal.id, null, "AI_SERVICE_UNAVAILABLE");
        }
        return tx.dispatched(proposal.id, task, null);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<GradingProposal> list(Actor actor, UUID evaluationId) {
        var evaluation = evaluations.findById(evaluationId).orElseThrow(() -> ApiException.notFound("GRADE_NOT_FOUND"));
        access.requireTeacher(actor, evaluation.classId, Actions.GRADING_REVIEW);
        tx.invalidateChangedSnapshot(evaluationId);
        var current = proposals.findForEvaluation(evaluationId);
        boolean reconciled = false;
        for (var proposal : current)
            if (("PENDING".equals(proposal.status) || GradingProposalTransactions.legacyFailure(proposal)) && proposal.taskId != null) {
                // Failure or rejection callback can precede task-id attachment. Reconcile terminal task
                // state on polling and expose live task state while pending.
                Optional<AiTaskView> maybeTask = Optional.ofNullable(ai.get(proposal.taskId)).flatMap(o -> o);
                if (maybeTask.isPresent()) {
                    var task = maybeTask.get();
                    if (Set.of(AiTaskState.FAILED, AiTaskState.CANCELLED, AiTaskState.UNKNOWN).contains(task.state())
                            || ("PENDING".equals(proposal.status) && task.state() == AiTaskState.READY
                                    && task.applyResult() != null && task.applyResult() != ApplyOutcome.APPLIED)) {
                        tx.dispatched(proposal.id, task, null);
                        reconciled = true;
                    } else if ("PENDING".equals(proposal.status)) {
                        proposal.taskState = task.state().name();
                    }
                }
            }
        var listed = reconciled ? proposals.findForEvaluation(evaluationId) : current;
        if (reconciled) {
            for (var proposal : listed) {
                if ("PENDING".equals(proposal.status) && proposal.taskId != null) {
                    Optional.ofNullable(ai.get(proposal.taskId)).flatMap(o -> o)
                            .ifPresent(task -> proposal.taskState = task.state().name());
                }
            }
        }
        return listed;
    }

    public BatchResult requestBatch(Actor actor, AiBatch batch) {
        if (batch.items() == null || batch.items().isEmpty() || batch.items().size() > 200)
            throw ApiException.badRequest("INVALID_BATCH", "Batch must contain 1–200 items");
        List<ItemResult> outcomes = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        boolean sourceBlocked = false;
        for (var item : batch.items()) {
            if (!seen.add(item.evaluationId())) {
                outcomes.add(new ItemResult(item.evaluationId(), "SKIPPED", "DUPLICATE_ITEM", null));
                continue;
            }
            if (sourceBlocked) {
                outcomes.add(new ItemResult(item.evaluationId(), "SKIPPED", "AI_SOURCE_UNAVAILABLE", null));
                continue;
            }
            try {
                var evaluation = evaluations.findById(item.evaluationId())
                        .orElseThrow(() -> ApiException.notFound("GRADE_NOT_FOUND"));
                access.requireTeacher(actor, evaluation.classId, Actions.GRADING_WRITE);
                evaluation.checkVersion(item.version());
                var request = batch.request();
                var proposal = request(actor, evaluation.id,
                        new AiRequest(request.source(), request.provider(),
                                "u09-batch:" + UUID.nameUUIDFromBytes((request.requestRef() + ":" + evaluation.id)
                                        .getBytes(java.nio.charset.StandardCharsets.UTF_8))));
                outcomes.add(new ItemResult(evaluation.id, proposal.status, proposal.safeError, null));
                if ("FAILED".equals(proposal.status))
                    sourceBlocked = true;
            } catch (ApiException ex) {
                outcomes.add(new ItemResult(item.evaluationId(), "FAILED", ex.code(), null));
            } catch (RuntimeException ex) {
                outcomes.add(new ItemResult(item.evaluationId(), "FAILED", "AI_SERVICE_UNAVAILABLE", null));
                sourceBlocked = true;
            }
        }
        return new BatchResult(outcomes);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AiEstimate estimateBatch(Actor actor, Batch batch) {
        if (batch.items() == null || batch.items().isEmpty() || batch.items().size() > 200)
            throw ApiException.badRequest("INVALID_BATCH", "Batch must contain 1–200 items");
        Set<UUID> seen = new HashSet<>();
        long estimatedTokens = 0;
        int items = 0;
        for (var item : batch.items()) {
            if (!seen.add(item.evaluationId()))
                continue;
            long itemEstimate = tx.estimateTokens(actor, item.evaluationId(), item.version());
            estimatedTokens = Math.addExact(estimatedTokens, itemEstimate);
            items++;
        }
        return new AiEstimate(estimatedTokens, items);
    }
}
