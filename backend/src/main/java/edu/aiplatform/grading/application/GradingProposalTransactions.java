package edu.aiplatform.grading.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.contract.AiPrompt;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.ai.contract.AiTokenEstimate;
import edu.aiplatform.ai.contract.AiTaskFailure;
import edu.aiplatform.ai.contract.AiTaskResult;
import edu.aiplatform.ai.contract.AiTaskState;
import edu.aiplatform.ai.contract.AiTaskView;
import edu.aiplatform.ai.contract.ApplyOutcome;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.grading.application.GradingCommands.AiRequest;
import edu.aiplatform.grading.application.GradingCommands.Save;
import edu.aiplatform.grading.domain.Evaluation;
import edu.aiplatform.grading.domain.EvaluationKind;
import edu.aiplatform.grading.domain.EvaluationStatus;
import edu.aiplatform.grading.domain.GradingProposal;
import edu.aiplatform.grading.domain.IEvaluationRepository;
import edu.aiplatform.grading.domain.IGradingProposalRepository;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Separate bean: proposal is committed before U06 dispatch, and callbacks may
 * run immediately.
 */
@Service
public class GradingProposalTransactions {
    private final IEvaluationRepository evaluations;
    private final IGradingProposalRepository proposals;
    private final GradingService grading;
    private final GradingAccess access;
    private final SubmissionReviewService review;
    private final IAssignmentQuery assignments;
    private final GradingPromptBuilder prompts;
    private final GradingProposalValidator validator;
    private final ObjectMapper json;
    private final Clock clock;

    public GradingProposalTransactions(IEvaluationRepository evaluations, IGradingProposalRepository proposals,
            GradingService grading, GradingAccess access, SubmissionReviewService review, IAssignmentQuery assignments,
            GradingPromptBuilder prompts, GradingProposalValidator validator, ObjectMapper json, Clock clock) {
        this.evaluations = evaluations;
        this.proposals = proposals;
        this.grading = grading;
        this.access = access;
        this.review = review;
        this.assignments = assignments;
        this.prompts = prompts;
        this.validator = validator;
        this.json = json;
        this.clock = clock;
    }

