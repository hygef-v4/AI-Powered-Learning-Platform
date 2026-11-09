package edu.aiplatform.grading.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.contract.AiPrompt;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.ai.contract.AiTaskFailure;
import edu.aiplatform.ai.contract.AiTaskResult;
import edu.aiplatform.ai.contract.AiTaskState;
import edu.aiplatform.ai.contract.AiTaskView;
import edu.aiplatform.ai.contract.AiTokenEstimate;
import edu.aiplatform.ai.contract.ApplyOutcome;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.RubricView;
import edu.aiplatform.grading.service.GradingCommands.AiRequest;
import edu.aiplatform.grading.service.GradingCommands.Save;
import edu.aiplatform.grading.model.Evaluation;
import edu.aiplatform.grading.model.EvaluationKind;
import edu.aiplatform.grading.model.EvaluationStatus;
import edu.aiplatform.grading.model.GradingProposal;
import edu.aiplatform.grading.repository.IEvaluationRepository;
import edu.aiplatform.grading.repository.IGradingProposalRepository;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Separate bean: proposal is committed before U06 dispatch, and callbacks may
 * run immediately.
 */
@Service
public class GradingProposalTransactions {
    private static final Logger log = LoggerFactory.getLogger(GradingProposalTransactions.class);

    private final IEvaluationRepository evaluationRepository;
    private final IGradingProposalRepository gradingProposalRepository;
    private final GradingService gradingService;
    private final GradingAccess gradingAccess;
    private final SubmissionReviewService submissionReviewService;
    private final IAssignmentQuery assignmentQuery;
    private final GradingPromptBuilder gradingPromptBuilder;
    private final GradingProposalValidator gradingProposalValidator;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /** Creates proposal transaction operations with grading, persistence, AI, and clock dependencies. */
    public GradingProposalTransactions(IEvaluationRepository evaluationRepository,
            IGradingProposalRepository gradingProposalRepository, GradingService gradingService,
            GradingAccess gradingAccess, SubmissionReviewService submissionReviewService,
            IAssignmentQuery assignmentQuery, GradingPromptBuilder gradingPromptBuilder,
            GradingProposalValidator gradingProposalValidator, ObjectMapper objectMapper,
            Clock clock) {
        this.evaluationRepository = evaluationRepository;
        this.gradingProposalRepository = gradingProposalRepository;
        this.gradingService = gradingService;
        this.gradingAccess = gradingAccess;
        this.submissionReviewService = submissionReviewService;
        this.assignmentQuery = assignmentQuery;
        this.gradingPromptBuilder = gradingPromptBuilder;
        this.gradingProposalValidator = gradingProposalValidator;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public record Prepared(GradingProposal proposal, AiPrompt prompt, boolean dispatch,
            String outputSchema, int maxOutputTokens) {
        /** Defaults the output token budget for callers using the earlier prepared shape. */
        public Prepared(GradingProposal proposal, AiPrompt prompt, boolean dispatch,
                String outputSchema) {
            this(proposal, prompt, dispatch, outputSchema, 4096);
        }
    }

    /** Creates or reuses a proposal and prepares its AI prompt for dispatch. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Prepared prepareProposal(Actor actor, UUID evaluationId, AiRequest request) {
        requireAiSource(request);
        var evaluation = evaluationRepository.lockById(evaluationId)
                .orElseThrow(() -> ApiException.notFound("GRADE_NOT_FOUND"));
        gradingService.requireCurrent(actor, evaluation.id, evaluation.version);
        if (evaluation.kind == EvaluationKind.MEMBER)
            throw ApiException.badRequest("GROUP_ROOT_REQUIRED", "Use the group document");
        if (evaluation.state != EvaluationStatus.PENDING
                && evaluation.state != EvaluationStatus.DRAFT)
            throw ApiException.conflict("INVALID_GRADE_STATE",
                    "AI requests require a pending or draft grade");
        var existing = gradingProposalRepository.findByRequestRef(request.requestRef());
        if (existing.isPresent()) {
            var proposal = existing.get();
            requireSameRequest(actor, evaluation, request, proposal);
            return new Prepared(proposal, null, false, null, 4096);
        }
        int maxOutputTokens = gradingPromptBuilder.calculateMaxOutputTokens(evaluation);
        var prompt = buildPrompt(actor, evaluation);
        var outputSchema = gradingPromptBuilder.buildOutputSchema(evaluation);
        var proposal = createProposal(actor, evaluation, request);
        gradingProposalRepository.save(proposal);
        gradingService.recordAudit(actor, evaluation, "AI_GRADING_REQUESTED", null);
        return new Prepared(proposal, prompt, true, outputSchema, maxOutputTokens);
    }

    /** Estimates prompt and output tokens for the current evaluation version. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long estimateTokens(Actor actor, UUID evaluationId, long version) {
        var evaluation = gradingService.requireCurrent(actor, evaluationId, version);
        if (evaluation.kind == EvaluationKind.MEMBER)
            throw ApiException.badRequest("GROUP_ROOT_REQUIRED", "Use the group document");
        if (evaluation.state != EvaluationStatus.PENDING
                && evaluation.state != EvaluationStatus.DRAFT)
            throw ApiException.conflict("INVALID_GRADE_STATE",
                    "AI proposals require a pending or draft grade");
        int maxOutputTokens = gradingPromptBuilder.calculateMaxOutputTokens(evaluation);
        var prompt = buildPrompt(actor, evaluation);
        int promptChars = (prompt.system() == null ? 0 : prompt.system().length())
                + prompt.user().length();
        return AiTokenEstimate.estimate(promptChars, maxOutputTokens);
    }

    /** Builds a prompt after confirming the assignment rubric matches the locked snapshot. */
    private edu.aiplatform.ai.contract.AiPrompt buildPrompt(Actor actor, Evaluation evaluation) {
        var content = assignmentQuery.getContent(evaluation.assignmentId)
                .orElseThrow(() -> ApiException.unavailable("ASSIGNMENT_UNAVAILABLE",
                        "Assignment content unavailable"));
        if (!GradingJson.writeSnapshot(objectMapper, content.rubrics())
                .equals(evaluation.rubricSnapshot))
            throw ApiException.conflict("RUBRIC_CHANGED",
                    "Locked rubric no longer matches submitted snapshot");
        var detail = submissionReviewService.loadEvaluation(actor, evaluation.id);
        return gradingPromptBuilder.buildPrompt(evaluation, detail.document(), content);
    }

    /** Validates the AI source and request reference before proposal creation. */
    private void requireAiSource(AiRequest request) {
        // Nguồn bỏ trống: hệ thống tự chọn (yêu cầu 2026-11-05).
        if (request == null || Evaluation.isBlankText(request.requestRef())
                || request.requestRef().length() > 200
                || request.provider() != null && request.source() != AiSource.PERSONAL)
            throw ApiException.badRequest("AI_SOURCE_REQUIRED", "Invalid AI source");
    }

    /** Ensures an existing request reference belongs to the same actor and evaluation snapshot. */
    private void requireSameRequest(Actor actor, Evaluation evaluation, AiRequest request,
            GradingProposal proposal) {
        if (!proposal.requesterId.equals(actor.accountId())
                || !proposal.evaluationId.equals(evaluation.id)
                || !proposal.submissionHash.equals(evaluation.submissionHash)
                || !Objects.equals(proposal.source,
                        request.source() == null ? "AUTO" : request.source().name())
                || !Objects.equals(proposal.provider,
                        request.provider() == null ? null : request.provider().name()))
            throw ApiException.conflict("REQUEST_REF_CONFLICT", "Request reference already used");
    }

    /** Creates a pending proposal record linked to the current evaluation snapshot. */
    private GradingProposal createProposal(Actor actor, Evaluation evaluation, AiRequest request) {
        var proposal = new GradingProposal();
        proposal.id = UUID.randomUUID();
        proposal.evaluationId = evaluation.id;
        proposal.requesterId = actor.accountId();
        proposal.requestRef = request.requestRef();
        proposal.versionNo = gradingProposalRepository.findForEvaluation(evaluation.id).stream()
                .mapToLong(v -> v.versionNo).max().orElse(0) + 1;
        proposal.submissionHash = evaluation.submissionHash;
        proposal.rubricFingerprint = evaluation.rubricSnapshot;
        proposal.source = request.source() == null ? "AUTO" : request.source().name();
        proposal.provider = request.provider() == null ? null : request.provider().name();
        proposal.createdAt = clock.instant();
        return proposal;
    }

    /** Records dispatch state or a safe failure outcome for an AI proposal. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GradingProposal recordDispatch(UUID proposalId, AiTaskView task, String error) {
        var evaluationId = gradingProposalRepository.findEvaluationId(proposalId).orElseThrow();
        evaluationRepository.lockById(evaluationId).orElseThrow();
        var proposal = gradingProposalRepository.findById(proposalId).orElseThrow();
        if (task != null) {
            if (proposal.taskId != null && !proposal.taskId.equals(task.taskId())) {
                proposal.status = "REJECTED";
                proposal.safeError = "Task correlation mismatch";
                proposal.taskState = null;
                if (proposal.completedAt == null)
                    proposal.completedAt = clock.instant();
            } else {
                proposal.taskId = task.taskId();
                if (task.model() != null) {
                    proposal.model = task.model();
                }
                if (proposal.usage == null || proposal.usage.isNull()
                        || task.state() == AiTaskState.READY) {
                    proposal.usage = objectMapper.valueToTree(task.usage());
                }
                if ((proposal.status.equals("PENDING") || isLegacyFailure(proposal))
                        && Set.of(AiTaskState.FAILED, AiTaskState.CANCELLED, AiTaskState.UNKNOWN)
                                .contains(task.state())) {
                    proposal.status = "FAILED";
                    proposal.safeError = formatTaskError(task.safeError());
                    proposal.taskState = null;
                    if (proposal.completedAt == null)
                        proposal.completedAt = clock.instant();
                } else if (proposal.status.equals("PENDING") && task.state() == AiTaskState.READY
                        && task.applyResult() != null
                        && task.applyResult() != ApplyOutcome.APPLIED) {
                    proposal.status = task.applyResult() == ApplyOutcome.STALE ? "STALE"
                            : "REJECTED";
                    if ("REJECTED".equals(proposal.status)) {
                        proposal.safeError = task.safeError() == null || task.safeError().isBlank()
                                ? "AI proposal cannot be applied to this submission"
                                : formatTaskError(task.safeError());
                    }
                    proposal.taskState = null;
                    if (proposal.completedAt == null)
                        proposal.completedAt = clock.instant();
                } else if (proposal.status.equals("PENDING")) {
                    proposal.taskState = task.state().name();
                } else {
                    proposal.taskState = null;
                }
            }
        } else if (proposal.status.equals("PENDING")) {
            proposal.status = "FAILED";
            proposal.safeError = error == null ? "AI service unavailable" : error;
            proposal.taskState = null;
            proposal.completedAt = clock.instant();
        }
        return gradingProposalRepository.save(proposal);
    }

    /** Validates and stores a completed AI proposal, returning its application outcome. */
    @Transactional
    public ApplyOutcome acceptProposal(AiTaskResult result) {
        if (!"GRADING_PROPOSAL".equals(result.taskType()) || result.actor() == null
                || result.targetRef() == null || !result.targetRef().startsWith("evaluation:"))
            return ApplyOutcome.REJECTED;
        UUID evaluationId;
        try {
            evaluationId = UUID.fromString(result.targetRef().substring(11));
        } catch (RuntimeException ex) {
            return ApplyOutcome.REJECTED;
        }
        var found = evaluationRepository.lockById(evaluationId);
        if (found.isEmpty())
            return ApplyOutcome.REJECTED;
        var evaluation = found.get();
        var match = gradingProposalRepository.findForEvaluation(evaluationId).stream()
                .filter(proposal -> proposal.versionNo == result.inputVersion()).findFirst();
        if (match.isEmpty())
            return ApplyOutcome.REJECTED;
        var proposal = match.get();
        if (proposal.taskId != null && !proposal.taskId.equals(result.taskId()))
            return ApplyOutcome.REJECTED;
        if (!proposal.requesterId.equals(result.actor().accountId())) {
            if ("PENDING".equals(proposal.status)) {
                proposal.status = "REJECTED";
                proposal.taskId = result.taskId();
                proposal.taskState = null;
                proposal.safeError = "AI proposal cannot be applied to this submission";
                proposal.completedAt = clock.instant();
                gradingProposalRepository.save(proposal);
            }
            return ApplyOutcome.REJECTED;
        }
        if (!proposal.status.equals("PENDING"))
            return proposal.status.equals("READY") ? ApplyOutcome.APPLIED
                    : proposal.status.equals("STALE") ? ApplyOutcome.STALE : ApplyOutcome.REJECTED;
        if (!proposal.submissionHash.equals(evaluation.submissionHash)
                || !proposal.rubricFingerprint.equals(evaluation.rubricSnapshot)
                || evaluation.state != EvaluationStatus.PENDING
                        && evaluation.state != EvaluationStatus.DRAFT) {
            proposal.status = "STALE";
            proposal.taskId = result.taskId();
            proposal.taskState = null;
            proposal.completedAt = clock.instant();
            gradingProposalRepository.save(proposal);
            log.info(
                    "AI grading proposal stale evaluationId={} proposalId={} taskId={} versionNo={}",
                    evaluationId, proposal.id, result.taskId(), proposal.versionNo);
            return ApplyOutcome.STALE;
        }
        try {
            gradingAccess.requireTeacher(result.actor(), evaluation.classId, Actions.GRADING_WRITE);
            gradingService.requireCurrent(result.actor(), evaluation.id, evaluation.version);
            var rubrics = GradingJson.readRubrics(objectMapper, evaluation.rubricSnapshot);
            var validBlockIds = resolveValidBlockIds(result.actor(), evaluation, rubrics);
            var payload = gradingProposalValidator.parseProposal(result.output());
            gradingProposalValidator.validateProposal(payload, rubrics, validBlockIds);
            proposal.proposal = payload;
            proposal.status = "READY";
            proposal.taskId = result.taskId();
            proposal.taskState = null;
            proposal.completedAt = clock.instant();
            gradingProposalRepository.save(proposal);
            log.info(
                    "AI grading proposal ready evaluationId={} proposalId={} taskId={} versionNo={}",
                    evaluationId, proposal.id, result.taskId(), proposal.versionNo);
            return ApplyOutcome.APPLIED;
        } catch (ApiException ex) {
            if (ex.status().is5xxServerError()) {
                throw ex;
            }
            proposal.status = ex.status().value() == 409 ? "STALE" : "REJECTED";
            proposal.taskId = result.taskId();
            proposal.taskState = null;
            proposal.safeError = "INVALID_AI_PROPOSAL".equals(ex.code())
                    ? "Phản hồi AI chưa hợp lệ ("
                            + GradingProposalValidator.buildRejectionDiagnostic(ex)
                            + "). Bạn vẫn có thể chấm thủ công."
                    : "AI proposal cannot be applied to this submission";
            proposal.completedAt = clock.instant();
            gradingProposalRepository.save(proposal);
            log.warn(
                    "AI grading proposal rejected evaluationId={} proposalId={} taskId={} versionNo={} status={} code={} diagnostic={}",
                    evaluationId, proposal.id, result.taskId(), proposal.versionNo, proposal.status,
                    ex.code(), GradingProposalValidator.buildRejectionDiagnostic(ex));
            return proposal.status.equals("STALE") ? ApplyOutcome.STALE : ApplyOutcome.REJECTED;
        }
    }

    /** Resolves submitted document block IDs for feedback evidence validation. */
    private Set<String> resolveValidBlockIds(Actor actor, Evaluation evaluation,
            List<RubricView> rubrics) {
        if (submissionReviewService == null) {
            return GradingFeedbackValidator.listValidBlockIds(rubrics, null);
        }
        var detail = submissionReviewService.loadEvaluation(actor, evaluation.id);
        DocumentModel doc = detail == null ? null : detail.document();
        return GradingFeedbackValidator.listValidBlockIds(rubrics, doc);
    }

    /** Marks proposals stale when their evaluation snapshot has changed. */
    @Transactional
    public void invalidateChangedSnapshot(UUID evaluationId) {
        var evaluation = evaluationRepository.lockById(evaluationId)
                .orElseThrow(() -> ApiException.notFound("GRADE_NOT_FOUND"));
        for (var proposal : gradingProposalRepository.findForEvaluation(evaluationId))
            if (Set.of("PENDING", "READY").contains(proposal.status)
                    && (!Objects.equals(proposal.submissionHash, evaluation.submissionHash)
                            || !Objects.equals(proposal.rubricFingerprint,
                                    evaluation.rubricSnapshot)
                            || evaluation.state == EvaluationStatus.SUPERSEDED)) {
                proposal.status = "STALE";
                proposal.taskState = null;
                proposal.completedAt = clock.instant();
                gradingProposalRepository.save(proposal);
            }
    }

    /** Records an AI task failure against its matching pending proposal. */
    @Transactional
    public void recordFailure(AiTaskFailure failure) {
        if (failure.targetRef() == null || !failure.targetRef().startsWith("evaluation:")
                || !"GRADING_PROPOSAL".equals(failure.taskType()))
            return;
        UUID evaluationId;
        try {
            evaluationId = UUID.fromString(failure.targetRef().substring(11));
        } catch (RuntimeException ex) {
            return;
        }
        if (evaluationRepository.lockById(evaluationId).isEmpty())
            return;
        for (var proposal : gradingProposalRepository.findForEvaluation(evaluationId)) {
            boolean matchesTask = Objects.equals(proposal.taskId, failure.taskId())
                    || (proposal.taskId == null && failure.inputVersion() != null
                            && proposal.versionNo == failure.inputVersion());
            boolean matchesActor = failure.actor() == null
                    || proposal.requesterId.equals(failure.actor().accountId());
            if (proposal.status.equals("PENDING") && matchesTask && matchesActor) {
                proposal.taskId = failure.taskId();
                proposal.status = failure.state() == AiTaskState.READY ? "REJECTED" : "FAILED";
                proposal.taskState = null;
                proposal.safeError = formatTaskError(failure.safeError());
                proposal.completedAt = clock.instant();
                gradingProposalRepository.save(proposal);
                log.info(
                        "AI grading proposal failed evaluationId={} proposalId={} taskId={} versionNo={} status={}",
                        evaluationId, proposal.id, failure.taskId(), proposal.versionNo,
                        proposal.status);
            }
        }
    }

    /** Identifies failed proposals that predate explicit task state tracking. */
    static boolean isLegacyFailure(GradingProposal proposal) {
        return "FAILED".equals(proposal.status) && (proposal.safeError == null
                || Set.of("AI task failed", "AI task unavailable or failed")
                        .contains(proposal.safeError));
    }

    /** Supplies a fallback message when the AI task has no diagnostic. */
    private static String formatTaskError(String diagnostic) {
        return diagnostic == null || diagnostic.isBlank()
                ? "Không thể tạo đề xuất AI. Bạn vẫn có thể chấm thủ công."
                : diagnostic;
    }

    /** Saves a grade using its ready proposal and marks that proposal as used. */
    @Transactional
    public Evaluation saveUsingProposal(Actor actor, UUID evaluationId, Save input) {
        // Step 1: Enforce optimistic lock checking on the current evaluation version
        var evaluation = gradingService.requireCurrent(actor, evaluationId, input.version());

        // Step 2: Validate that proposal exists, is in READY state, and matches submission & rubric hashes
        var proposal = gradingProposalRepository.findById(input.usedProposalId())
                .orElseThrow(() -> ApiException.notFound("PROPOSAL_NOT_FOUND"));
        if (!evaluationId.equals(proposal.evaluationId) || !"READY".equals(proposal.status)
                || !evaluation.submissionHash.equals(proposal.submissionHash)
                || !evaluation.rubricSnapshot.equals(proposal.rubricFingerprint))
            throw ApiException.conflict("STALE_PROPOSAL", "Proposal is stale or unavailable");

        // Step 3: Apply proposal scores to evaluation draft and save
        var saved = gradingService.saveCheckedEvaluation(actor, evaluationId, input, proposal);

        // Step 4: Mark proposal as consumed and update timestamp in database
        proposal.usedAt = clock.instant();
        gradingProposalRepository.save(proposal);
        return saved;
    }

    /** Validates and stores an MCP proposal for teacher review. */
    @Transactional
    public GradingProposal recordMcpProposal(Actor actor, UUID evaluationId, long expectedVersion,
            String hash, String requestRef, JsonNode payload, String agentLabel) {
        // Step 1: Verify caller access and validate current evaluation state and submission hash
        var evaluation = gradingService.requireCurrent(actor, evaluationId, expectedVersion);
        if (!evaluation.submissionHash.equals(hash))
            throw ApiException.conflict("STALE_PROPOSAL", "Submission changed");
        if (evaluation.kind == EvaluationKind.MEMBER)
            throw ApiException.badRequest("GROUP_ROOT_REQUIRED", "Use the group document");
        if (evaluation.state != EvaluationStatus.PENDING
                && evaluation.state != EvaluationStatus.DRAFT)
            throw ApiException.conflict("INVALID_GRADE_STATE",
                    "AI proposals require a pending or draft grade");

        // Step 2: Ensure idempotency via requestRef - return existing proposal if identical
        var existing = gradingProposalRepository.findByRequestRef(requestRef);
        if (existing.isPresent()) {
            var proposal = existing.get();
            if (!Objects.equals(proposal.rubricFingerprint, evaluation.rubricSnapshot))
                throw ApiException.conflict("STALE_PROPOSAL", "Rubric snapshot changed");
            if (!evaluationId.equals(proposal.evaluationId)
                    || !proposal.requesterId.equals(actor.accountId())
                    || !Objects.equals(proposal.submissionHash, hash)
                    || !Objects.equals(proposal.proposal, payload))
                throw ApiException.conflict("REQUEST_REF_CONFLICT",
                        "Request reference already used");
            return proposal;
        }

        // Step 3: Parse rubrics and validate proposal structure against rubric items and block IDs
        var rubrics = GradingJson.readRubrics(objectMapper, evaluation.rubricSnapshot);
        var validBlockIds = resolveValidBlockIds(actor, evaluation, rubrics);
        gradingProposalValidator.validateProposal(payload, rubrics, validBlockIds);

        // Step 4: Build proposal entity with origin MCP and READY status
        var proposal = new GradingProposal();
        proposal.id = UUID.randomUUID();
        proposal.evaluationId = evaluationId;
        proposal.versionNo = gradingProposalRepository.findForEvaluation(evaluationId).stream()
                .mapToLong(v -> v.versionNo).max().orElse(0) + 1;
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

        // Step 5: Record audit event and persist proposal entity in repository
        gradingService.recordAudit(actor, evaluation, "AI_GRADING_REQUESTED", null);
        return gradingProposalRepository.save(proposal);
    }
}
