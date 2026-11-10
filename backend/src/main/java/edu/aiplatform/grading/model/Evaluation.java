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

    /** Creates a pending evaluation for a submitted attempt. */
    public static Evaluation createPendingEvaluation(UUID attemptId, UUID assignmentId,
            UUID classId, UUID accountId, String hash, BigDecimal max, String rubrics,
            Instant now) {
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

    /** Rejects a write when the evaluation version has changed. */
    public void checkVersion(long expected) {
        if (version != expected)
            throw ApiException.conflict("GRADE_VERSION_CONFLICT",
                    "Reload the evaluation before saving");
    }

    /** Saves rubric results, an optional adjustment, and feedback as a draft grade. */
    public void saveGrade(String checks, BigDecimal rubricScore, BigDecimal delta,
            String deltaReason, String feedback, String changeReason, UUID actor, Instant now) {
        requireMutableGrade();
        if (feedback != null && feedback.length() > 10000)
            throw ApiException.badRequest("FEEDBACK_TOO_LONG", "Feedback exceeds 10000 characters");
        delta = delta == null ? BigDecimal.ZERO : delta;
        if (delta.scale() > 2 || delta.abs().compareTo(new BigDecimal("9999.99")) > 0)
            throw ApiException.badRequest("INVALID_ADJUSTMENT", "Invalid adjustment");
        if (delta.signum() != 0 && isBlankText(deltaReason))
            throw ApiException.badRequest("ADJUSTMENT_REASON_REQUIRED",
                    "Adjustment requires a reason");
        var next = rubricScore.add(delta).max(BigDecimal.ZERO).min(maxScore).setScale(2,
                RoundingMode.HALF_UP);
        rubricChecks = checks;
        score = next;
        adjustment = delta;
        adjustmentReason = deltaReason;
        this.feedback = feedback;
        gradedBy = actor;
        gradedAt = now;
        state = EvaluationStatus.DRAFT;
    }

    /** Finalizes a draft grade. */
    public void finalizeGrade(UUID actor, Instant now) {
        if (state != EvaluationStatus.DRAFT || score == null)
            throw ApiException.conflict("INVALID_GRADE_STATE",
                    "Only DRAFT grades can be finalized");
        state = EvaluationStatus.FINALIZED;
        gradedBy = actor;
        gradedAt = now;
    }

    /** Publishes a finalized grade to the student. */
    public void publishGrade(UUID actor, Instant now) {
        if (state != EvaluationStatus.FINALIZED)
            throw ApiException.conflict("INVALID_GRADE_STATE",
                    "Only FINALIZED grades can be published");
        state = EvaluationStatus.PUBLISHED;
        publishedAt = now;
    }

    /** Reports whether the grade has been finalized or published. */
    public boolean hasOfficialGrade() {
        return state == EvaluationStatus.FINALIZED || state == EvaluationStatus.PUBLISHED;
    }

    /** Checks whether this member evaluation belongs to the current group submission. */
    public boolean isCurrentMemberOf(Evaluation root) {
        return kind == EvaluationKind.MEMBER && root.groupDocumentId.equals(groupDocumentId)
                && root.submissionHash.equals(submissionHash)
                && state != EvaluationStatus.SUPERSEDED;
    }

    /** Copies the group grade and review data to a member unless their score is overridden. */
    public void inheritGroupGrade(Evaluation root, String reason, UUID actorId, Instant now) {
        requireMutableGrade();
        if (!memberOverride) {
            saveGrade(root.rubricChecks, root.score, BigDecimal.ZERO, null, root.feedback, reason,
                    actorId, now);
        } else {
            rubricChecks = root.rubricChecks;
        }
        rubricItemScores = copyItemScores(root.rubricItemScores);
        structuredFeedback = root.structuredFeedback == null ? null
                : root.structuredFeedback.copyFeedback();
        reviewedItemScores = copyItemScores(root.reviewedItemScores);
        acknowledgedReviewItems = copyChecks(root.acknowledgedReviewItems);
    }

    /** Sets an individual member score and records the override reason. */
    public void overrideMemberScore(BigDecimal memberScore, String reason, UUID actorId,
            Instant now) {
        requireMutableGrade();
        if (memberScore == null || memberScore.signum() < 0 || memberScore.compareTo(maxScore) > 0
                || memberScore.scale() > 2) {
            throw ApiException.badRequest("INVALID_MEMBER_SCORE", "Invalid member score");
        }
        memberOverride = true;
        saveGrade(rubricChecks, memberScore, BigDecimal.ZERO, null, feedback, reason, actorId, now);
    }

    /** Resets grading state after a group submits a new version. */
    public void resetForGroupSubmission(String hash, BigDecimal maximum, String rubrics,
            Instant now) {
        if (score != null) {
            changes.add(new EvaluationHistory(null, now, score, null, "GROUP_RESUBMITTED",
                    rubricChecks, feedback, copyItemScores(rubricItemScores),
                    structuredFeedback == null ? null : structuredFeedback.copyFeedback()));
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

    /** Marks this evaluation as superseded. */
    public void supersede() {
        state = EvaluationStatus.SUPERSEDED;
    }

    /** Ensures the grade is not locked and the evaluation is editable. */
    public void requireMutableGrade() {
        if (hasOfficialGrade())
            throw ApiException.conflict("GRADE_LOCKED", "Điểm đã chốt không thể chỉnh sửa.");
        requireEditableGrade();
    }

    /** Rejects edits to a superseded evaluation. */
    public void requireEditableGrade() {
        if (state == EvaluationStatus.SUPERSEDED)
            throw ApiException.conflict("SUPERSEDED", "Evaluation is superseded");
    }

    /** Returns the current evaluation status. */
    public EvaluationStatus status() {
        return state;
    }

    /** Returns the current score. */
    public BigDecimal score() {
        return score;
    }

    /** Returns an immutable view of grade change history. */
    public List<EvaluationHistory> history() {
        return List.copyOf(changes);
    }

    /** Checks whether text is null or contains only whitespace. */
    public static boolean isBlankText(String value) {
        return value == null || value.isBlank();
    }

    /** Copies rubric item scores into immutable nested maps. */
    private static Map<UUID, Map<UUID, BigDecimal>> copyItemScores(
            Map<UUID, Map<UUID, BigDecimal>> scores) {
        if (scores == null || scores.isEmpty())
            return Map.of();
        var copy = new LinkedHashMap<UUID, Map<UUID, BigDecimal>>();
        scores.forEach(
                (id, values) -> copy.put(id, values == null ? Map.of() : Map.copyOf(values)));
        return Map.copyOf(copy);
    }

    /** Copies rubric checks into immutable nested sets. */
    private static Map<UUID, Set<UUID>> copyChecks(Map<UUID, Set<UUID>> checks) {
        if (checks == null || checks.isEmpty())
            return Map.of();
        var copy = new LinkedHashMap<UUID, Set<UUID>>();
        checks.forEach(
                (id, values) -> copy.put(id, values == null ? Set.of() : Set.copyOf(values)));
        return Map.copyOf(copy);
    }
}
