package edu.aiplatform.grading.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.contract.AiPrompt;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.ai.contract.AiTaskFailure;
import edu.aiplatform.ai.contract.AiTaskResult;
import edu.aiplatform.ai.contract.AiTaskView;
import edu.aiplatform.ai.contract.AiTokenEstimate;
import edu.aiplatform.ai.contract.ApplyOutcome;
import edu.aiplatform.assignments.contract.AssignmentContent;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.RubricView;
import edu.aiplatform.grading.model.Evaluation;
import edu.aiplatform.grading.model.EvaluationKind;
import edu.aiplatform.grading.model.EvaluationStatus;
import edu.aiplatform.grading.model.GradingProposal;
import edu.aiplatform.grading.repository.IEvaluationRepository;
import edu.aiplatform.grading.repository.IGradingProposalRepository;
import edu.aiplatform.grading.service.GradingCommands.AiRequest;
import edu.aiplatform.grading.service.GradingCommands.Save;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Separate transactions for request preparation, task attachment, and callback delivery. */
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
        public Prepared(GradingProposal proposal, AiPrompt prompt, boolean dispatch,
                String outputSchema) {
            this(proposal, prompt, dispatch, outputSchema, 4096);
        }
    }

    /** Creates or reuses a proposal and prepares its AI prompt for dispatch. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Prepared prepareProposal(Actor actor, UUID evaluationId, AiRequest request) {
        return prepareProposal(actor, evaluationId, request, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Prepared prepareProposal(Actor actor, UUID evaluationId, AiRequest request,
            Long expectedVersion) {         // kiểm tra phiên bản bài nộp
        requireAiSource(request);
        var evaluation = evaluationRepository.lockById(evaluationId)
                .orElseThrow(() -> ApiException.createNotFound("GRADE_NOT_FOUND"));
        gradingService.requireCurrent(actor, evaluation.id,
                expectedVersion == null ? evaluation.version : expectedVersion);
        requireGradableTarget(evaluation, "AI requests require a pending or draft grade");
        var existing = gradingProposalRepository.findByRequestRef(request.requestRef()).orElse(null);
        if (existing != null) {
            requireSameRequest(actor, evaluation, request, existing);
            if (!canDispatchProposal(existing)) {
                return new Prepared(existing, null, false, null, 4096);
            }
        }
        var content = loadValidatedAssignmentContent(evaluation);
        var detail = submissionReviewService.loadEvaluation(actor, evaluation.id);
        DocumentModel document = detail == null ? null : detail.document();
        var rubrics = GradingJson.readRubrics(objectMapper, evaluation.rubricSnapshot);
        List<RubricView> activeRubrics = gradingPromptBuilder.findActiveRubrics(
                document, content.skeleton(), rubrics);
        if (activeRubrics.isEmpty()) {
            return completeZeroProposal(actor, evaluation, request, rubrics, existing);
        }
        Set<String> emptyPartIds = gradingPromptBuilder.findEmptyPartIds(document, content.skeleton());
        int maxOutputTokens = gradingPromptBuilder.calculateMaxOutputTokens(activeRubrics);
        var prompt = gradingPromptBuilder.buildPrompt(
                evaluation, document, content, activeRubrics, emptyPartIds);
        var outputSchema = gradingPromptBuilder.buildProviderSchema(activeRubrics);
        return new Prepared(existing == null ? saveRequestedProposal(actor, evaluation, request) : existing,
                prompt, true, outputSchema, maxOutputTokens);
    }

    private Prepared completeZeroProposal(Actor actor, Evaluation evaluation, AiRequest request,
            List<RubricView> rubrics, GradingProposal existing) {
        var proposal = existing == null ? createProposal(actor, evaluation, request) : existing;
        proposal.proposal = GradingPromptBuilder.createZeroProposal(rubrics, objectMapper);
        proposal.status = "READY";
        proposal.completedAt = clock.instant();
        gradingProposalRepository.saveProposal(proposal);
        log.info("AI grading proposal completed immediately for empty submission evaluationId={} proposalId={}",
                evaluation.id, proposal.id);
        return new Prepared(proposal, null, false, null, 0);
    }

    private GradingProposal saveRequestedProposal(Actor actor, Evaluation evaluation, AiRequest request) {
        var proposal = gradingProposalRepository.saveProposal(createProposal(actor, evaluation, request));
        return proposal;
    }

    /** Estimates prompt and output tokens for the current evaluation version. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long estimateTokens(Actor actor, UUID evaluationId, long version) {
        var evaluation = gradingService.requireCurrent(actor, evaluationId, version);
        requireGradableTarget(evaluation, "AI proposals require a pending or draft grade");
        var content = loadValidatedAssignmentContent(evaluation);
        var detail = submissionReviewService.loadEvaluation(actor, evaluation.id);
        DocumentModel document = detail == null ? null : detail.document();
        var rubrics = GradingJson.readRubrics(objectMapper, evaluation.rubricSnapshot);
        List<RubricView> activeRubrics = gradingPromptBuilder.findActiveRubrics(
                document, content.skeleton(), rubrics);
        if (activeRubrics.isEmpty()) {
            return 0L;
        }
        Set<String> emptyPartIds = gradingPromptBuilder.findEmptyPartIds(document, content.skeleton());
        int maxOutputTokens = gradingPromptBuilder.calculateMaxOutputTokens(activeRubrics);
        var prompt = gradingPromptBuilder.buildPrompt(
                evaluation, document, content, activeRubrics, emptyPartIds);
        int promptChars = (prompt.system() == null ? 0 : prompt.system().length())
                + prompt.user().length();
        return AiTokenEstimate.estimate(promptChars, maxOutputTokens);
    }

    private void requireGradableTarget(Evaluation evaluation, String stateMessage) {
        if (evaluation.kind == EvaluationKind.MEMBER) {
            throw ApiException.createBadRequest("GROUP_ROOT_REQUIRED", "Use the group document");
        }
        if (evaluation.state != EvaluationStatus.PENDING
                && evaluation.state != EvaluationStatus.DRAFT
                && evaluation.state != EvaluationStatus.FINALIZED
                && evaluation.state != EvaluationStatus.PUBLISHED) {
            throw ApiException.createConflict("INVALID_GRADE_STATE", stateMessage);
        }
    }

    private AssignmentContent loadValidatedAssignmentContent(Evaluation evaluation) {
        var content = assignmentQuery.getContent(evaluation.assignmentId)
                .orElseThrow(() -> ApiException.createUnavailable("ASSIGNMENT_UNAVAILABLE",
                        "Assignment content unavailable"));
        if (!GradingJson.writeSnapshot(objectMapper, content.rubrics())
                .equals(evaluation.rubricSnapshot)) {
            throw ApiException.createConflict("RUBRIC_CHANGED",
                    "Locked rubric no longer matches submitted snapshot");
        }
        return content;
    }

    private void requireAiSource(AiRequest request) {
        if (request == null || Evaluation.isBlankText(request.requestRef())
                || request.requestRef().length() > 200
                || request.provider() != null && request.source() != AiSource.PERSONAL) {
            throw ApiException.createBadRequest("AI_SOURCE_REQUIRED", "Invalid AI source");
        }
    }

    private void requireSameRequest(Actor actor, Evaluation evaluation, AiRequest request,
            GradingProposal proposal) {
        if (!proposal.requesterId.equals(actor.accountId())
                || !proposal.evaluationId.equals(evaluation.id)
                || !matchesEvaluationSnapshot(proposal, evaluation)
                || !Objects.equals(proposal.source,
                        request.source() == null ? "AUTO" : request.source().name())
                || !Objects.equals(proposal.provider,
                        request.provider() == null ? null : request.provider().name())) {
            throw ApiException.createConflict("REQUEST_REF_CONFLICT", "Request reference already used");
        }
    }

    private static boolean matchesEvaluationSnapshot(GradingProposal proposal, Evaluation evaluation) {
        return Objects.equals(proposal.submissionHash, evaluation.submissionHash)
                && Objects.equals(proposal.rubricFingerprint, evaluation.rubricSnapshot);
    }

    private static boolean hasStaleSnapshot(GradingProposal proposal, Evaluation evaluation) {
        return evaluation.state == EvaluationStatus.SUPERSEDED || !matchesEvaluationSnapshot(proposal, evaluation);
    }

    private static boolean canDispatchProposal(GradingProposal proposal) {
        return "PENDING".equals(proposal.status) && proposal.taskId == null;
    }

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

    /** Attaches the task in the same database transaction as its enqueue request. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GradingProposal dispatchPreparedProposal(UUID proposalId, Supplier<AiTaskView> submitTask) {
        var evaluationId = gradingProposalRepository.findEvaluationId(proposalId).orElseThrow();
        var evaluation = evaluationRepository.lockById(evaluationId).orElseThrow();
        var proposal = gradingProposalRepository.findById(proposalId).orElseThrow();
        if (!canDispatchProposal(proposal)) {
            return proposal;
        }
        if (hasStaleSnapshot(proposal, evaluation)) {
            markProposalStale(proposal);
        } else {
            GradingProposalCallbackSupport.applyDispatchState(
                    proposal, submitTask.get(), null, objectMapper, clock);
        }
        return gradingProposalRepository.saveProposal(proposal);
    }

    /** Records polling reconciliation or a safe failure after dispatch rolled back. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GradingProposal recordDispatch(UUID proposalId, AiTaskView task, String error) {
        var evaluationId = gradingProposalRepository.findEvaluationId(proposalId).orElseThrow();
        var evaluation = evaluationRepository.lockById(evaluationId).orElseThrow();
        var proposal = gradingProposalRepository.findById(proposalId).orElseThrow();
        if (task == null) {
            if (!canDispatchProposal(proposal)) {
                return proposal;
            }
            if (hasStaleSnapshot(proposal, evaluation)) {
                markProposalStale(proposal);
                return gradingProposalRepository.saveProposal(proposal);
            }
        }
        GradingProposalCallbackSupport.applyDispatchState(
                proposal, task, error, objectMapper, clock);
        return gradingProposalRepository.saveProposal(proposal);
    }

    /** Validates and stores a completed AI proposal, returning its application outcome. */
    @Transactional
    public ApplyOutcome acceptProposal(AiTaskResult result) {
        UUID evaluationId = GradingProposalCallbackSupport.parseEvaluationTarget(
                result.targetRef(), result.taskType());
        if (evaluationId == null || result.actor() == null) {
            return ApplyOutcome.REJECTED;
        }
        var found = evaluationRepository.lockById(evaluationId);
        if (found.isEmpty()) {
            return ApplyOutcome.REJECTED;
        }
        var evaluation = found.get();
        var match = gradingProposalRepository.findForEvaluation(evaluationId).stream()
                .filter(p -> p.versionNo == result.inputVersion()).findFirst();
        if (match.isEmpty()) {
            return ApplyOutcome.REJECTED;
        }
        var proposal = match.get();
        ApplyOutcome precheck = precheckProposalCallback(result, evaluation, proposal);
        if (precheck != null) {
            return precheck;
        }
        return validateAndStoreCallbackPayload(result, evaluation, proposal);
    }

    private ApplyOutcome precheckProposalCallback(AiTaskResult result, Evaluation evaluation,
            GradingProposal proposal) {
        if (proposal.taskId != null && !proposal.taskId.equals(result.taskId())) {
            return ApplyOutcome.REJECTED;
        }
        if (!proposal.requesterId.equals(result.actor().accountId())) {
            if ("PENDING".equals(proposal.status)) {
                proposal.status = "REJECTED";
                proposal.taskId = result.taskId();
                proposal.taskState = null;
                proposal.safeError = "AI proposal cannot be applied to this submission";
                proposal.completedAt = clock.instant();
                gradingProposalRepository.saveProposal(proposal);
            }
            return ApplyOutcome.REJECTED;
        }
        if (!"PENDING".equals(proposal.status)) {
            return "READY".equals(proposal.status) ? ApplyOutcome.APPLIED
                    : "STALE".equals(proposal.status) ? ApplyOutcome.STALE : ApplyOutcome.REJECTED;
        }
        if (!proposal.submissionHash.equals(evaluation.submissionHash)
                || !proposal.rubricFingerprint.equals(evaluation.rubricSnapshot)
                || (evaluation.state != EvaluationStatus.PENDING
                        && evaluation.state != EvaluationStatus.DRAFT
                        && evaluation.state != EvaluationStatus.FINALIZED
                        && evaluation.state != EvaluationStatus.PUBLISHED)) {
            proposal.status = "STALE";
            proposal.taskId = result.taskId();
            proposal.taskState = null;
            proposal.completedAt = clock.instant();
            gradingProposalRepository.saveProposal(proposal);
            return ApplyOutcome.STALE;
        }
        return null;
    }

    private ApplyOutcome validateAndStoreCallbackPayload(AiTaskResult result,
            Evaluation evaluation, GradingProposal proposal) {
        try {
            gradingAccess.requireTeacher(result.actor(), evaluation.classId, Actions.GRADING_WRITE);
            gradingService.requireCurrent(result.actor(), evaluation.id, evaluation.version);
            var rubrics = GradingJson.readRubrics(objectMapper, evaluation.rubricSnapshot);
            var document = loadProposalDocument(result.actor(), evaluation);
            var validBlockIds = GradingFeedbackValidator.listValidBlockIds(rubrics, document);
            var content = assignmentQuery.getContent(evaluation.assignmentId).orElse(null);
            var payload = gradingProposalValidator.parseProposal(result.output());
            if (content != null && gradingPromptBuilder != null) {
                var emptyRubrics = gradingPromptBuilder.findEmptyRubrics(document, content.skeleton(), rubrics);
                if (emptyRubrics != null && !emptyRubrics.isEmpty()) {
                    payload = GradingPromptBuilder.mergeProposalWithEmptyRubrics(
                            payload, rubrics, emptyRubrics, objectMapper);
                }
            }
            payload = GradingPromptBuilder.sanitizeAndNormalizeProposal(
                    payload, rubrics, validBlockIds, objectMapper);
            if (content != null) {
                payload = gradingProposalValidator.flagProposalEvidenceForReview(
                        payload, rubrics, document, content.parts());
            }
            gradingProposalValidator.validateProposal(payload, rubrics, validBlockIds);
            proposal.proposal = payload;
            proposal.status = "READY";
            proposal.taskId = result.taskId();
            proposal.taskState = null;
            proposal.completedAt = clock.instant();
            gradingProposalRepository.saveProposal(proposal);
            return ApplyOutcome.APPLIED;
        } catch (ApiException ex) {
            if (ex.readStatus().is5xxServerError()) {
                throw ex;
            }
            GradingProposalCallbackSupport.applyRejectionFromException(
                    proposal, result.taskId(), ex, clock);
            gradingProposalRepository.saveProposal(proposal);
            return "STALE".equals(proposal.status) ? ApplyOutcome.STALE : ApplyOutcome.REJECTED;
        }
    }

    private DocumentModel loadProposalDocument(Actor actor, Evaluation evaluation) {
        if (submissionReviewService == null) {
            return null;
        }
        var detail = submissionReviewService.loadEvaluation(actor, evaluation.id);
        return detail == null ? null : detail.document();
    }

    /** Marks proposals stale when their evaluation snapshot has changed. */
    @Transactional
    public void invalidateChangedSnapshot(UUID evaluationId) {
        var evaluation = evaluationRepository.lockById(evaluationId)
                .orElseThrow(() -> ApiException.createNotFound("GRADE_NOT_FOUND"));
        for (var proposal : gradingProposalRepository.findForEvaluation(evaluationId)) {
            if (Set.of("PENDING", "READY").contains(proposal.status) && hasStaleSnapshot(proposal, evaluation)) {
                markProposalStale(proposal);
                gradingProposalRepository.saveProposal(proposal);
            }
        }
    }

    private void markProposalStale(GradingProposal proposal) {
        proposal.status = "STALE";
        proposal.taskState = null;
        proposal.completedAt = clock.instant();
    }

    /** Records an AI task failure against its matching pending proposal. */
    @Transactional
    public void recordFailure(AiTaskFailure failure) {
        UUID evaluationId = GradingProposalCallbackSupport.parseEvaluationTarget(
                failure.targetRef(), failure.taskType());
        if (evaluationId == null || evaluationRepository.lockById(evaluationId).isEmpty()) {
            return;
        }
        for (var proposal : gradingProposalRepository.findForEvaluation(evaluationId)) {
            if (GradingProposalCallbackSupport.matchesFailureTarget(proposal, failure)) {
                GradingProposalCallbackSupport.applyFailure(proposal, failure, clock);
                gradingProposalRepository.saveProposal(proposal);
            }
        }
    }

    static boolean isLegacyFailure(GradingProposal proposal) {
        return GradingProposalCallbackSupport.isLegacyFailure(proposal);
    }

    /** Saves a grade using its ready proposal and marks that proposal as used. */
    @Transactional
    public Evaluation saveUsingProposal(Actor actor, UUID evaluationId, Save input) {
        var evaluation = gradingService.requireCurrent(actor, evaluationId, input.version());
        var proposal = gradingProposalRepository.findById(input.usedProposalId())
                .orElseThrow(() -> ApiException.createNotFound("PROPOSAL_NOT_FOUND"));
        if (!evaluationId.equals(proposal.evaluationId) || !"READY".equals(proposal.status)
                || !evaluation.submissionHash.equals(proposal.submissionHash)
                || !evaluation.rubricSnapshot.equals(proposal.rubricFingerprint)) {
            throw ApiException.createConflict("STALE_PROPOSAL", "Proposal is stale or unavailable");
        }
        var saved = gradingService.saveCheckedEvaluation(actor, evaluationId, input, proposal);
        proposal.usedAt = clock.instant();
        gradingProposalRepository.saveProposal(proposal);
        return saved;
    }

    /** Validates and stores an MCP proposal for teacher review. */
    @Transactional
    public GradingProposal recordMcpProposal(Actor actor, UUID evaluationId, long expectedVersion,
            String hash, String requestRef, JsonNode payload, String agentLabel) {
        var evaluation = gradingService.requireCurrent(actor, evaluationId, expectedVersion);
        if (!evaluation.submissionHash.equals(hash)) {
            throw ApiException.createConflict("STALE_PROPOSAL", "Submission changed");
        }
        requireGradableTarget(evaluation, "AI proposals require a pending or draft grade");
        var existing = gradingProposalRepository.findByRequestRef(requestRef).orElse(null);
        if (existing != null) {
            requireSameMcpProposalTarget(actor, evaluation, existing);
            if (Objects.equals(existing.proposal, payload)) {
                return existing;
            }
        }
        var rubrics = GradingJson.readRubrics(objectMapper, evaluation.rubricSnapshot);
        var document = loadProposalDocument(actor, evaluation);
        var validBlockIds = GradingFeedbackValidator.listValidBlockIds(rubrics, document);
        if (existing == null) {
            gradingProposalValidator.validateProposal(payload, rubrics, validBlockIds);
        }
        var content = assignmentQuery == null || document == null ? null
                : assignmentQuery.getContent(evaluation.assignmentId).orElse(null);
        var reviewed = content == null ? payload
                : gradingProposalValidator.flagProposalEvidenceForReview(
                        payload, rubrics, document, content.parts());
        if (existing != null) {
            if (!Objects.equals(existing.proposal, reviewed)) {
                throw ApiException.createConflict("REQUEST_REF_CONFLICT", "Request reference already used");
            }
            try {
                gradingProposalValidator.validateProposal(payload, rubrics, validBlockIds);
            } catch (ApiException ex) {
                throw ApiException.createConflict("REQUEST_REF_CONFLICT", "Request reference already used");
            }
            return existing;
        }
        var proposal = buildMcpProposal(actor, evaluationId, hash, requestRef, reviewed,
                agentLabel, evaluation);
        return gradingProposalRepository.saveProposal(proposal);
    }

    private void requireSameMcpProposalTarget(Actor actor, Evaluation evaluation, GradingProposal proposal) {
        if (!Objects.equals(proposal.rubricFingerprint, evaluation.rubricSnapshot)) {
            throw ApiException.createConflict("STALE_PROPOSAL", "Rubric snapshot changed");
        }
        if (!evaluation.id.equals(proposal.evaluationId)
                || !proposal.requesterId.equals(actor.accountId())
                || !Objects.equals(proposal.submissionHash, evaluation.submissionHash)) {
            throw ApiException.createConflict("REQUEST_REF_CONFLICT", "Request reference already used");
        }
    }

    private GradingProposal buildMcpProposal(Actor actor, UUID evaluationId, String hash,
            String requestRef, JsonNode payload, String agentLabel, Evaluation evaluation) {
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
        return proposal;
    }
}
