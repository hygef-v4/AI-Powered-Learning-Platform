package edu.aiplatform.assignmentwork.application;

import static edu.aiplatform.assignmentwork.application.WorkFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.assignments.contract.AssignmentContent;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.BlockOrigin;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.DocumentPart;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignments.contract.SubmissionWindow;
import edu.aiplatform.assignments.contract.ValidationIssue;
import edu.aiplatform.assignments.contract.ValidationResult;
import edu.aiplatform.assignmentwork.contract.AttemptStatus;
import edu.aiplatform.assignmentwork.contract.SubmitMode;
import edu.aiplatform.assignmentwork.domain.AssignmentAttempt;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** F3–F7; BR-U08-10…16, 20…23, 60, 61. */
class AttemptServiceTest {

    private final WorkFixtures f = new WorkFixtures();

    private static String code(Runnable action) {
        try {
            action.run();
        } catch (ApiException e) {
            return e.code();
        }
        throw new AssertionError("Cần lỗi nghiệp vụ");
    }

    private DocumentModel withStudentWork() {
        return new DocumentModel(List.of(WorkFixtures.paragraph(BlockOrigin.STUDENT, "Bài làm"), f.teacherBlock));
    }

    @Test
    void notEnrolledStudentGetsNotFoundAndDeniedAudit() {
        when(f.authorization.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.deny("NOT_ENROLLED"));
        assertThat(code(() -> f.attemptService.start(f.student, f.assignmentId, f.classId))).isEqualTo("NOT_FOUND");
        verify(f.audit).record(argThat(e -> e.result() == AuditResult.DENIED));
        assertThat(f.store).isEmpty();
    }

    @Test
    void startChecksGroupWindowAndAttemptsLeft() {
        f.view = f.view(AssignmentStatus.OPEN, SubmissionMode.GROUP, GradingMode.GRADED, NOW.plus(Duration.ofDays(1)), null, 1);
        assertThat(code(() -> f.attemptService.start(f.student, f.assignmentId, f.classId))).isEqualTo("GROUP_ASSIGNMENT");
        f.view = f.view(AssignmentStatus.OPEN, SubmissionMode.INDIVIDUAL, GradingMode.GRADED, NOW.plus(Duration.ofDays(1)), null, 1);
        f.window = SubmissionWindow.CLOSED;
        assertThat(code(() -> f.attemptService.start(f.student, f.assignmentId, f.classId))).isEqualTo("SUBMISSION_CLOSED");
        verify(f.audit).record(argThat(e -> e.action().equals("SUBMIT_REJECTED") && e.result() == AuditResult.FAILURE));
        f.window = SubmissionWindow.ON_TIME;
        f.attempt(1, AttemptStatus.SUBMITTED, NOW, false);
        assertThat(code(() -> f.attemptService.start(f.student, f.assignmentId, f.classId))).isEqualTo("ATTEMPTS_EXHAUSTED");
    }

    @Test
    void startReturnsOpenAttemptOrCreatesNextFromSkeleton() {
        f.view = f.view(AssignmentStatus.OPEN, SubmissionMode.INDIVIDUAL, GradingMode.GRADED, NOW.plus(Duration.ofDays(1)),
                NOW.plus(Duration.ofDays(3)), 3);
        f.attempt(1, AttemptStatus.SUBMITTED, NOW, false);
        AttemptService.Started started = f.attemptService.start(f.student, f.assignmentId, f.classId);
        assertThat(started.created()).isTrue();
        assertThat(started.attempt().attemptNo()).isEqualTo(2);
        assertThat(started.attempt().deadlineAt()).isEqualTo(NOW.plus(Duration.ofDays(3)));
        assertThat(started.attempt().content().blocks()).extracting(b -> b.id()).containsExactly(f.teacherBlock.id());
        AttemptService.Started again = f.attemptService.start(f.student, f.assignmentId, f.classId);
        assertThat(again.created()).isFalse();
        assertThat(again.attempt().attemptId()).isEqualTo(started.attempt().attemptId());
    }

