package edu.aiplatform.grading.model;

import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Aggregate for an immutable submitted snapshot. No provider or persistence
 * dependencies.
 */
public class Evaluation {
    public UUID id;
    public UUID assignmentId;
    public UUID classId;
    public UUID attemptId;
    public UUID groupDocumentId;
    public UUID accountId;
    public UUID gradedBy;
    public EvaluationKind kind;
    public String submissionHash;
    public String rubricSnapshot;
    public String rubricChecks = "{}";
    public Map<UUID, Map<UUID, BigDecimal>> rubricItemScores = Map.of();
    public GradingFeedback structuredFeedback;
    public Map<UUID, Map<UUID, BigDecimal>> reviewedItemScores = Map.of();
    public Map<UUID, Set<UUID>> acknowledgedReviewItems = Map.of();
    public String feedback;
    public String adjustmentReason;
    public BigDecimal maxScore;
    public BigDecimal score;
    public BigDecimal adjustment = BigDecimal.ZERO;
    public EvaluationStatus state = EvaluationStatus.PENDING;
    public Instant createdAt;
    public Instant gradedAt;
    public Instant publishedAt;
    public long version;
    public boolean memberOverride;
    public List<EvaluationHistory> changes = new ArrayList<>();

    public static Evaluation pending(UUID attemptId, UUID assignmentId, UUID classId, UUID accountId, String hash,
            BigDecimal max, String rubrics, Instant now) {
        var evaluation = new Evaluation();
        evaluation.id = UUID.randomUUID();
        evaluation.attemptId = attemptId;
        evaluation.assignmentId = assignmentId;
        evaluation.classId = classId;
        evaluation.accountId = accountId;
        evaluation.kind = EvaluationKind.ATTEMPT;
        evaluation.submissionHash = Objects.requireNonNull(hash);
        evaluation.maxScore = Objects.requireNonNull(max);
        if (max.signum() < 0 || max.compareTo(new BigDecimal("9999.99")) > 0)
            throw ApiException.badRequest("INVALID_RUBRIC", "Invalid maximum score");
        evaluation.rubricSnapshot = rubrics;
        evaluation.createdAt = now;
        return evaluation;
    }

    public void checkVersion(long expected) {
        if (version != expected)
            throw ApiException.conflict("GRADE_VERSION_CONFLICT", "Reload the evaluation before saving");
    }

    public void save(String checks, BigDecimal rubricScore, BigDecimal delta, String deltaReason, String feedback,
            String changeReason, UUID actor, Instant now) {
        editable();
        if (feedback != null && feedback.length() > 10000)
            throw ApiException.badRequest("FEEDBACK_TOO_LONG", "Feedback exceeds 10000 characters");
        delta = delta == null ? BigDecimal.ZERO : delta;
        if (delta.scale() > 2 || delta.abs().compareTo(new BigDecimal("9999.99")) > 0)
            throw ApiException.badRequest("INVALID_ADJUSTMENT", "Invalid adjustment");
        if (delta.signum() != 0 && blank(deltaReason))
            throw ApiException.badRequest("ADJUSTMENT_REASON_REQUIRED", "Adjustment requires a reason");
        boolean official = official();
        if (official && blank(changeReason))
            throw ApiException.badRequest("CHANGE_REASON_REQUIRED", "Changing an official grade requires a reason");
        var next = rubricScore.add(delta).max(BigDecimal.ZERO).min(maxScore).setScale(2, RoundingMode.HALF_UP);
        if (official)
            changes.add(new EvaluationHistory(actor, now, score, next, changeReason, rubricChecks, this.feedback,
                    copyItemScores(rubricItemScores),
                    structuredFeedback == null ? null : structuredFeedback.copy()));
        rubricChecks = checks;
        score = next;
        adjustment = delta;
        adjustmentReason = deltaReason;
        this.feedback = feedback;
        gradedBy = actor;
        gradedAt = now;
        if (!official)
            state = EvaluationStatus.DRAFT;
    }

    public void finalizeGrade(UUID actor, Instant now) {
        if (state != EvaluationStatus.DRAFT || score == null)
            throw ApiException.conflict("INVALID_GRADE_STATE", "Only DRAFT grades can be finalized");
        state = EvaluationStatus.FINALIZED;
        gradedBy = actor;
        gradedAt = now;
    }

