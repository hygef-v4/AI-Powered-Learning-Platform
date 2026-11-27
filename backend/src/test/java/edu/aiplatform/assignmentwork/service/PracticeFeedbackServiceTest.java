package edu.aiplatform.assignmentwork.service;

import static edu.aiplatform.assignmentwork.service.WorkFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import edu.aiplatform.ai.contract.AiPrompt;
import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.ai.contract.AiTaskResult;
import edu.aiplatform.ai.contract.ApplyOutcome;
import edu.aiplatform.assignments.contract.AssignmentContent;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.CriterionKind;
import edu.aiplatform.assignments.contract.DocumentPart;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.RubricCriterion;
import edu.aiplatform.assignments.contract.RubricItem;
import edu.aiplatform.assignments.contract.RubricView;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignmentwork.contract.AttemptStatus;
import edu.aiplatform.assignmentwork.model.AssignmentAttempt;
import edu.aiplatform.assignmentwork.model.PracticeFeedback;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** F9, F10; BR-U08-40…45. */
class PracticeFeedbackServiceTest {

    private final WorkFixtures f = new WorkFixtures();
    private static final String OUTPUT = """
            {"summary":"Khá","strengths":["Rõ"],"parts":[{"partId":"DOCUMENT","comment":"Ổn"}],"suggestions":["Thêm ví dụ"],
             "rubric":[{"partId":"DOCUMENT","criterion":"Nội dung","item":"Đủ ý","achieved":true,"estimatedPoints":2}],"estimatedTotal":2}
            """;

    @BeforeEach
    void practice() {
        f.view = f.view(AssignmentStatus.OPEN, SubmissionMode.INDIVIDUAL, GradingMode.PRACTICE, NOW.plus(Duration.ofDays(1)), null, 3);
    }

    private static String code(Runnable action) {
        try {
            action.run();
        } catch (ApiException e) {
            return e.readCode();
        }
        throw new AssertionError("Cần lỗi nghiệp vụ");
    }

    private PracticeFeedback pending(AssignmentAttempt a, String hash) {
        PracticeFeedback p = new PracticeFeedback(UUID.randomUUID(), a.id(), a.accountId(), PracticeFeedback.Origin.AI_TASK,
                null, "r", PracticeFeedback.Status.PENDING, hash, null, JsonNodeFactory.instance.objectNode(), null, NOW, null);
        when(f.feedback.findFeedback(p.id())).thenReturn(Optional.of(p));
        return p;
    }

    private AiTaskResult result(PracticeFeedback p, String output) {
        return new AiTaskResult(UUID.randomUUID(), PracticeFeedbackService.TASK_TYPE, f.student,
                PracticeFeedbackService.TARGET_PREFIX + p.id(), 0, output);
    }

    @Test
    void onlySubmittedPracticeAttemptsOfMine() {
        AssignmentAttempt working = f.attempt(1, AttemptStatus.IN_PROGRESS, NOW.plus(Duration.ofDays(1)), false);
        assertThat(code(() -> f.feedbackService.request(f.student, working.id(), AiProvider.GEMINI, "r1")))
                .isEqualTo("FEEDBACK_NOT_ALLOWED");
        f.view = f.view(AssignmentStatus.OPEN, SubmissionMode.INDIVIDUAL, GradingMode.GRADED, NOW.plus(Duration.ofDays(1)), null, 3);
        AssignmentAttempt graded = f.attempt(2, AttemptStatus.SUBMITTED, NOW, false);
        assertThat(code(() -> f.feedbackService.request(f.student, graded.id(), AiProvider.GEMINI, "r2")))
                .isEqualTo("FEEDBACK_NOT_ALLOWED");
    }

    @Test
    void pendingAndDailyLimits() {
        AssignmentAttempt a = f.attempt(1, AttemptStatus.SUBMITTED, NOW, false);
        when(f.feedback.findByRequest(anyString())).thenReturn(Optional.empty());
        when(f.feedback.hasPending(a.id())).thenReturn(true);
        assertThat(code(() -> f.feedbackService.request(f.student, a.id(), AiProvider.GEMINI, "r1"))).isEqualTo("FEEDBACK_PENDING");
        when(f.feedback.hasPending(a.id())).thenReturn(false);
        when(f.feedback.countSince(eq(f.student.accountId()), any())).thenReturn(20L);
        assertThat(code(() -> f.feedbackService.request(f.student, a.id(), AiProvider.GEMINI, "r1"))).isEqualTo("FEEDBACK_LIMIT");
    }

