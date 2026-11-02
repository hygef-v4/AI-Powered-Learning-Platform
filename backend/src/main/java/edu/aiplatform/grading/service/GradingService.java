package edu.aiplatform.grading.service;

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
    private final IEvaluationRepository evaluations;
    private final GradingAccess access;
    private final IAudit audit;
    private final IEventPublisher events;
    private final ISubmissionQuery submissions;
    private final ObjectMapper json;
    private final Clock clock;
    private final GradingFeedbackValidator feedbackValidator;

    public GradingService(IEvaluationRepository evaluations, GradingAccess access, IAudit audit, IEventPublisher events,
            ISubmissionQuery submissions, ObjectMapper json, Clock clock) {
        this.evaluations = evaluations;
        this.access = access;
        this.audit = audit;
        this.events = events;
        this.submissions = submissions;
        this.json = json;
        this.clock = clock;
        this.feedbackValidator = new GradingFeedbackValidator(json);
    }

    public Evaluation requireCurrent(Actor actor, UUID id, long version) {
        var evaluation = evaluations.lockById(id).orElseThrow(() -> ApiException.notFound("GRADE_NOT_FOUND"));
        access.requireTeacher(actor, evaluation.classId, Actions.GRADING_WRITE);
        evaluation.checkVersion(version);
        evaluation.editable();
        if (evaluation.kind == EvaluationKind.ATTEMPT) {
            requireLatestSubmission(evaluation);
        }
        return evaluation;
    }

    private SubmissionView requireLatestSubmission(Evaluation evaluation) {
        var submission = submissions.getSubmission(evaluation.attemptId)
                .orElseThrow(() -> ApiException.unavailable("SUBMISSION_UNAVAILABLE", "Submission unavailable"));
        if (!evaluation.submissionHash.equals(submission.receiptHash())
                || !evaluation.assignmentId.equals(submission.assignmentId())
                || !evaluation.classId.equals(submission.classId()))
            throw ApiException.conflict("SUBMISSION_CHANGED", "Submission snapshot changed");
        boolean latest = submissions.listSubmissions(evaluation.assignmentId, evaluation.classId).stream()
                .anyMatch(row -> evaluation.accountId.equals(row.accountId())
                        && evaluation.attemptId.equals(row.gradedAttemptId()));
        if (!latest)
            throw ApiException.conflict("NOT_LATEST_ATTEMPT", "Only the latest submitted attempt can be graded");
        return submission;
    }

    @Transactional
    public Evaluation save(Actor actor, UUID id, Save input) {
        if (input.usedProposalId() != null)
            throw ApiException.badRequest("PROPOSAL_REQUIRES_VALIDATION",
                    "Apply proposal through proposal review endpoint");
        return saveChecked(actor, id, input, null);
    }

    Evaluation saveChecked(Actor actor, UUID id, Save input) {
        return saveChecked(actor, id, input, null);
    }

    Evaluation saveChecked(Actor actor, UUID id, Save input, GradingProposal usedProposal) {
        var evaluation = requireCurrent(actor, id, input.version());
        if (evaluation.kind == EvaluationKind.MEMBER)
            throw ApiException.badRequest("MEMBER_ENDPOINT_REQUIRED", "Use member grading endpoint");
        var rubrics = GradingJson.rubrics(json, evaluation.rubricSnapshot);
        var points = GradingJson.score(rubrics, input.checkedItemsByRubric(), input.itemScoresByRubric());
        var itemScores = GradingJson.copyItemScores(input.itemScoresByRubric());
        var currentEffectiveScores = GradingJson.effectiveItemScores(rubrics, input.checkedItemsByRubric(), itemScores);
        var previousEffectiveScores = evaluation.score == null
                ? Map.<UUID, Map<UUID, BigDecimal>>of()
                : GradingJson.effectiveItemScores(rubrics, GradingJson.readChecks(json, evaluation.rubricChecks),
                        evaluation.rubricItemScores);

        DocumentModel submittedDocument = null;
        if (evaluation.kind == EvaluationKind.ATTEMPT && evaluation.attemptId != null) {
            submittedDocument = submissions.getSubmission(evaluation.attemptId)
                    .map(SubmissionView::content)
                    .orElse(null);
        }
        Set<String> validBlockIds = GradingFeedbackValidator.validBlockIds(rubrics, submittedDocument);

        var feedbackState = resolveFeedbackState(evaluation, input, usedProposal, rubrics, validBlockIds,
                currentEffectiveScores, previousEffectiveScores);
        boolean changingOfficialGrade = evaluation.official();
        if (changingOfficialGrade && feedbackState.feedback() != null) {
            requireStructuredFeedbackReviewed(feedbackState.feedback(), feedbackState.reviewedScores(),
                    feedbackState.acknowledgedItems(), currentEffectiveScores);
        }

        evaluation.save(GradingJson.write(json, input.checkedItemsByRubric()), points, input.adjustment(),
                input.adjustmentReason(), input.feedback(), input.changeReason(), actor.accountId(), clock.instant());
        evaluation.rubricItemScores = itemScores;
        evaluation.structuredFeedback = feedbackState.feedback();
        evaluation.reviewedItemScores = feedbackState.reviewedScores();
        evaluation.acknowledgedReviewItems = feedbackState.acknowledgedItems();
        propagateGroup(evaluation, actor, GroupOperation.SAVE, input.changeReason());
        evaluations.save(evaluation);
        record(actor, evaluation, changingOfficialGrade ? "GRADE_CHANGED" : "GRADE_SAVED", input.changeReason());
        return evaluation;
    }

    @Transactional
    public Evaluation finalizeOne(Actor actor, ItemVersion item) {
        var evaluation = requireCurrent(actor, item.evaluationId(), item.version());
        if (evaluation.kind == EvaluationKind.MEMBER)
            throw ApiException.badRequest("GROUP_ROOT_REQUIRED", "Finalize the group document");
        if (evaluation.structuredFeedback != null) {
            var rubrics = GradingJson.rubrics(json, evaluation.rubricSnapshot);
            var currentEffectiveScores = GradingJson.effectiveItemScores(rubrics,
                    GradingJson.readChecks(json, evaluation.rubricChecks), evaluation.rubricItemScores);
            requireStructuredFeedbackReviewed(evaluation.structuredFeedback, evaluation.reviewedItemScores,
                    evaluation.acknowledgedReviewItems, currentEffectiveScores);
        }
        evaluation.finalizeGrade(actor.accountId(), clock.instant());
        propagateGroup(evaluation, actor, GroupOperation.FINALIZE, null);
        evaluations.save(evaluation);
        record(actor, evaluation, "GRADE_FINALIZED", null);
        return evaluation;
    }

    @Transactional
    public Evaluation publishOne(Actor actor, ItemVersion item) {
        var evaluation = requireCurrent(actor, item.evaluationId(), item.version());
        if (evaluation.kind == EvaluationKind.MEMBER)
            throw ApiException.badRequest("GROUP_ROOT_REQUIRED", "Publish the group document");
        evaluation.publish(actor.accountId(), clock.instant());
        var recipients = propagateGroup(evaluation, actor, GroupOperation.PUBLISH, null);
        if (evaluation.kind == EvaluationKind.ATTEMPT)
            recipients = List.of(evaluation.accountId);
        evaluations.save(evaluation);
        record(actor, evaluation, "GRADE_PUBLISHED", null);
        events.publish(new DomainEvent(1, UUID.randomUUID(), EventTypes.GRADE_PUBLISHED, "U09", clock.instant(),
                evaluation.id.toString(),
                Map.of("assignmentId", evaluation.assignmentId.toString(), "classId", evaluation.classId.toString(),
                        "studentIds", recipients.stream().map(UUID::toString).toList())));
        return evaluation;
    }

    @Transactional
    public Evaluation saveMember(Actor actor, UUID rootId, UUID accountId, MemberSave input) {
        var root = evaluations.findById(rootId).orElseThrow(() -> ApiException.notFound("GRADE_NOT_FOUND"));
        access.requireTeacher(actor, root.classId, Actions.GRADING_WRITE);
        root = evaluations.lockById(rootId).orElseThrow();
        root.editable();
        if (root.kind != EvaluationKind.GROUP_DOCUMENT || root.score == null)
            throw ApiException.conflict("GROUP_NOT_GRADED", "Grade group document first");
        final UUID documentId = root.groupDocumentId;
        final String hash = root.submissionHash;
        var candidate = evaluations.findForAssignmentAndClass(root.assignmentId, root.classId).stream()
                .filter(evaluation -> evaluation.kind == EvaluationKind.MEMBER
                        && documentId.equals(evaluation.groupDocumentId) && hash.equals(evaluation.submissionHash)
                        && accountId.equals(evaluation.accountId) && evaluation.state != EvaluationStatus.SUPERSEDED)
                .findFirst().orElseThrow(() -> ApiException.notFound("GRADE_NOT_FOUND"));
        var member = evaluations.lockById(candidate.id).orElseThrow();
        member.checkVersion(input.version());
        member.overrideMemberScore(input.score(), input.reason(), actor.accountId(), clock.instant());
        evaluations.save(member);
        record(actor, member, "GRADE_CHANGED", input.reason());
        return member;
    }

    private record ResolvedFeedbackState(GradingFeedback feedback, Map<UUID, Map<UUID, BigDecimal>> reviewedScores,
            Map<UUID, Set<UUID>> acknowledgedItems) {
    }

    private ResolvedFeedbackState resolveFeedbackState(Evaluation evaluation, Save input, GradingProposal usedProposal,
            List<RubricView> rubrics, Set<String> validBlockIds,
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
                validateMetadataEmptyOrKnown(input.acknowledgedReviewItems(), input.reviewedItemScores(), null,
                        currentEffectiveScores);
                return new ResolvedFeedbackState(null, Map.of(), Map.of());
            }
            var retained = evaluation.structuredFeedback.copy();
            validateMetadataEmptyOrKnown(input.acknowledgedReviewItems(), input.reviewedItemScores(), retained,
                    currentEffectiveScores);
            return computeReviewMetadata(evaluation, retained, retained, input, previousEffectiveScores,
                    currentEffectiveScores, false);
        }

        var validated = feedbackValidator.validate(input.structuredFeedback(), rubrics, validBlockIds, false);
        validateMetadataEmptyOrKnown(input.acknowledgedReviewItems(), input.reviewedItemScores(), validated,
                currentEffectiveScores);

        GradingFeedback baselineFeedback = evaluation.structuredFeedback;
        Map<UUID, Map<UUID, BigDecimal>> baselineScores = previousEffectiveScores;
        boolean fromProposal = false;
        if (usedProposal != null && usedProposal.proposal != null) {
            fromProposal = true;
            baselineScores = extractProposalEffectiveScores(usedProposal.proposal, rubrics);
            if (usedProposal.proposal.hasNonNull("structuredFeedback")) {
                baselineFeedback = feedbackValidator.validateNode(
                        usedProposal.proposal.get("structuredFeedback"), rubrics, validBlockIds, false);
            }
        }

        return computeReviewMetadata(evaluation, validated, baselineFeedback, input, baselineScores,
                currentEffectiveScores, fromProposal);
    }

    private ResolvedFeedbackState computeReviewMetadata(Evaluation evaluation, GradingFeedback target,
            GradingFeedback baselineFeedback, Save input,
            Map<UUID, Map<UUID, BigDecimal>> baselineScores,
            Map<UUID, Map<UUID, BigDecimal>> currentEffectiveScores,
            boolean fromProposal) {
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
            if (baseItem != null && baseItem.requiresReview() && !previouslyAck && !item.requiresReview() && !inputAck) {
                throw ApiException.badRequest("UNACKNOWLEDGED_REVIEW_FLAG",
                        "Cần xác nhận rõ ràng trước khi gỡ cờ kiểm tra của AI.");
            }

            BigDecimal baseScore = baselineScores == null ? null : baselineScores.getOrDefault(r, Map.of()).get(i);
            BigDecimal prevReviewedScore = !fromProposal && evaluation.reviewedItemScores != null
                    ? evaluation.reviewedItemScores.getOrDefault(r, Map.of()).get(i)
                    : baseScore;
            boolean editedText = baseItem == null || !Objects.equals(baseItem.evidence(), item.evidence())
                    || !Objects.equals(baseItem.reason(), item.reason())
                    || !Objects.equals(baseItem.suggestion(), item.suggestion());
            boolean scoreChangedFromBaseline = baseScore != null && currentScore.compareTo(baseScore) != 0;

            BigDecimal suppliedReviewedScore = input.reviewedItemScores() != null
                    && input.reviewedItemScores().getOrDefault(r, Map.of()).containsKey(i)
                            ? input.reviewedItemScores().get(r).get(i).setScale(2, RoundingMode.HALF_UP)
                            : null;

            if (inputAck && suppliedReviewedScore != null && suppliedReviewedScore.compareTo(currentScore) != 0) {
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
                if (suppliedReviewedScore != null && suppliedReviewedScore.compareTo(currentScore) == 0) {
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
                resolvedReviewedScore = prevReviewedScore != null ? prevReviewedScore : currentScore;
                resolvedAck = previouslyAck && input.acknowledgedReviewItems() == null;
            }

            nextReviewed.computeIfAbsent(r, k -> new LinkedHashMap<>()).put(i, resolvedReviewedScore);
            if (resolvedAck) {
                nextAck.computeIfAbsent(r, k -> new HashSet<>()).add(i);
            }
        }

        return new ResolvedFeedbackState(
                target,
                GradingJson.copyItemScores(nextReviewed),
                GradingJson.copyChecks(nextAck));
    }

    private void validateMetadataEmptyOrKnown(Map<UUID, Set<UUID>> acknowledged,
            Map<UUID, Map<UUID, BigDecimal>> reviewedScores, GradingFeedback feedback,
            Map<UUID, Map<UUID, BigDecimal>> currentEffectiveScores) {
        Map<UUID, Set<UUID>> knownFeedbackItems = new HashMap<>();
        if (feedback != null && feedback.items() != null) {
            for (var item : feedback.items()) {
                knownFeedbackItems.computeIfAbsent(item.rubricId(), k -> new HashSet<>()).add(item.itemId());
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
                            || !currentEffectiveScores.getOrDefault(r, Map.of()).containsKey(itemEntry.getKey())) {
                        throw ApiException.badRequest("INVALID_REVIEW_METADATA", "Invalid reviewedScore value");
                    }
                }
            }
        }
    }

    private static void requireStructuredFeedbackReviewed(GradingFeedback feedback,
            Map<UUID, Map<UUID, BigDecimal>> reviewedScores, Map<UUID, Set<UUID>> acknowledgedItems,
            Map<UUID, Map<UUID, BigDecimal>> currentEffectiveScores) {
        if (feedback == null || feedback.items() == null) {
            return;
        }
        for (var item : feedback.items()) {
            UUID r = item.rubricId();
            UUID i = item.itemId();
            boolean ack = acknowledgedItems != null && acknowledgedItems.getOrDefault(r, Set.of()).contains(i);
            if (item.requiresReview() && !ack) {
                throw ApiException.conflict("STRUCTURED_FEEDBACK_REVIEW_REQUIRED",
                        "Cần xác nhận hoặc chỉnh sửa các mục nhận xét được đánh dấu cần giảng viên kiểm tra trước khi chốt điểm.");
            }
            BigDecimal currentScore = currentEffectiveScores.getOrDefault(r, Map.of()).get(i);
            BigDecimal reviewedScore = reviewedScores == null ? null
                    : reviewedScores.getOrDefault(r, Map.of()).get(i);
            if (currentScore != null && (reviewedScore == null || currentScore.compareTo(reviewedScore) != 0) && !ack) {
                throw ApiException.conflict("STRUCTURED_FEEDBACK_SCORE_MISMATCH",
                        "Điểm mục rubric đã thay đổi sau khi tạo nhận xét; vui lòng chỉnh sửa hoặc xác nhận lại giải thích trước khi chốt điểm.");
            }
        }
    }

    private Map<UUID, Map<UUID, BigDecimal>> extractProposalEffectiveScores(JsonNode proposalNode,
            List<RubricView> rubrics) {
        try {
            Map<UUID, Set<UUID>> checks = proposalNode.has("checks")
                    ? json.convertValue(proposalNode.get("checks"), new TypeReference<Map<UUID, Set<UUID>>>() {
                    })
                    : Map.of();
            Map<UUID, Map<UUID, BigDecimal>> itemScores = proposalNode.has("itemScoresByRubric")
                    ? json.convertValue(proposalNode.get("itemScoresByRubric"),
                            new TypeReference<Map<UUID, Map<UUID, BigDecimal>>>() {
                            })
                    : Map.of();
            return GradingJson.effectiveItemScores(rubrics, checks, itemScores);
        } catch (RuntimeException ex) {
            return Map.of();
        }
    }

    private enum GroupOperation {
        SAVE, FINALIZE, PUBLISH
    }

    private List<UUID> propagateGroup(Evaluation root, Actor actor, GroupOperation operation, String reason) {
        if (root.kind != EvaluationKind.GROUP_DOCUMENT)
            return List.of();
        List<UUID> recipients = new ArrayList<>();
        var members = evaluations.findForAssignmentAndClass(root.assignmentId, root.classId).stream()
                .filter(member -> member.currentMemberOf(root))
                .sorted(Comparator.comparing(evaluation -> evaluation.id)).toList();
        for (var candidate : members) {
            var member = evaluations.lockById(candidate.id).orElseThrow();
            switch (operation) {
            case SAVE -> member.inheritGroupGrade(root, reason, actor.accountId(), clock.instant());
            case FINALIZE -> member.finalizeGrade(actor.accountId(), clock.instant());
            case PUBLISH -> member.publish(actor.accountId(), clock.instant());
            }
            evaluations.save(member);
            recipients.add(member.accountId);
        }
        if (recipients.isEmpty())
            throw ApiException.conflict("GROUP_MEMBERS_UNAVAILABLE", "Group member snapshot unavailable");
        return recipients;
    }

    void record(Actor actor, Evaluation evaluation, String action, String reason) {
        audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(), action, "EVALUATION",
                evaluation.id.toString(), AuditResult.SUCCESS, reason, Map.of("classId", evaluation.classId.toString()),
                clock.instant()));
    }
}
