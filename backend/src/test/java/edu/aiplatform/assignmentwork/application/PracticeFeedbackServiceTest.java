package edu.aiplatform.assignmentwork.application;

import static edu.aiplatform.assignmentwork.application.WorkFixtures.NOW;
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
import edu.aiplatform.assignmentwork.domain.AssignmentAttempt;
import edu.aiplatform.assignmentwork.domain.PracticeFeedback;
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
            return e.code();
        }
        throw new AssertionError("Cần lỗi nghiệp vụ");
    }

    private PracticeFeedback pending(AssignmentAttempt a, String hash) {
        PracticeFeedback p = new PracticeFeedback(UUID.randomUUID(), a.id(), a.accountId(), PracticeFeedback.Origin.AI_TASK,
                null, "r", PracticeFeedback.Status.PENDING, hash, null, JsonNodeFactory.instance.objectNode(), null, NOW, null);
        when(f.feedback.find(p.id())).thenReturn(Optional.of(p));
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
        assertThat(code(() -> f.feedbackService.request(f.student, a.id(), null, "r1"))).isEqualTo("AI_SOURCE_INVALID");
    }

    @Test
    void missingPersonalKeyIsNotSentAndLeavesNoFailedFeedback() {
        AssignmentAttempt a = f.attempt(1, AttemptStatus.SUBMITTED, NOW, false);
        when(f.feedback.findByRequest(anyString())).thenReturn(Optional.empty());
        doThrow(ApiException.badRequest("AI_PERSONAL_NOT_CONFIGURED", "Bạn chưa lưu khóa API cho nhà cung cấp này"))
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
        when(f.feedback.find(any())).thenAnswer(i -> Optional.of(new PracticeFeedback(i.getArgument(0), a.id(),
                a.accountId(), PracticeFeedback.Origin.AI_TASK, null, "r1", PracticeFeedback.Status.FAILED, null, null,
                JsonNodeFactory.instance.objectNode(), "Hết hạn mức", NOW, NOW)));
        when(f.aiTasks.submit(any())).thenThrow(ApiException.badRequest("AI_QUOTA_EXCEEDED", "Hết hạn mức"));
        f.feedbackService.request(f.student, a.id(), AiProvider.GEMINI, "r1");
        verify(f.feedback).insert(any());
        verify(f.feedback).markFailed(any(), eq("Hết hạn mức"), any());
    }

    @Test
    void resultWithOtherHashIsStaleAndBadFormatIsRejected() {
        AssignmentAttempt a = f.attempt(1, AttemptStatus.SUBMITTED, NOW, false);
        PracticeFeedback old = pending(a, "hash-cũ");
        assertThat(f.feedbackService.onReady(result(old, OUTPUT))).isEqualTo(ApplyOutcome.STALE);
        verify(f.feedback).markFailed(eq(old.id()), anyString(), any());
        PracticeFeedback current = pending(a, a.receiptHash());
        assertThat(f.feedbackService.onReady(result(current, "{\"summary\":1}"))).isEqualTo(ApplyOutcome.REJECTED);
    }

    @Test
    void validResultIsSavedWithEstimateLabelAndWithoutRubric() {
        AssignmentAttempt a = f.attempt(1, AttemptStatus.SUBMITTED, NOW, false);
        PracticeFeedback p = pending(a, a.receiptHash());
        when(f.feedback.markReady(eq(p.id()), any(), any(), any())).thenReturn(1);
        assertThat(f.feedbackService.onReady(result(p, OUTPUT))).isEqualTo(ApplyOutcome.APPLIED);
        verify(f.feedback).markReady(eq(p.id()), argThat((JsonNode body) -> FeedbackFormat.LABEL.equals(body.path("label").asText())
                && body.path("parts").get(0).path("comment").asText().equals("Ổn")
                && !body.has("rubric") && !body.has("estimatedTotal")), any(), any());
        assertThat(FeedbackFormat.OUTPUT_SCHEMA).doesNotContain("rubric");
    }

    @Test
    void promptAndAgentReadHaveNoRubric() {
        when(f.assignments.getContent(any())).thenAnswer(i -> Optional.of(new AssignmentContent(f.view.assignmentId(), 1,
                "Hướng dẫn", f.skeleton, List.of(new DocumentPart("DOCUMENT", null, List.of(), "Toàn bài")), Map.of(),
                List.of(new RubricView(UUID.randomUUID(), f.view.assignmentId(), "DOCUMENT", List.of(new RubricCriterion(
                        UUID.randomUUID(), "Mục chấm bí mật", "", CriterionKind.TEXT,
                        List.of(new RubricItem(UUID.randomUUID(), "Mục chấm bí mật", BigDecimal.ONE)))), BigDecimal.ONE)))));
        AssignmentAttempt a = f.attempt(1, AttemptStatus.SUBMITTED, NOW, false);
        AiPrompt prompt = f.feedbackService.prompt(a);
        assertThat(prompt.system() + prompt.user()).doesNotContain("bí mật").doesNotContainIgnoringCase("rubric");
        assertThat(f.feedbackService.readOwnWork(f.student, a.id())).doesNotContainKey("rubric")
                .containsKeys("instructions", "assignment", "submission", "submissionHash");
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
        verify(f.audit).record(argThat(e -> e.action().equals("PRACTICE_FEEDBACK_REQUESTED")));
    }
}