    @Test
    void saveNeedsMatchingContentVersionAndDeadlinePlusGrace() {
        AssignmentAttempt a = f.attempt(1, AttemptStatus.IN_PROGRESS, NOW.minusSeconds(10), false);
        f.view = f.view(AssignmentStatus.OPEN, SubmissionMode.INDIVIDUAL, GradingMode.GRADED, NOW.minusSeconds(10), null, 2);
        assertThat(code(() -> f.attemptService.save(f.student, a.id(), 5, withStudentWork()))).isEqualTo("CONTENT_VERSION_CONFLICT");
        WorkViews.Saved saved = f.attemptService.save(f.student, a.id(), 0, withStudentWork());
        assertThat(saved.contentVersion()).isEqualTo(1);
        AssignmentAttempt late = f.attempt(2, AttemptStatus.IN_PROGRESS, NOW.minusSeconds(31), false);
        f.view = f.view(AssignmentStatus.OPEN, SubmissionMode.INDIVIDUAL, GradingMode.GRADED, NOW.minusSeconds(31), null, 2);
        assertThat(code(() -> f.attemptService.save(f.student, late.id(), 0, withStudentWork()))).isEqualTo("DEADLINE_PASSED");
    }

    @Test
    void teacherBlocksAreStoredFromSkeleton() {
        AssignmentAttempt a = f.attempt(1, AttemptStatus.IN_PROGRESS, NOW.plus(Duration.ofDays(1)), false);
        f.attemptService.save(f.student, a.id(), 0, withStudentWork());
        assertThat(f.store.get(a.id()).content().blocks()).contains(f.teacherBlock);
    }

    @Test
    void drawingOnTeacherDiagramIsKeptOtherFieldsComeFromSkeleton() {
        ObjectNode slotContent = JsonNodeFactory.instance.objectNode().put("diagramType", "CLASS").put("xml", "<mxfile/>")
                .put("alt", "Vẽ sơ đồ lớp");
        DocumentBlock slot = new DocumentBlock(UUID.randomUUID(), BlockOrigin.TEACHER, BlockType.DIAGRAM, slotContent);
        DocumentModel skeleton = new DocumentModel(List.of(f.teacherBlock, slot));
        when(f.assignments.getContent(any())).thenAnswer(i -> Optional.of(new AssignmentContent(f.view.assignmentId(), 1,
                "Hướng dẫn", skeleton, List.of(new DocumentPart("DOCUMENT", null, List.of(), "Toàn bài")), Map.of(),
                List.of())));
        AssignmentAttempt a = f.attempt(1, AttemptStatus.IN_PROGRESS, NOW.plus(Duration.ofDays(1)), false);
        ObjectNode drawn = slotContent.deepCopy().put("xml", "<mxfile><diagram id=\"p\"/></mxfile>")
                .put("svgPreview", "<svg/>").put("alt", "mô tả bị sửa");
        f.attemptService.save(f.student, a.id(), 0, new DocumentModel(List.of(f.teacherBlock,
                new DocumentBlock(slot.id(), BlockOrigin.TEACHER, BlockType.DIAGRAM, drawn))));
        DocumentBlock stored = f.store.get(a.id()).content().blocks().get(1);
        assertThat(stored.content().path("xml").asText()).isEqualTo("<mxfile><diagram id=\"p\"/></mxfile>");
        assertThat(stored.content().path("svgPreview").asText()).isEqualTo("<svg/>");
        assertThat(stored.content().path("alt").asText()).as("mô tả giữ theo đề").isEqualTo("Vẽ sơ đồ lớp");
    }

    @Test
    void submitValidatesThenStoresReceiptAndRepeatsSameReceipt() {
        AssignmentAttempt a = f.attempt(1, AttemptStatus.IN_PROGRESS, NOW.plus(Duration.ofDays(1)), false);
        f.submitResult = new ValidationResult(List.of(new ValidationIssue("REQUIRED_DIAGRAMS_MISSING", null, "Thiếu")), List.of());
        assertThat(code(() -> f.attemptService.submit(f.student, a.id(), 0))).isEqualTo("SUBMISSION_INVALID");
        f.submitResult = new ValidationResult(List.of(), List.of());
        WorkViews.Receipt receipt = f.attemptService.submit(f.student, a.id(), 0);
        assertThat(receipt.receiptHash()).hasSize(64);
        assertThat(receipt.late()).isFalse();
        assertThat(receipt.submitMode()).isEqualTo(SubmitMode.MANUAL);
        WorkViews.Receipt again = f.attemptService.submit(f.student, a.id(), 7);
        assertThat(again.receiptHash()).isEqualTo(receipt.receiptHash());
        verify(f.listener, times(1)).onSubmitted(a.id());
        verify(f.audit).record(argThat(e -> e.action().equals("ASSIGNMENT_SUBMITTED")));
    }

