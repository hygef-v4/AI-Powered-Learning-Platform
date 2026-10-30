package edu.aiplatform.assignmentwork.service;

import edu.aiplatform.assignmentwork.dto.WorkViews;

import static edu.aiplatform.assignmentwork.service.WorkFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.AssignmentView;
import edu.aiplatform.assignments.contract.DocumentPart;
import edu.aiplatform.assignments.contract.RubricView;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignmentwork.contract.AttemptStatus;
import edu.aiplatform.assignmentwork.contract.StudentSubmissionStatus;
import edu.aiplatform.assignmentwork.model.AssignmentAttempt;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** F1, F2, F8, F13; BR-U08-01…03, 30…32; BR-U07-37. */
class StudentAssignmentServiceTest {

    private final WorkFixtures f = new WorkFixtures();

    private AssignmentView other(UUID id, AssignmentStatus status, Instant closesAt) {
        return new AssignmentView(id, AssignmentScope.SUBJECT, null, f.subjectId, 1, "Bài " + status, SubmissionMode.INDIVIDUAL,
                GradingMode.GRADED, false, status, NOW.minus(Duration.ofDays(3)), closesAt, null, 1, false, null, List.of());
    }

    @Test
    void listShowsOnlyVisibleAssignmentsSortedByDeadline() {
        UUID draft = UUID.randomUUID();
        UUID retired = UUID.randomUUID();
        UUID closed = UUID.randomUUID();
        when(f.assignments.listForClass(any())).thenReturn(List.of(f.view,
                other(draft, AssignmentStatus.SCHEDULED, NOW.plus(Duration.ofDays(9))),
                other(retired, AssignmentStatus.RETIRED, NOW.plus(Duration.ofDays(9))),
                other(closed, AssignmentStatus.CLOSED, NOW.minus(Duration.ofDays(1)))));
        List<WorkViews.Row> rows = f.studentAssignments.list(f.student, null);
        assertThat(rows).extracting(WorkViews.Row::assignmentId).containsExactly(closed, f.assignmentId);
        assertThat(rows).extracting(WorkViews.Row::myStatus).containsExactly("MISSED", "NOT_STARTED");
        assertThat(rows.get(0).scope()).isEqualTo(AssignmentScope.SUBJECT);
    }

    @Test
    void partCountIsTheNumberOfTopLevelBranchesWithARubric() {
        UUID chapter1 = UUID.randomUUID();
        UUID chapter2 = UUID.randomUUID();
        UUID leaf1 = UUID.randomUUID();
        UUID leaf2 = UUID.randomUUID();
        UUID leaf3 = UUID.randomUUID();
        UUID appendix = UUID.randomUUID();
        // Cây: Chương 1 › (1.1, 1.2), Chương 2 › 2.1 › a, Phụ lục (không có con) → 3 nhánh con trực tiếp của gốc.
        List<DocumentPart> parts = List.of(
                new DocumentPart(leaf1.toString(), leaf1, List.of(chapter1), "Chương 1 › 1.1"),
                new DocumentPart(leaf2.toString(), leaf2, List.of(chapter1), "Chương 1 › 1.2"),
                new DocumentPart(leaf3.toString(), leaf3, List.of(chapter2, UUID.randomUUID()), "Chương 2 › 2.1 › a"),
                new DocumentPart(appendix.toString(), appendix, List.of(), "Phụ lục"));
        // Rubric ở 1.1, 1.2 và 2.1 › a; Phụ lục không có rubric nên không tính.
        List<RubricView> rubrics = List.of(rubric(leaf1), rubric(leaf2), rubric(leaf3));
        assertThat(StudentAssignmentService.topLevelParts(parts, rubrics)).isEqualTo(2);
        assertThat(StudentAssignmentService.topLevelParts(parts, List.of())).isZero();
        DocumentPart whole = new DocumentPart("DOCUMENT", null, List.of(), "Toàn bài");
        assertThat(StudentAssignmentService.topLevelParts(List.of(whole),
                List.of(new RubricView(UUID.randomUUID(), f.assignmentId, "DOCUMENT", List.of(), BigDecimal.TEN)))).isEqualTo(1);
    }

    private RubricView rubric(UUID partId) {
        return new RubricView(UUID.randomUUID(), f.assignmentId, partId.toString(), List.of(), BigDecimal.TEN);
    }

    @Test
    void statusReflectsAttempts() {
        f.attempt(1, AttemptStatus.SUBMITTED, NOW, true);
        assertThat(f.studentAssignments.list(f.student, f.classId).get(0).myStatus()).isEqualTo("SUBMITTED_LATE");
        AssignmentAttempt working = f.attempt(2, AttemptStatus.IN_PROGRESS, NOW.plus(Duration.ofDays(1)), false);
        WorkViews.Row row = f.studentAssignments.list(f.student, f.classId).get(0);
        assertThat(row.myStatus()).isEqualTo("IN_PROGRESS");
        assertThat(row.inProgressAttemptId()).isEqualTo(working.id());
        assertThat(row.submittedCount()).isEqualTo(1);
    }

    @Test
    void detailExplainsWhyStartIsBlockedAndMarksGradedAttempt() {
        f.attempt(1, AttemptStatus.SUBMITTED, NOW, false);
        AssignmentAttempt last = f.attempt(2, AttemptStatus.SUBMITTED, NOW, false);
        WorkViews.Detail detail = f.studentAssignments.detail(f.student, f.assignmentId, f.classId);
        assertThat(detail.canStart()).isFalse();
        assertThat(detail.startBlockedReason()).isEqualTo("ATTEMPTS_EXHAUSTED");
        assertThat(detail.latestAttempt().attemptId()).isEqualTo(last.id());
        assertThat(f.studentAssignments.history(f.student, f.assignmentId, f.classId))
                .filteredOn(WorkViews.AttemptSummary::graded).extracting(WorkViews.AttemptSummary::attemptId)
                .containsExactly(last.id());
    }

    @Test
    void teachersCannotUseStudentScreens() {
        try {
            f.studentAssignments.list(new Actor(UUID.randomUUID(), Role.TEACHER), null);
            throw new AssertionError("Cần 404");
        } catch (ApiException e) {
            assertThat(e.code()).isEqualTo("NOT_FOUND");
        }
    }

    @Test
    void submissionQueryGivesGradedAttemptAndMissingStudents() {
        SubmissionQueryService query = new SubmissionQueryService(f.attempts, f.assignments, f.classes, f.groupWork);
        UUID absent = UUID.randomUUID();
        when(f.classes.listActiveStudents(f.classId)).thenReturn(List.of(f.student.accountId(), absent));
        f.attempt(1, AttemptStatus.SUBMITTED, NOW, false);
        AssignmentAttempt last = f.attempt(2, AttemptStatus.SUBMITTED, NOW, true);
        List<StudentSubmissionStatus> statuses = query.listSubmissions(f.assignmentId, f.classId);
        assertThat(statuses).filteredOn(s -> s.accountId().equals(f.student.accountId())).singleElement().satisfies(s -> {
            assertThat(s.gradedAttemptId()).isEqualTo(last.id());
            assertThat(s.attemptCount()).isEqualTo(2);
            assertThat(s.late()).isTrue();
        });
        assertThat(query.listStudentsWithoutSubmission(f.assignmentId, f.classId)).containsExactly(absent);
        AssignmentAttempt working = f.attempt(3, AttemptStatus.IN_PROGRESS, NOW.plus(Duration.ofDays(1)), false);
        assertThat(query.getSubmission(working.id())).isEmpty();
        assertThat(query.getSubmission(last.id())).isPresent();
    }
}