    @Test
    void missingPersonalKeyIsNotSentAndLeavesNoFailedFeedback() {
        AssignmentAttempt a = f.attempt(1, AttemptStatus.SUBMITTED, NOW, false);
        when(f.feedback.findByRequest(anyString())).thenReturn(Optional.empty());
        doThrow(ApiException.createBadRequest("AI_PERSONAL_NOT_CONFIGURED", "Bạn chưa lưu khóa API cho nhà cung cấp này"))
                .when(f.aiTasks).requireSource(f.student, AiSource.PERSONAL, AiProvider.GEMINI);
        assertThat(code(() -> f.feedbackService.request(f.student, a.id(), AiProvider.GEMINI, "r1")))
                .isEqualTo("AI_PERSONAL_NOT_CONFIGURED");
        verify(f.feedback, never()).insert(any());
        verify(f.aiTasks, never()).submit(any());
        verify(f.feedback, never()).markFailed(any(), anyString(), any());
    }

    @Test
    void otherAiErrorKeepsFailedFeedback() {
        AssignmentAttempt a = f.attempt(1, AttemptStatus.SUBMITTED, NOW, false);
        when(f.feedback.findByRequest(anyString())).thenReturn(Optional.empty());
        when(f.feedback.findFeedback(any())).thenAnswer(i -> Optional.of(new PracticeFeedback(i.getArgument(0), a.id(),
                a.accountId(), PracticeFeedback.Origin.AI_TASK, null, "r1", PracticeFeedback.Status.FAILED, null, null,
                JsonNodeFactory.instance.objectNode(), "Hết hạn mức", NOW, NOW)));
        when(f.aiTasks.submit(any())).thenThrow(ApiException.createBadRequest("AI_QUOTA_EXCEEDED", "Hết hạn mức"));
        f.feedbackService.request(f.student, a.id(), AiProvider.GEMINI, "r1");
        verify(f.feedback).insert(any());
        verify(f.feedback).markFailed(any(), eq("Hết hạn mức"), any());
    }

    @Test
    void resultWithOtherHashIsStaleAndBadFormatIsRejected() {
        AssignmentAttempt a = f.attempt(1, AttemptStatus.SUBMITTED, NOW, false);
        PracticeFeedback old = pending(a, "hash-cũ");
        assertThat(f.feedbackService.handleReadyResult(result(old, OUTPUT))).isEqualTo(ApplyOutcome.STALE);
        verify(f.feedback).markFailed(eq(old.id()), anyString(), any());
        PracticeFeedback current = pending(a, a.receiptHash());
        assertThat(f.feedbackService.handleReadyResult(result(current, "{\"summary\":1}"))).isEqualTo(ApplyOutcome.REJECTED);
    }

    @Test
    void resultWrappedAsTextAtTheRootIsStillSaved() throws Exception {
        AssignmentAttempt a = f.attempt(1, AttemptStatus.SUBMITTED, NOW, false);
        PracticeFeedback p = pending(a, a.receiptHash());
        when(f.feedback.markReady(eq(p.id()), any(), any(), any())).thenReturn(1);
        String wrapped = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(OUTPUT.strip());

        assertThat(f.feedbackService.handleReadyResult(result(p, wrapped))).isEqualTo(ApplyOutcome.APPLIED);
        verify(f.feedback).markReady(eq(p.id()), argThat((JsonNode body) -> "Khá".equals(body.path("summary").asText())),
                any(), any());
    }