    public void publish(UUID actor, Instant now) {
        if (state != EvaluationStatus.FINALIZED)
            throw ApiException.conflict("INVALID_GRADE_STATE", "Only FINALIZED grades can be published");
        state = EvaluationStatus.PUBLISHED;
        publishedAt = now;
    }

    public boolean official() {
        return state == EvaluationStatus.FINALIZED || state == EvaluationStatus.PUBLISHED;
    }

    public boolean currentMemberOf(Evaluation root) {
        return kind == EvaluationKind.MEMBER && root.groupDocumentId.equals(groupDocumentId)
                && root.submissionHash.equals(submissionHash) && state != EvaluationStatus.SUPERSEDED;
    }

    public void inheritGroupGrade(Evaluation root, String reason, UUID actorId, Instant now) {
        if (!memberOverride) {
            save(root.rubricChecks, root.score, BigDecimal.ZERO, null, root.feedback, reason, actorId, now);
        } else {
            rubricChecks = root.rubricChecks;
        }
        rubricItemScores = copyItemScores(root.rubricItemScores);
        structuredFeedback = root.structuredFeedback == null ? null : root.structuredFeedback.copy();
        reviewedItemScores = copyItemScores(root.reviewedItemScores);
        acknowledgedReviewItems = copyChecks(root.acknowledgedReviewItems);
    }

    public void overrideMemberScore(BigDecimal memberScore, String reason, UUID actorId, Instant now) {
        if (memberScore == null || memberScore.signum() < 0 || memberScore.compareTo(maxScore) > 0
                || memberScore.scale() > 2) {
            throw ApiException.badRequest("INVALID_MEMBER_SCORE", "Invalid member score");
        }
        memberOverride = true;
        save(rubricChecks, memberScore, BigDecimal.ZERO, null, feedback, reason, actorId, now);
    }

    public void resetForGroupSubmission(String hash, BigDecimal maximum, String rubrics, Instant now) {
        if (score != null) {
            changes.add(new EvaluationHistory(null, now, score, null, "GROUP_RESUBMITTED", rubricChecks, feedback,
                    copyItemScores(rubricItemScores),
                    structuredFeedback == null ? null : structuredFeedback.copy()));
        }
        memberOverride = false;
        state = EvaluationStatus.PENDING;
        submissionHash = hash;
        rubricSnapshot = rubrics;
        maxScore = maximum;
        score = null;
        rubricChecks = "{}";
        rubricItemScores = Map.of();
        structuredFeedback = null;
        reviewedItemScores = Map.of();
        acknowledgedReviewItems = Map.of();
        feedback = null;
        adjustment = BigDecimal.ZERO;
        adjustmentReason = null;
        gradedBy = null;
        gradedAt = null;
        publishedAt = null;
    }

    public void supersede() {
        if (state != EvaluationStatus.PUBLISHED)
            state = EvaluationStatus.SUPERSEDED;
    }

    public void editable() {
        if (state == EvaluationStatus.SUPERSEDED)
            throw ApiException.conflict("SUPERSEDED", "Evaluation is superseded");
    }

    public EvaluationStatus status() {
        return state;
    }

    public BigDecimal score() {
        return score;
    }

    public List<EvaluationHistory> history() {
        return List.copyOf(changes);
    }

    public static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static Map<UUID, Map<UUID, BigDecimal>> copyItemScores(Map<UUID, Map<UUID, BigDecimal>> scores) {
        if (scores == null || scores.isEmpty())
            return Map.of();
        var copy = new LinkedHashMap<UUID, Map<UUID, BigDecimal>>();
        scores.forEach((id, values) -> copy.put(id, values == null ? Map.of() : Map.copyOf(values)));
        return Map.copyOf(copy);
    }

    private static Map<UUID, Set<UUID>> copyChecks(Map<UUID, Set<UUID>> checks) {
        if (checks == null || checks.isEmpty())
            return Map.of();
        var copy = new LinkedHashMap<UUID, Set<UUID>>();
        checks.forEach((id, values) -> copy.put(id, values == null ? Set.of() : Set.copyOf(values)));
        return Map.copyOf(copy);
    }
}
