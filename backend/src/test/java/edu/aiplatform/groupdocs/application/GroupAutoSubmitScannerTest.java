package edu.aiplatform.groupdocs.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.SubmissionWindow;
import edu.aiplatform.groupdocs.domain.GroupDocumentRepository;
import edu.aiplatform.groupdocs.domain.SectionRules;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GroupAutoSubmitScannerTest {

    private static final Instant NOW = Instant.parse("2026-10-24T03:00:00Z");
    private final GroupDocumentRepository documents = mock(GroupDocumentRepository.class);
    private final GroupSubmissionService submissions = mock(GroupSubmissionService.class);
    private final IAssignmentQuery assignments = mock(IAssignmentQuery.class);
    private final GroupAutoSubmitScanner scanner = new GroupAutoSubmitScanner(documents, submissions, assignments);
    private final UUID assignmentId = UUID.randomUUID();
    private final UUID documentId = UUID.randomUUID();

    @Test
    void justOpenedAssignmentIsNotTreatedAsOverdue() {
        when(documents.listInProgressAssignments()).thenReturn(List.of(assignmentId));
        // 30 giây trước bài chưa mở (CLOSED) nhưng lúc này đang nhận bài.
        when(assignments.submissionWindow(eq(assignmentId), eq(NOW))).thenReturn(SubmissionWindow.ON_TIME);
        when(assignments.submissionWindow(eq(assignmentId), eq(NOW.minus(SectionRules.GRACE)))).thenReturn(SubmissionWindow.CLOSED);
        when(documents.listDueForAutoSubmit(List.of())).thenReturn(List.of());

        scanner.scan(NOW);

        verify(documents).listDueForAutoSubmit(List.of());
        verify(submissions, never()).autoSubmit(any());
    }

    @Test
    void assignmentPastDeadlineAndGraceIsAutoSubmitted() {
        when(documents.listInProgressAssignments()).thenReturn(List.of(assignmentId));
        when(assignments.submissionWindow(eq(assignmentId), any())).thenReturn(SubmissionWindow.CLOSED);
        when(documents.listDueForAutoSubmit(List.of(assignmentId))).thenReturn(List.of(documentId));

        scanner.scan(NOW);

        verify(submissions).autoSubmit(documentId);
    }

    @Test
    void lastDraftGraceKeepsDocumentOpenRightAfterDeadline() {
        when(documents.listInProgressAssignments()).thenReturn(List.of(assignmentId));
        when(assignments.submissionWindow(eq(assignmentId), eq(NOW))).thenReturn(SubmissionWindow.CLOSED);
        when(assignments.submissionWindow(eq(assignmentId), eq(NOW.minus(SectionRules.GRACE)))).thenReturn(SubmissionWindow.ON_TIME);
        when(documents.listDueForAutoSubmit(List.of())).thenReturn(List.of());

        scanner.scan(NOW);

        verify(submissions, never()).autoSubmit(any());
    }
}