    @Test
    void validResultIsSavedWithEstimateLabelAndWithoutRubric() {
        AssignmentAttempt a = f.attempt(1, AttemptStatus.SUBMITTED, NOW, false);
        PracticeFeedback p = pending(a, a.receiptHash());
        when(f.feedback.markReady(eq(p.id()), any(), any(), any())).thenReturn(1);
        assertThat(f.feedbackService.handleReadyResult(result(p, OUTPUT))).isEqualTo(ApplyOutcome.APPLIED);
        verify(f.feedback).markReady(eq(p.id()), argThat((JsonNode body) -> FeedbackFormat.LABEL.equals(body.path("label").asText())
                && body.path("parts").get(0).path("comment").asText().equals("Ổn")
                && !body.has("rubric") && !body.has("estimatedTotal")), any(), any());
        assertThat(FeedbackFormat.OUTPUT_SCHEMA).doesNotContain("rubric");
    }

    private void rubricOfTwoPoints() {
        when(f.assignments.getContent(any())).thenAnswer(i -> Optional.of(new AssignmentContent(f.view.assignmentId(), 1,
                "Hướng dẫn", f.skeleton, List.of(new DocumentPart("DOCUMENT", null, List.of(), "Toàn bài")), Map.of(),
                List.of(new RubricView(UUID.randomUUID(), f.view.assignmentId(), "DOCUMENT", List.of(new RubricCriterion(
                        UUID.randomUUID(), "Nội dung", "", CriterionKind.TEXT,
                        List.of(new RubricItem(UUID.randomUUID(), "Đủ ý chính", new BigDecimal("2"))))),
                        new BigDecimal("2"))))));
    }

    @Test
    void promptAndAgentReadIncludeTheRubricToEstimatePartScores() {
        rubricOfTwoPoints();
        AssignmentAttempt a = f.attempt(1, AttemptStatus.SUBMITTED, NOW, false);
        AiPrompt prompt = f.feedbackService.prompt(a);
        assertThat(prompt.user()).contains("Rubric", "Đủ ý chính", "tối đa 2 điểm");
        assertThat(prompt.system()).contains("score").contains("không chép lại hay liệt kê từng tiêu chí");
        assertThat(f.feedbackService.readOwnWork(f.student, a.id())).containsKey("rubric")
                .containsKeys("instructions", "assignment", "submission", "submissionHash");
    }

    @Test
    void partScoresAreClampedToTheRubricAndTotalled() {
        rubricOfTwoPoints();
        AssignmentAttempt a = f.attempt(1, AttemptStatus.SUBMITTED, NOW, false);
        PracticeFeedback p = pending(a, a.receiptHash());
        when(f.feedback.markReady(eq(p.id()), any(), any(), any())).thenReturn(1);
        String output = """
                {"summary":"Khá","strengths":[],"parts":[{"partId":"DOCUMENT","comment":"Ổn","score":5},
                 {"partId":"OTHER","comment":"Thêm","score":3}],"suggestions":[]}
                """;

        assertThat(f.feedbackService.handleReadyResult(result(p, output))).isEqualTo(ApplyOutcome.APPLIED);

        verify(f.feedback).markReady(eq(p.id()), argThat((JsonNode body) ->
                body.path("score").decimalValue().compareTo(new BigDecimal("2")) == 0
                        && body.path("maxScore").decimalValue().compareTo(new BigDecimal("2")) == 0
                        && body.path("parts").get(0).path("score").decimalValue().compareTo(new BigDecimal("2")) == 0
                        && body.path("parts").get(0).path("maxScore").decimalValue().compareTo(new BigDecimal("2")) == 0
                        && !body.path("parts").get(1).has("score")), any(), any());
    }

    @Test
    void agentFeedbackNeedsMatchingHash() throws Exception {
        AssignmentAttempt a = f.attempt(1, AttemptStatus.SUBMITTED, NOW, false);
        JsonNode body = f.json.readTree(OUTPUT);
        assertThat(code(() -> f.feedbackService.recordFromAgent(f.student, a.id(), "khác", body, "agent")))
                .isEqualTo("SUBMISSION_HASH_MISMATCH");
        PracticeFeedback saved = f.feedbackService.recordFromAgent(f.student, a.id(), a.receiptHash(), body, "agent");
        assertThat(saved.status()).isEqualTo(PracticeFeedback.Status.READY);
        assertThat(saved.origin()).isEqualTo(PracticeFeedback.Origin.MCP);
    }
}
