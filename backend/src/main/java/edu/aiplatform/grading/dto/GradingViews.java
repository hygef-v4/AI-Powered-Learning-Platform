package edu.aiplatform.grading.dto;

import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.RubricView;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignmentwork.contract.ActivitySummary;
import edu.aiplatform.grading.contract.GroupSectionAuthor;
import edu.aiplatform.grading.model.Evaluation;
import edu.aiplatform.grading.model.GradingFeedback;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class GradingViews {
    private GradingViews() {
    }

    public record ItemResult(UUID evaluationId, String outcome, String code, Long newVersion) {
    }

    public record BatchResult(List<ItemResult> items) {
    }

    public record AiEstimate(long estimatedTokens, int items) {
    }

    public record Person(UUID accountId, String name, String email) {
    }

    public record SubmissionRow(Person student, UUID evaluationId, UUID attemptId, String status, BigDecimal score,
            BigDecimal maxScore, boolean late, Instant submittedAt, int attemptCount, long version) {
    }

    public record SubmissionPage(List<SubmissionRow> rows, int total, int page, int size, Map<String, Long> summary,
            Instant deadline) {
    }

    public record EvaluationDetail(Evaluation evaluation, DocumentModel document, List<RubricView> rubrics,
            List<UUID> navigationIds, ActivitySummary activity, String activityState, List<Evaluation> members,
            List<MemberActivity> memberActivities, List<GroupSectionAuthor> sectionAuthors, boolean current) {
    }

    public record MemberActivity(Person student, ActivitySummary activity, String state) {
    }

    public record GradeCell(UUID assignmentId, UUID evaluationId, String status, BigDecimal score, BigDecimal maxScore,
            boolean late, Instant submittedAt, String feedback) {
    }

    public record GradebookRow(Person student, List<GradeCell> grades) {
    }

    public record AssignmentColumn(UUID id, String title, BigDecimal maxScore, SubmissionMode submissionMode) {
    }

    public record GradebookPage(List<GradebookRow> rows, List<AssignmentColumn> assignments, int total, int page,
            int size) {
    }

    public record StudentRubricItemView(UUID rubricId, String partId, String criterionTitle, UUID itemId,
            String itemText, BigDecimal awardedPoints, BigDecimal maxPoints, String evidence, String reason,
            String suggestion) {
    }

    public record StudentGradeView(UUID assignmentId, UUID attemptId, BigDecimal score, BigDecimal maxScore,
            String feedback, Instant publishedAt, boolean updated, String rubricChecks,
            GradingFeedback.Summary structuredSummary, List<StudentRubricItemView> rubricItems,
            BigDecimal rubricScore, boolean memberOverride) {
        public StudentGradeView(UUID assignmentId, UUID attemptId, BigDecimal score, BigDecimal maxScore,
                String feedback, Instant publishedAt, boolean updated, String rubricChecks) {
            this(assignmentId, attemptId, score, maxScore, feedback, publishedAt, updated, rubricChecks, null,
                    List.of(), null, false);
        }
    }
}
