package edu.aiplatform.grading.service;

import edu.aiplatform.grading.model.RubricScoring;
import edu.aiplatform.grading.contract.IGroupSubmissionSource;
import edu.aiplatform.grading.contract.GroupSubmissionView;
import org.springframework.beans.factory.ObjectProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.RubricView;
import edu.aiplatform.assignmentwork.contract.ISubmissionQuery;
import edu.aiplatform.assignmentwork.contract.SubmissionView;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.grading.service.GradingCommands.ItemVersion;
import edu.aiplatform.grading.service.GradingCommands.MemberSave;
import edu.aiplatform.grading.service.GradingCommands.Save;
import edu.aiplatform.grading.model.Evaluation;
import edu.aiplatform.grading.model.EvaluationKind;
import edu.aiplatform.grading.model.EvaluationStatus;
import edu.aiplatform.grading.model.GradingFeedback;
import edu.aiplatform.grading.model.GradingProposal;
import edu.aiplatform.grading.repository.IEvaluationRepository;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.jobs.contract.DomainEvent;
import edu.aiplatform.jobs.contract.EventTypes;
import edu.aiplatform.jobs.contract.IEventPublisher;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GradingService {
    private final IEvaluationRepository evaluationRepository;
    private final GradingAccess gradingAccess;
    private final IAudit audit;
    private final IEventPublisher eventPublisher;
    private final ISubmissionQuery submissionQuery;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final GradingFeedbackValidator gradingFeedbackValidator;
    private final ObjectProvider<IGroupSubmissionSource> groupSubmissionSourceProvider;

    /** Creates the grading service with evaluation, authorization, and audit dependencies. */
    public GradingService(IEvaluationRepository evaluationRepository, GradingAccess gradingAccess,
            IAudit audit, IEventPublisher eventPublisher, ISubmissionQuery submissionQuery,
            ObjectMapper objectMapper, Clock clock,
            ObjectProvider<IGroupSubmissionSource> groupSubmissionSourceProvider) {
        this.evaluationRepository = evaluationRepository;
        this.gradingAccess = gradingAccess;
        this.audit = audit;
        this.eventPublisher = eventPublisher;
        this.submissionQuery = submissionQuery;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.groupSubmissionSourceProvider = groupSubmissionSourceProvider;
        this.gradingFeedbackValidator = new GradingFeedbackValidator(objectMapper);
    }

    /**
     * Validates teacher rights, locks the evaluation, checks version concurrency, and verifies attempt recency.
     */
    public Evaluation requireCurrent(Actor actor, UUID id, long version) {
        var evaluation = evaluationRepository.lockById(id)
                .orElseThrow(() -> ApiException.notFound("GRADE_NOT_FOUND"));
        gradingAccess.requireTeacher(actor, evaluation.classId, Actions.GRADING_WRITE);
        evaluation.checkVersion(version);
        evaluation.requireEditableGrade();
        if (evaluation.kind == EvaluationKind.ATTEMPT) {
            requireLatestSubmission(evaluation);
        } else if (evaluation.kind == EvaluationKind.GROUP_DOCUMENT) {
            requireGroupSubmission(evaluation);
        }
        return evaluation;
    }

    private GroupSubmissionView requireGroupSubmission(Evaluation evaluation) {
        var source = groupSubmissionSourceProvider.getIfAvailable();
        if (source == null)
            throw ApiException.unavailable("GROUP_SUBMISSIONS_UNAVAILABLE", "Group submission source unavailable");
        var submission = source.getGroupSubmission(evaluation.groupDocumentId).orElseThrow(() ->
                ApiException.unavailable("SUBMISSION_UNAVAILABLE", "Group submission unavailable"));
        if (!Objects.equals(evaluation.groupDocumentId, submission.groupDocumentId())
                || !Objects.equals(evaluation.submissionHash, submission.submissionHash())
                || !evaluation.assignmentId.equals(submission.assignmentId())
                || !evaluation.classId.equals(submission.classId()))
            throw ApiException.conflict("SUBMISSION_CHANGED", "Group submission snapshot changed");
        return submission;
    }

    /**
     * Verifies submission snapshot consistency and ensures only the latest student attempt is evaluated.
     */
    private SubmissionView requireLatestSubmission(Evaluation evaluation) {
        var submission = submissionQuery.getSubmission(evaluation.attemptId).orElseThrow(
                () -> ApiException.unavailable("SUBMISSION_UNAVAILABLE", "Submission unavailable"));
        if (!evaluation.submissionHash.equals(submission.receiptHash())
                || !evaluation.assignmentId.equals(submission.assignmentId())
                || !evaluation.classId.equals(submission.classId()))
            throw ApiException.conflict("SUBMISSION_CHANGED", "Submission snapshot changed");
        boolean latest = submissionQuery
                .listSubmissions(evaluation.assignmentId, evaluation.classId).stream()
                .anyMatch(row -> evaluation.accountId.equals(row.accountId())
                        && evaluation.attemptId.equals(row.gradedAttemptId()));
        if (!latest)
            throw ApiException.conflict("NOT_LATEST_ATTEMPT",
                    "Only the latest submitted attempt can be graded");
        return submission;
    }

    /**
     * Saves manual grades and rubric scores for an evaluation without applying an AI proposal.
     */
    @Transactional
    public Evaluation saveEvaluation(Actor actor, UUID id, Save input) {
        if (input.usedProposalId() != null)
            throw ApiException.badRequest("PROPOSAL_REQUIRES_VALIDATION",
                    "Apply proposal through proposal review endpoint");
        return saveCheckedEvaluation(actor, id, input, null);
    }

    /**
     * Internal overload saving evaluation without an AI proposal baseline.
     */
    Evaluation saveCheckedEvaluation(Actor actor, UUID id, Save input) {
        return saveCheckedEvaluation(actor, id, input, null);
    }

    /**
     * Core grading logic updating rubric scores, adjustments, structured feedback, and propagating to group members.
     */
    Evaluation saveCheckedEvaluation(Actor actor, UUID id, Save input,
            GradingProposal usedProposal) {
        // Step 1: Validate teacher rights, lock evaluation, verify version concurrency, and ensure grade is not locked
        var evaluation = requireCurrent(actor, id, input.version());
        evaluation.requireMutableGrade();
        if (evaluation.kind == EvaluationKind.MEMBER)
            throw ApiException.badRequest("MEMBER_ENDPOINT_REQUIRED",
                    "Use member grading endpoint");

        // Step 2: Compute rubric score points and compare current vs previous effective item scores
        var rubrics = GradingJson.readRubrics(objectMapper, evaluation.rubricSnapshot);
        var points = RubricScoring.calculateScore(rubrics, input.checkedItemsByRubric(),
                input.itemScoresByRubric());
        var itemScores = RubricScoring.copyItemScores(input.itemScoresByRubric());
        var currentEffectiveScores = RubricScoring.calculateEffectiveItemScores(rubrics,
                input.checkedItemsByRubric(), itemScores);
        var previousEffectiveScores = evaluation.score == null
                ? Map.<UUID, Map<UUID, BigDecimal>>of()
                : RubricScoring.calculateEffectiveItemScores(rubrics,
                        GradingJson.readChecks(objectMapper, evaluation.rubricChecks),
                        evaluation.rubricItemScores);

        // Step 3: Collect valid document block IDs to prevent ghost evidence citations
        DocumentModel submittedDocument = null;
        if (evaluation.kind == EvaluationKind.ATTEMPT && evaluation.attemptId != null) {
            submittedDocument = submissionQuery.getSubmission(evaluation.attemptId)
                    .map(SubmissionView::content).orElse(null);
        }
        if (evaluation.kind == EvaluationKind.GROUP_DOCUMENT) {
            submittedDocument = requireGroupSubmission(evaluation).content();
        }
        Set<String> validBlockIds = GradingFeedbackValidator.listValidBlockIds(rubrics,
                submittedDocument);

        // Step 4: Resolve and validate feedback state, ensuring modifications to AI scores have acknowledged reviews
        var feedbackState = resolveFeedbackState(evaluation, input, usedProposal, rubrics,
                validBlockIds, currentEffectiveScores, previousEffectiveScores);
        if (evaluation.state == EvaluationStatus.PUBLISHED) {
            requireStructuredFeedbackReviewed(feedbackState.feedback(), feedbackState.reviewedScores(),
                    feedbackState.acknowledgedItems(), currentEffectiveScores);
        }

        // Keep published corrections visible; unpublished evaluations remain drafts.
        evaluation.saveGrade(GradingJson.writeSnapshot(objectMapper,
                        RubricScoring.synchronizeChecks(rubrics, currentEffectiveScores)),
                points, input.adjustment(), input.adjustmentReason(), input.feedback(),
                input.changeReason(), actor.accountId(), clock.instant());
        evaluation.rubricItemScores = itemScores;
        evaluation.structuredFeedback = feedbackState.feedback();
        evaluation.reviewedItemScores = feedbackState.reviewedScores();
        evaluation.acknowledgedReviewItems = feedbackState.acknowledgedItems();

        // Step 6: Propagate grade to all group members (if group submission), persist to DB, and record audit event
        propagateGroupGrade(evaluation, actor, GroupOperation.SAVE, input.changeReason());
        evaluationRepository.save(evaluation);
        recordAudit(actor, evaluation, "GRADE_SAVED", null);
        return evaluation;
    }

    /**
     * Validates a saved grade for legacy finalize callers without locking it.
     */
    @Transactional
    public Evaluation finalizeEvaluation(Actor actor, ItemVersion item) {
        // Step 1: Lock evaluation, verify teacher permission, and ensure it is not a group member sub-record
        var evaluation = requireCurrent(actor, item.evaluationId(), item.version());
        if (evaluation.kind == EvaluationKind.MEMBER)
            throw ApiException.badRequest("GROUP_ROOT_REQUIRED", "Finalize the group document");

        requireEvaluationReviewed(evaluation);

        // Legacy validation must not introduce a new locked state.
        evaluation.finalizeGrade(actor.accountId(), clock.instant());
        evaluationRepository.save(evaluation);
        recordAudit(actor, evaluation, "GRADE_REVIEWED", null);
        return evaluation;
    }

    /**
     * Publishes a saved evaluation once and emits the GRADE_PUBLISHED domain event.
     */
    @Transactional
    public Evaluation publishEvaluation(Actor actor, ItemVersion item) {
        // Step 1: Lock evaluation and ensure publication is performed on root evaluation
        var evaluation = requireCurrent(actor, item.evaluationId(), item.version());
        if (evaluation.kind == EvaluationKind.MEMBER)
            throw ApiException.badRequest("GROUP_ROOT_REQUIRED", "Publish the group document");

        if (evaluation.state == EvaluationStatus.PUBLISHED) return evaluation;
        requireEvaluationReviewed(evaluation);

        // Step 2: Transition state to PUBLISHED and propagate to group members
        evaluation.publishGrade(actor.accountId(), clock.instant());
        var recipients = propagateGroupGrade(evaluation, actor, GroupOperation.PUBLISH, null);
        if (evaluation.kind == EvaluationKind.ATTEMPT)
            recipients = List.of(evaluation.accountId);
        evaluationRepository.save(evaluation);
        recordAudit(actor, evaluation, "GRADE_PUBLISHED", null);

        // Step 3: Broadcast domain event to notify students
        eventPublisher.publish(new DomainEvent(1, UUID.randomUUID(), EventTypes.GRADE_PUBLISHED,
                "U09", clock.instant(), evaluation.id.toString(),
                Map.of("assignmentId", evaluation.assignmentId.toString(), "classId",
                        evaluation.classId.toString(), "studentIds",
                        recipients.stream().map(UUID::toString).toList())));
        return evaluation;
    }

    /**
     * Overrides an individual member's score in a group document submission with a recorded reason.
     */
    @Transactional
    public Evaluation saveMemberGrade(Actor actor, UUID rootId, UUID accountId, MemberSave input) {
        // Step 1: Lock group root evaluation and assert it has already been graded
        var root = evaluationRepository.findById(rootId)
                .orElseThrow(() -> ApiException.notFound("GRADE_NOT_FOUND"));
        gradingAccess.requireTeacher(actor, root.classId, Actions.GRADING_WRITE);
        root = evaluationRepository.lockById(rootId).orElseThrow();
        root.requireMutableGrade();
        if (root.kind != EvaluationKind.GROUP_DOCUMENT || root.score == null)
            throw ApiException.conflict("GROUP_NOT_GRADED", "Grade group document first");
        requireGroupSubmission(root);
        if (root.state == EvaluationStatus.PUBLISHED) requireEvaluationReviewed(root);

        // Step 2: Locate matching member evaluation record and check version concurrency
        final UUID documentId = root.groupDocumentId;
        final String hash = root.submissionHash;
        var candidate = evaluationRepository
                .findForAssignmentAndClass(root.assignmentId, root.classId).stream()
                .filter(evaluation -> evaluation.kind == EvaluationKind.MEMBER
                        && documentId.equals(evaluation.groupDocumentId)
                        && hash.equals(evaluation.submissionHash)
                        && accountId.equals(evaluation.accountId)
                        && evaluation.state != EvaluationStatus.SUPERSEDED)
                .findFirst().orElseThrow(() -> ApiException.notFound("GRADE_NOT_FOUND"));
        var member = evaluationRepository.lockById(candidate.id).orElseThrow();
        member.checkVersion(input.version());

        // Step 3: Set overridden member score, save to DB, and record audit event
        var groupRubricScore = RubricScoring.calculateScore(
                GradingJson.readRubrics(objectMapper, root.rubricSnapshot),
                GradingJson.readChecks(objectMapper, root.rubricChecks), root.rubricItemScores);
        var previousLevel = member.contributionLevel;
        member.overrideMemberScore(input.score(), groupRubricScore, input.reason(), actor.accountId(),
                clock.instant());
        member.contributionLevel = input.contributionLevel();
        evaluationRepository.save(member);
        audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(), "GRADE_CHANGED", "EVALUATION",
                member.id.toString(), AuditResult.SUCCESS, input.reason(),
                Map.of("classId", member.classId.toString(),
                        "previousContributionLevel", previousLevel == null ? "UNASSESSED" : previousLevel.name(),
                        "contributionLevel", member.contributionLevel == null
                                ? "UNASSESSED" : member.contributionLevel.name()), clock.instant()));
        return member;
    }

    private record ResolvedFeedbackState(GradingFeedback feedback,
            Map<UUID, Map<UUID, BigDecimal>> reviewedScores,
            Map<UUID, Set<UUID>> acknowledgedItems) {
    }

    /**
     * Resolves and validates structured feedback state, accounting for clear requests and AI proposal baselines.
     */
    private ResolvedFeedbackState resolveFeedbackState(Evaluation evaluation, Save input,
            GradingProposal usedProposal, List<RubricView> rubrics, Set<String> validBlockIds,
            Map<UUID, Map<UUID, BigDecimal>> currentEffectiveScores,
            Map<UUID, Map<UUID, BigDecimal>> previousEffectiveScores) {
        if (Boolean.TRUE.equals(input.clearStructuredFeedback())) {
            if (input.structuredFeedback() != null) {
                throw ApiException.badRequest("INVALID_STRUCTURED_FEEDBACK",
                        "Cannot provide structuredFeedback when clearStructuredFeedback is true");
            }
            return new ResolvedFeedbackState(null, Map.of(), Map.of());
        }

        if (input.structuredFeedback() == null) {
            if (evaluation.structuredFeedback == null) {
                validateMetadataEmptyOrKnown(input.acknowledgedReviewItems(),
                        input.reviewedItemScores(), null, currentEffectiveScores);
                return new ResolvedFeedbackState(null, Map.of(), Map.of());
            }
            var retained = evaluation.structuredFeedback.copyFeedback();
            validateMetadataEmptyOrKnown(input.acknowledgedReviewItems(),
                    input.reviewedItemScores(), retained, currentEffectiveScores);
            return computeReviewMetadata(evaluation, retained, retained, input,
                    previousEffectiveScores, currentEffectiveScores, false);
        }

        var validated = gradingFeedbackValidator.validateFeedback(input.structuredFeedback(),
                rubrics, validBlockIds, false);
        validateMetadataEmptyOrKnown(input.acknowledgedReviewItems(), input.reviewedItemScores(),
                validated, currentEffectiveScores);

        GradingFeedback baselineFeedback = evaluation.structuredFeedback;
        Map<UUID, Map<UUID, BigDecimal>> baselineScores = previousEffectiveScores;
        boolean fromProposal = false;
        if (usedProposal != null && usedProposal.proposal != null) {
            fromProposal = true;
            baselineScores = extractProposalEffectiveScores(usedProposal.proposal, rubrics);
            if (usedProposal.proposal.hasNonNull("structuredFeedback")) {
                baselineFeedback = gradingFeedbackValidator.validateNode(
                        usedProposal.proposal.get("structuredFeedback"), rubrics, validBlockIds,
                        false);
            }
        }

        return computeReviewMetadata(evaluation, validated, baselineFeedback, input, baselineScores,
                currentEffectiveScores, fromProposal);
    }

    /**
     * Calculates reviewed scores and reviewer acknowledgment tracking by comparing against baseline scores/text.
     */
    private ResolvedFeedbackState computeReviewMetadata(Evaluation evaluation,
            GradingFeedback target, GradingFeedback baselineFeedback, Save input,
            Map<UUID, Map<UUID, BigDecimal>> baselineScores,
            Map<UUID, Map<UUID, BigDecimal>> currentEffectiveScores, boolean fromProposal) {
        Map<UUID, GradingFeedback.Item> baselineItems = new HashMap<>();
        if (baselineFeedback != null && baselineFeedback.items() != null) {
            for (var item : baselineFeedback.items()) {
                baselineItems.put(item.itemId(), item);
            }
        }

        var nextReviewed = new LinkedHashMap<UUID, Map<UUID, BigDecimal>>();
        var nextAck = new LinkedHashMap<UUID, Set<UUID>>();

        for (var item : target.items()) {
            UUID r = item.rubricId();
            UUID i = item.itemId();
            BigDecimal currentScore = currentEffectiveScores.getOrDefault(r, Map.of())
                    .getOrDefault(i, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            boolean inputAck = input.acknowledgedReviewItems() != null
                    && input.acknowledgedReviewItems().getOrDefault(r, Set.of()).contains(i);
            boolean previouslyAck = !fromProposal && evaluation.acknowledgedReviewItems != null
                    && evaluation.acknowledgedReviewItems.getOrDefault(r, Set.of()).contains(i);

            var baseItem = baselineItems.get(i);
            if (baseItem != null && baseItem.requiresReview() && !previouslyAck
                    && !item.requiresReview() && !inputAck) {
                throw ApiException.badRequest("UNACKNOWLEDGED_REVIEW_FLAG",
                        "Cần xác nhận rõ ràng trước khi gỡ cờ kiểm tra của AI.");
            }

            BigDecimal baseScore = baselineScores == null ? null
                    : baselineScores.getOrDefault(r, Map.of()).get(i);
            BigDecimal prevReviewedScore = !fromProposal && evaluation.reviewedItemScores != null
                    ? evaluation.reviewedItemScores.getOrDefault(r, Map.of()).get(i)
                    : baseScore;
            boolean editedText = baseItem == null
                    || !Objects.equals(baseItem.evidence(), item.evidence())
                    || !Objects.equals(baseItem.reason(), item.reason())
                    || !Objects.equals(baseItem.suggestion(), item.suggestion());
            boolean scoreChangedFromBaseline = baseScore != null
                    && currentScore.compareTo(baseScore) != 0;

            BigDecimal suppliedReviewedScore = input.reviewedItemScores() != null
                    && input.reviewedItemScores().getOrDefault(r, Map.of()).containsKey(i)
                            ? input.reviewedItemScores().get(r).get(i).setScale(2,
                                    RoundingMode.HALF_UP)
                            : null;

            if (inputAck && suppliedReviewedScore != null
                    && suppliedReviewedScore.compareTo(currentScore) != 0) {
                throw ApiException.badRequest("INVALID_REVIEW_METADATA",
                        "Điểm xác nhận giải thích không khớp với điểm mục rubric đang lưu.");
            }

            BigDecimal resolvedReviewedScore;
            boolean resolvedAck;
            if (inputAck) {
                resolvedReviewedScore = currentScore;
                resolvedAck = true;
            } else if (editedText && baseItem != null) {
                resolvedReviewedScore = currentScore;
                resolvedAck = previouslyAck && !scoreChangedFromBaseline;
            } else if (scoreChangedFromBaseline) {
                if (suppliedReviewedScore != null
                        && suppliedReviewedScore.compareTo(currentScore) == 0) {
                    throw ApiException.badRequest("INVALID_REVIEW_METADATA",
                            "Điểm mục rubric đã thay đổi; cần chỉnh sửa hoặc xác nhận lại giải thích.");
                }
                resolvedReviewedScore = prevReviewedScore != null ? prevReviewedScore : baseScore;
                resolvedAck = false;
            } else if (suppliedReviewedScore != null) {
                if (prevReviewedScore != null && prevReviewedScore.compareTo(currentScore) != 0
                        && suppliedReviewedScore.compareTo(currentScore) == 0) {
                    throw ApiException.badRequest("INVALID_REVIEW_METADATA",
                            "Không thể xóa trạng thái cần xem lại giải thích mà không xác nhận hoặc chỉnh sửa.");
                }
                resolvedReviewedScore = suppliedReviewedScore;
                resolvedAck = previouslyAck && input.acknowledgedReviewItems() == null;
            } else {
                resolvedReviewedScore = prevReviewedScore != null ? prevReviewedScore
                        : currentScore;
                resolvedAck = previouslyAck && input.acknowledgedReviewItems() == null;
            }

            nextReviewed.computeIfAbsent(r, k -> new LinkedHashMap<>()).put(i,
                    resolvedReviewedScore);
            if (resolvedAck) {
                nextAck.computeIfAbsent(r, k -> new HashSet<>()).add(i);
            }
        }

        return new ResolvedFeedbackState(target, RubricScoring.copyItemScores(nextReviewed),
                RubricScoring.copyChecks(nextAck));
    }

    /**
     * Validates that all acknowledged and reviewed item references belong to known feedback and rubric items.
     */
    private void validateMetadataEmptyOrKnown(Map<UUID, Set<UUID>> acknowledged,
            Map<UUID, Map<UUID, BigDecimal>> reviewedScores, GradingFeedback feedback,
            Map<UUID, Map<UUID, BigDecimal>> currentEffectiveScores) {
        Map<UUID, Set<UUID>> knownFeedbackItems = new HashMap<>();
        if (feedback != null && feedback.items() != null) {
            for (var item : feedback.items()) {
                knownFeedbackItems.computeIfAbsent(item.rubricId(), k -> new HashSet<>())
                        .add(item.itemId());
            }
        }
        if (acknowledged != null) {
            for (var entry : acknowledged.entrySet()) {
                UUID r = entry.getKey();
                Set<UUID> items = entry.getValue();
                if (r == null || items == null || !knownFeedbackItems.containsKey(r)
                        || !knownFeedbackItems.get(r).containsAll(items)) {
                    throw ApiException.badRequest("INVALID_REVIEW_METADATA",
                            "Unknown rubric or item in acknowledgedReviewItems");
                }
            }
        }
        if (reviewedScores != null) {
            for (var entry : reviewedScores.entrySet()) {
                UUID r = entry.getKey();
                var map = entry.getValue();
                if (r == null || map == null || !knownFeedbackItems.containsKey(r)
                        || !knownFeedbackItems.get(r).containsAll(map.keySet())) {
                    throw ApiException.badRequest("INVALID_REVIEW_METADATA",
                            "Unknown rubric or item in reviewedItemScores");
                }
                for (var itemEntry : map.entrySet()) {
                    BigDecimal val = itemEntry.getValue();
                    if (val == null || val.scale() > 2 || val.signum() < 0
                            || !currentEffectiveScores.getOrDefault(r, Map.of())
                                    .containsKey(itemEntry.getKey())) {
                        throw ApiException.badRequest("INVALID_REVIEW_METADATA",
                                "Invalid reviewedScore value");
                    }
                }
            }
        }
    }

    /**
     * Asserts that all feedback items flagged for manual review or affected by score changes have been reviewed.
     */
    private static void requireStructuredFeedbackReviewed(GradingFeedback feedback,
            Map<UUID, Map<UUID, BigDecimal>> reviewedScores, Map<UUID, Set<UUID>> acknowledgedItems,
            Map<UUID, Map<UUID, BigDecimal>> currentEffectiveScores) {
        if (feedback == null || feedback.items() == null) {
            return;
        }
        for (var item : feedback.items()) {
            UUID r = item.rubricId();
            UUID i = item.itemId();
            boolean ack = acknowledgedItems != null
            && acknowledgedItems.getOrDefault(r, Set.of()).contains(i);
            if (item.requiresReview() && !ack) {
                throw ApiException.conflict("STRUCTURED_FEEDBACK_REVIEW_REQUIRED",
                        "Cần xác nhận hoặc chỉnh sửa các mục nhận xét cần kiểm tra trước khi công bố hoặc lưu điểm đã công bố.");
            }
            BigDecimal currentScore = currentEffectiveScores.getOrDefault(r, Map.of()).get(i);
            BigDecimal reviewedScore = reviewedScores == null ? null
                    : reviewedScores.getOrDefault(r, Map.of()).get(i);
            if (currentScore != null
                    && (reviewedScore == null || currentScore.compareTo(reviewedScore) != 0)
                    && !ack) {
                throw ApiException.conflict("STRUCTURED_FEEDBACK_SCORE_MISMATCH",
                        "Điểm mục rubric đã thay đổi sau khi tạo nhận xét; vui lòng chỉnh sửa hoặc xác nhận lại giải thích trước khi công bố hoặc lưu điểm đã công bố.");
            }
        }
    }

    private void requireEvaluationReviewed(Evaluation evaluation) {
        var rubrics = GradingJson.readRubrics(objectMapper, evaluation.rubricSnapshot);
        requireStructuredFeedbackReviewed(evaluation.structuredFeedback,
                evaluation.reviewedItemScores, evaluation.acknowledgedReviewItems,
                RubricScoring.calculateEffectiveItemScores(rubrics,
                        GradingJson.readChecks(objectMapper, evaluation.rubricChecks),
                        evaluation.rubricItemScores));
    }

    /**
     * Extracts rubric check states and item scores from proposal JSON to calculate effective scores.
     */
    private Map<UUID, Map<UUID, BigDecimal>> extractProposalEffectiveScores(JsonNode proposalNode,
            List<RubricView> rubrics) {
        try {
            Map<UUID, Set<UUID>> checks = proposalNode.has("checks") ? objectMapper.convertValue(
                    proposalNode.get("checks"), new TypeReference<Map<UUID, Set<UUID>>>() {
                    }) : Map.of();
            Map<UUID, Map<UUID, BigDecimal>> itemScores = proposalNode.has("itemScoresByRubric")
                    ? objectMapper.convertValue(proposalNode.get("itemScoresByRubric"),
                            new TypeReference<Map<UUID, Map<UUID, BigDecimal>>>() {
                            })
                    : Map.of();
            return RubricScoring.calculateEffectiveItemScores(rubrics, checks, itemScores);
        } catch (RuntimeException ex) {
            return Map.of();
        }
    }

    private enum GroupOperation {
        SAVE, PUBLISH
    }

    /**
     * Propagates grade actions (save, finalize, publish) from a group root evaluation to each member's evaluation.
     */
    private List<UUID> propagateGroupGrade(Evaluation root, Actor actor, GroupOperation operation,
            String reason) {
        if (root.kind != EvaluationKind.GROUP_DOCUMENT)
            return List.of();
        List<UUID> recipients = new ArrayList<>();
        var members = evaluationRepository
                .findForAssignmentAndClass(root.assignmentId, root.classId).stream()
                .filter(member -> member.isCurrentMemberOf(root))
                .sorted(Comparator.comparing(evaluation -> evaluation.id)).toList();
        for (var candidate : members) {
            var member = evaluationRepository.lockById(candidate.id).orElseThrow();
            switch (operation) {
            case SAVE -> member.inheritGroupGrade(root, reason, actor.accountId(), clock.instant());
            case PUBLISH -> member.publishGrade(actor.accountId(), clock.instant());
            }
            evaluationRepository.save(member);
            recipients.add(member.accountId);
        }
        if (recipients.isEmpty())
            throw ApiException.conflict("GROUP_MEMBERS_UNAVAILABLE",
                    "Group member snapshot unavailable");
        return recipients;
    }

    /**
     * Logs an audit record for grading operations.
     */
    void recordAudit(Actor actor, Evaluation evaluation, String action, String reason) {
        audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(), action, "EVALUATION",
                evaluation.id.toString(), AuditResult.SUCCESS, reason,
                Map.of("classId", evaluation.classId.toString()), clock.instant()));
    }
}