    @Test
    void submittingAfterClosesAtButBeforeLateUntilIsLate() {
        f.view = f.view(AssignmentStatus.OPEN, SubmissionMode.INDIVIDUAL, GradingMode.GRADED, NOW.minus(Duration.ofHours(1)),
                NOW.plus(Duration.ofDays(1)), 2);
        AssignmentAttempt a = f.attempt(1, AttemptStatus.IN_PROGRESS, NOW.plus(Duration.ofDays(1)), false);
        assertThat(f.attemptService.submit(f.student, a.id(), 0).late()).isTrue();
    }

    @Test
    void autoSubmitAtDeadlineKeepsWarningsAndRunsOnce() {
        AssignmentAttempt a = f.attempt(1, AttemptStatus.IN_PROGRESS, NOW.minus(Duration.ofMinutes(2)), false);
        f.view = f.view(AssignmentStatus.OPEN, SubmissionMode.INDIVIDUAL, GradingMode.GRADED, NOW.minus(Duration.ofMinutes(2)), null, 2);
        f.submitResult = new ValidationResult(List.of(new ValidationIssue("SUBMISSION_EMPTY", null, "Rỗng")), List.of());
        AutoSubmitScanner scanner = new AutoSubmitScanner(f.attempts, f.submitter, f.transactions);
        scanner.scan(NOW);
        scanner.scan(NOW);
        AssignmentAttempt submitted = f.store.get(a.id());
        assertThat(submitted.status()).isEqualTo(AttemptStatus.SUBMITTED);
        assertThat(submitted.submitMode()).isEqualTo(SubmitMode.AUTO_DEADLINE);
        assertThat(submitted.submitWarnings()).extracting(ValidationIssue::code).containsExactly("SUBMISSION_EMPTY");
        verify(f.listener, times(1)).onSubmitted(a.id());
    }

    @Test
    void extendedDeadlineMovesAttemptDeadlineInsteadOfSubmitting() {
        AssignmentAttempt a = f.attempt(1, AttemptStatus.IN_PROGRESS, NOW.minus(Duration.ofMinutes(5)), false);
        f.view = f.view(AssignmentStatus.OPEN, SubmissionMode.INDIVIDUAL, GradingMode.GRADED, NOW.plus(Duration.ofDays(2)), null, 2);
        assertThat(f.submitter.autoSubmit(a.id(), SubmitMode.AUTO_DEADLINE, NOW)).isFalse();
        assertThat(f.store.get(a.id()).deadlineAt()).isEqualTo(NOW.plus(Duration.ofDays(2)));
        verify(f.listener, never()).onSubmitted(any());
    }

    @Test
    void retiringAssignmentSubmitsAttemptsInProgress() {
        AssignmentAttempt a = f.attempt(1, AttemptStatus.IN_PROGRESS, NOW.plus(Duration.ofDays(1)), false);
        new AssignmentRetiredListener(f.attempts, f.submitter, Clock.fixed(NOW, ZoneOffset.UTC)).onRetired(f.assignmentId);
        assertThat(f.store.get(a.id()).submitMode()).isEqualTo(SubmitMode.AUTO_RETIRED);
        verify(f.listener).onSubmitted(a.id());
    }

    @Test
    void otherStudentsAttemptIsNotFound() {
        AssignmentAttempt a = f.attempt(1, AttemptStatus.IN_PROGRESS, NOW.plus(Duration.ofDays(1)), false);
        assertThat(code(() -> f.attemptService.get(new edu.aiplatform.identity.contract.Actor(java.util.UUID.randomUUID(),
                edu.aiplatform.identity.contract.Role.STUDENT), a.id()))).isEqualTo("NOT_FOUND");
    }
}