    public record Prepared(GradingProposal proposal, AiPrompt prompt, boolean dispatch, String outputSchema) {
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Prepared prepare(Actor actor, UUID evaluationId, AiRequest request) {
        requireAiSource(request);
        var evaluation = evaluations.lockById(evaluationId).orElseThrow(() -> ApiException.notFound("GRADE_NOT_FOUND"));
        grading.requireCurrent(actor, evaluation.id, evaluation.version);
        if (evaluation.kind == EvaluationKind.MEMBER)
            throw ApiException.badRequest("GROUP_ROOT_REQUIRED", "Use the group document");
        if (evaluation.state != EvaluationStatus.PENDING && evaluation.state != EvaluationStatus.DRAFT)
            throw ApiException.conflict("INVALID_GRADE_STATE", "AI requests require a pending or draft grade");
        var existing = proposals.findByRequestRef(request.requestRef());
        if (existing.isPresent()) {
            var proposal = existing.get();
            requireSameRequest(actor, evaluation, request, proposal);
            return new Prepared(proposal, null, false, null);
        }
        var prompt = buildPrompt(actor, evaluation);
        var proposal = newProposal(actor, evaluation, request);
        proposals.save(proposal);
        grading.record(actor, evaluation, "AI_GRADING_REQUESTED", null);
        return new Prepared(proposal, prompt, true, prompts.outputSchema(evaluation));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long estimateTokens(Actor actor, UUID evaluationId, long version) {
        var evaluation = grading.requireCurrent(actor, evaluationId, version);
        if (evaluation.kind == EvaluationKind.MEMBER)
            throw ApiException.badRequest("GROUP_ROOT_REQUIRED", "Use the group document");
        if (evaluation.state != EvaluationStatus.PENDING && evaluation.state != EvaluationStatus.DRAFT)
            throw ApiException.conflict("INVALID_GRADE_STATE", "AI proposals require a pending or draft grade");
        var prompt = buildPrompt(actor, evaluation);
        int promptChars = (prompt.system() == null ? 0 : prompt.system().length()) + prompt.user().length();
        return AiTokenEstimate.estimate(promptChars, 4096);
    }

    private edu.aiplatform.ai.contract.AiPrompt buildPrompt(Actor actor, Evaluation evaluation) {
        var content = assignments.getContent(evaluation.assignmentId).orElseThrow(
                () -> ApiException.unavailable("ASSIGNMENT_UNAVAILABLE", "Assignment content unavailable"));
        if (!GradingJson.write(json, content.rubrics()).equals(evaluation.rubricSnapshot))
            throw ApiException.conflict("RUBRIC_CHANGED", "Locked rubric no longer matches submitted snapshot");
        var detail = review.detail(actor, evaluation.id);
        return prompts.build(evaluation, detail.document(), content);
    }

    private void requireAiSource(AiRequest request) {
        if (request == null || request.source() == null || Evaluation.blank(request.requestRef())
                || request.requestRef().length() > 200
                || request.source() == AiSource.PERSONAL && request.provider() == null)
            throw ApiException.badRequest("AI_SOURCE_REQUIRED", "Choose an AI source and provider");
    }

    private void requireSameRequest(Actor actor, Evaluation evaluation, AiRequest request, GradingProposal proposal) {
        if (!proposal.requesterId.equals(actor.accountId()) || !proposal.evaluationId.equals(evaluation.id)
                || !proposal.submissionHash.equals(evaluation.submissionHash)
                || !Objects.equals(proposal.source, request.source().name())
                || !Objects.equals(proposal.provider, request.provider() == null ? null : request.provider().name()))
            throw ApiException.conflict("REQUEST_REF_CONFLICT", "Request reference already used");
    }

    private GradingProposal newProposal(Actor actor, Evaluation evaluation, AiRequest request) {
        var proposal = new GradingProposal();
        proposal.id = UUID.randomUUID();
        proposal.evaluationId = evaluation.id;
        proposal.requesterId = actor.accountId();
        proposal.requestRef = request.requestRef();
        proposal.versionNo = proposals.findForEvaluation(evaluation.id).stream().mapToLong(v -> v.versionNo).max()
                .orElse(0) + 1;
        proposal.submissionHash = evaluation.submissionHash;
        proposal.rubricFingerprint = evaluation.rubricSnapshot;
        proposal.source = request.source().name();
        proposal.provider = request.provider() == null ? null : request.provider().name();
        proposal.createdAt = clock.instant();
        return proposal;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GradingProposal dispatched(UUID proposalId, AiTaskView task, String error) {
        var evaluationId = proposals.findEvaluationId(proposalId).orElseThrow();
        evaluations.lockById(evaluationId).orElseThrow();
        var proposal = proposals.findById(proposalId).orElseThrow();
        if (task != null) {
            if (proposal.taskId != null && !proposal.taskId.equals(task.taskId())) {
                proposal.status = "REJECTED";
                proposal.safeError = "Task correlation mismatch";
            } else {
                proposal.taskId = task.taskId();
                proposal.model = task.model();
                proposal.usage = json.valueToTree(task.usage());
                if ((proposal.status.equals("PENDING") || legacyFailure(proposal))
                        && Set.of(AiTaskState.FAILED, AiTaskState.CANCELLED, AiTaskState.UNKNOWN).contains(task.state())) {
                    proposal.status = "FAILED";
                    proposal.safeError = safeTaskError(task.safeError());
                    if (proposal.completedAt == null) proposal.completedAt = clock.instant();
                }
            }
        } else if (proposal.status.equals("PENDING")) {
            proposal.status = "FAILED";
            proposal.safeError = error == null ? "AI service unavailable" : error;
            proposal.completedAt = clock.instant();
        }
        return proposals.save(proposal);
    }

    @Transactional
    public ApplyOutcome accept(AiTaskResult result) {
        if (!"GRADING_PROPOSAL".equals(result.taskType()) || result.actor() == null || result.targetRef() == null
                || !result.targetRef().startsWith("evaluation:"))
            return ApplyOutcome.REJECTED;
        UUID evaluationId;
        try {
            evaluationId = UUID.fromString(result.targetRef().substring(11));
        } catch (RuntimeException ex) {
            return ApplyOutcome.REJECTED;
        }
        var found = evaluations.lockById(evaluationId);
        if (found.isEmpty())
            return ApplyOutcome.REJECTED;
        var evaluation = found.get();
        var match = proposals.findForEvaluation(evaluationId).stream()
                .filter(proposal -> proposal.versionNo == result.inputVersion()).findFirst();
        if (match.isEmpty())
            return ApplyOutcome.REJECTED;
        var proposal = match.get();
        if (!proposal.requesterId.equals(result.actor().accountId())
                || proposal.taskId != null && !proposal.taskId.equals(result.taskId()))
            return ApplyOutcome.REJECTED;
        if (!proposal.status.equals("PENDING"))
            return proposal.status.equals("READY") ? ApplyOutcome.APPLIED
                    : proposal.status.equals("STALE") ? ApplyOutcome.STALE : ApplyOutcome.REJECTED;
        if (!proposal.submissionHash.equals(evaluation.submissionHash)
                || !proposal.rubricFingerprint.equals(evaluation.rubricSnapshot)
                || evaluation.state != EvaluationStatus.PENDING && evaluation.state != EvaluationStatus.DRAFT) {
            proposal.status = "STALE";
            proposals.save(proposal);
            return ApplyOutcome.STALE;
        }
        try {
            access.requireTeacher(result.actor(), evaluation.classId, Actions.GRADING_WRITE);
            grading.requireCurrent(result.actor(), evaluation.id, evaluation.version);
            var payload = validator.parse(result.output());
            validator.validate(payload, GradingJson.rubrics(json, evaluation.rubricSnapshot));
            proposal.proposal = payload;
            proposal.status = "READY";
            proposal.taskId = result.taskId();
            proposal.completedAt = clock.instant();
            proposals.save(proposal);
            return ApplyOutcome.APPLIED;
        } catch (ApiException ex) {
            proposal.status = ex.status().value() == 409 ? "STALE" : "REJECTED";
            proposal.safeError = "AI proposal cannot be applied to this submission";
            proposal.completedAt = clock.instant();
            proposals.save(proposal);
            return proposal.status.equals("STALE") ? ApplyOutcome.STALE : ApplyOutcome.REJECTED;
        }
    }

    @Transactional
    public void invalidateChangedSnapshot(UUID evaluationId) {
        var evaluation = evaluations.lockById(evaluationId).orElseThrow(() -> ApiException.notFound("GRADE_NOT_FOUND"));
        for (var proposal : proposals.findForEvaluation(evaluationId))
            if (Set.of("PENDING", "READY").contains(proposal.status)
                    && (!Objects.equals(proposal.submissionHash, evaluation.submissionHash)
                            || !Objects.equals(proposal.rubricFingerprint, evaluation.rubricSnapshot)
                            || evaluation.state == EvaluationStatus.SUPERSEDED)) {
                proposal.status = "STALE";
                proposal.completedAt = clock.instant();
                proposals.save(proposal);
            }
    }

    @Transactional
    public void failed(AiTaskFailure failure) {
        if (failure.targetRef() == null || !failure.targetRef().startsWith("evaluation:")
                || !"GRADING_PROPOSAL".equals(failure.taskType()))
            return;
        UUID evaluationId;
        try {
            evaluationId = UUID.fromString(failure.targetRef().substring(11));
        } catch (RuntimeException ex) {
            return;
        }
        if (evaluations.lockById(evaluationId).isEmpty())
            return;
        for (var proposal : proposals.findForEvaluation(evaluationId))
            if (proposal.status.equals("PENDING") && Objects.equals(proposal.taskId, failure.taskId())
                    && failure.actor() != null && proposal.requesterId.equals(failure.actor().accountId())) {
                proposal.status = "FAILED";
                proposal.safeError = safeTaskError(failure.safeError());
                proposal.completedAt = clock.instant();
                proposals.save(proposal);
            }
    }

    static boolean legacyFailure(GradingProposal proposal) {
        return "FAILED".equals(proposal.status) && (proposal.safeError == null
                || Set.of("AI task failed", "AI task unavailable or failed").contains(proposal.safeError));
    }

    private static String safeTaskError(String diagnostic) {
        return diagnostic == null || diagnostic.isBlank()
                ? "Không thể tạo đề xuất AI. Bạn vẫn có thể chấm thủ công." : diagnostic;
    }

    @Transactional
    public Evaluation saveUsingProposal(Actor actor, UUID evaluationId, Save input) {
        var evaluation = grading.requireCurrent(actor, evaluationId, input.version());
        var proposal = proposals.findById(input.usedProposalId())
                .orElseThrow(() -> ApiException.notFound("PROPOSAL_NOT_FOUND"));
        if (!evaluationId.equals(proposal.evaluationId) || !"READY".equals(proposal.status)
                || !evaluation.submissionHash.equals(proposal.submissionHash)
                || !evaluation.rubricSnapshot.equals(proposal.rubricFingerprint))
            throw ApiException.conflict("STALE_PROPOSAL", "Proposal is stale or unavailable");
        var saved = grading.saveChecked(actor, evaluationId, input);
        proposal.usedAt = clock.instant();
        proposals.save(proposal);
        return saved;
    }

    @Transactional
    public GradingProposal recordMcp(Actor actor, UUID evaluationId, long expectedVersion, String hash,
            String requestRef, JsonNode payload, String agentLabel) {
        var evaluation = grading.requireCurrent(actor, evaluationId, expectedVersion);
        if (!evaluation.submissionHash.equals(hash))
            throw ApiException.conflict("STALE_PROPOSAL", "Submission changed");
        if (evaluation.kind == EvaluationKind.MEMBER)
            throw ApiException.badRequest("GROUP_ROOT_REQUIRED", "Use the group document");
        if (evaluation.state != EvaluationStatus.PENDING && evaluation.state != EvaluationStatus.DRAFT)
            throw ApiException.conflict("INVALID_GRADE_STATE", "AI proposals require a pending or draft grade");
        validator.validate(payload, GradingJson.rubrics(json, evaluation.rubricSnapshot));
        var existing = proposals.findByRequestRef(requestRef);
        if (existing.isPresent()) {
            var proposal = existing.get();
            if (!evaluationId.equals(proposal.evaluationId) || !proposal.requesterId.equals(actor.accountId())
                    || !Objects.equals(proposal.submissionHash, hash) || !Objects.equals(proposal.proposal, payload))
                throw ApiException.conflict("REQUEST_REF_CONFLICT", "Request reference already used");
            return proposal;
        }
        var proposal = new GradingProposal();
        proposal.id = UUID.randomUUID();
        proposal.evaluationId = evaluationId;
        proposal.versionNo = proposals.findForEvaluation(evaluationId).stream().mapToLong(v -> v.versionNo).max()
                .orElse(0) + 1;
        proposal.requesterId = actor.accountId();
        proposal.requestRef = requestRef;
        proposal.submissionHash = hash;
        proposal.rubricFingerprint = evaluation.rubricSnapshot;
        proposal.origin = "MCP";
        proposal.source = "MCP";
        proposal.agentLabel = agentLabel;
        proposal.proposal = payload;
        proposal.status = "READY";
        proposal.createdAt = clock.instant();
        proposal.completedAt = proposal.createdAt;
        grading.record(actor, evaluation, "AI_GRADING_REQUESTED", null);
        return proposals.save(proposal);
    }
}
