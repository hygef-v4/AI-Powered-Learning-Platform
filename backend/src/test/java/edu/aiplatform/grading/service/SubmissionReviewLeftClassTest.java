package edu.aiplatform.grading.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.academics.contract.*;
import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.assignmentwork.contract.*;
import edu.aiplatform.grading.dto.GradingViews.SubmissionRow;
import edu.aiplatform.grading.model.Evaluation;
import edu.aiplatform.grading.repository.IEvaluationRepository;
import edu.aiplatform.identity.contract.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/** KAN-251: a student removed from the class keeps their earlier work visible to the teacher (BR-U05-25). */
class SubmissionReviewLeftClassTest {
    private final Actor actor = new Actor(UUID.randomUUID(), Role.TEACHER);
    private final UUID classId = UUID.randomUUID();
    private final UUID assignmentId = UUID.randomUUID();
    private final UUID enrolledId = UUID.randomUUID();
    private final UUID removedId = UUID.randomUUID();
    private final IAccountLookup accountLookup = mock(IAccountLookup.class);
    private final IAssignmentQuery assignmentQuery = mock(IAssignmentQuery.class);
    private final IEvaluationRepository evaluationRepository = mock(IEvaluationRepository.class);
    private final ISubmissionQuery submissionQuery = mock(ISubmissionQuery.class);
    private final IClassAccess classAccess = mock(IClassAccess.class);
    private Evaluation removedEvaluation;
    private SubmissionReviewService submissionReviewService;

    @BeforeEach
    void prepareClassWithRemovedStudent() {
        var subjectId = UUID.randomUUID();
        var assignment = mock(AssignmentView.class);
        when(assignment.assignmentId()).thenReturn(assignmentId);
        when(assignment.gradingMode()).thenReturn(GradingMode.GRADED);
        when(assignment.scope()).thenReturn(AssignmentScope.CLASS);
        when(assignment.classId()).thenReturn(classId);
        when(assignment.subjectId()).thenReturn(subjectId);
        when(assignment.status()).thenReturn(AssignmentStatus.CLOSED);
        when(assignment.submissionMode()).thenReturn(SubmissionMode.INDIVIDUAL);
        when(assignmentQuery.loadAssignment(assignmentId)).thenReturn(Optional.of(assignment));
        when(assignmentQuery.listForClass(classId)).thenReturn(List.of(assignment));
        when(classAccess.getClassRef(classId)).thenReturn(Optional.of(
                new ClassRef(classId, subjectId, "C", "Class", ClassStatus.OPEN, actor.accountId(), false)));
        when(classAccess.listActiveStudents(classId)).thenReturn(List.of(enrolledId));
        removedEvaluation = Evaluation.createPendingEvaluation(UUID.randomUUID(), assignmentId, classId, removedId,
                "hash", BigDecimal.TEN, "[]", Instant.now());
        when(evaluationRepository.findForAssignmentAndClass(assignmentId, classId))
                .thenReturn(List.of(removedEvaluation));
        when(evaluationRepository.findForClass(classId)).thenReturn(List.of(removedEvaluation));
        // ISubmissionQuery already returns students who left but still have attempts.
        when(submissionQuery.listSubmissions(assignmentId, classId)).thenReturn(List.of(
                new StudentSubmissionStatus(removedId, removedEvaluation.attemptId, 1, AttemptStatus.SUBMITTED,
                        Instant.now(), false)));
        when(accountLookup.getContacts(List.of(enrolledId))).thenReturn(Map.of(enrolledId,
                new AccountContact(enrolledId, "sv1@fpt.edu.vn", "Student One", Role.STUDENT,
                        AccountStatus.ACTIVE, "ST100001")));
        when(accountLookup.getContacts(List.of(removedId))).thenReturn(Map.of(removedId,
                new AccountContact(removedId, "sv4@fpt.edu.vn", "Student Four", Role.STUDENT,
                        AccountStatus.ACTIVE, "ST100004")));
        submissionReviewService = new SubmissionReviewService(evaluationRepository, mock(GradingAccess.class),
                assignmentQuery, submissionQuery, classAccess, accountLookup, mock(IActivityQuery.class),
                mock(ObjectProvider.class), new ObjectMapper());
    }

    @Test
    void listsRemovedStudentWithSubmissionAndMarksThemAsLeftClass() {
        var rows = submissionReviewService.listRows(actor, classId, assignmentId, "ALL", null).rows();

        assertThat(rows).extracting(row -> row.student().accountId()).containsExactly(enrolledId, removedId);
        SubmissionRow removed = rows.get(1);
        assertThat(removed.leftClass()).isTrue();
        assertThat(removed.student().name()).isEqualTo("Student Four");
        assertThat(removed.evaluationId()).isEqualTo(removedEvaluation.id);
        assertThat(removed.status()).isEqualTo("PENDING");
        assertThat(rows.get(0).leftClass()).isFalse();
        assertThat(rows.get(0).status()).isEqualTo("NOT_SUBMITTED");
    }

    @Test
    void countsRemovedStudentInGradingProgress() {
        var progress = submissionReviewService.loadProgress(actor, classId).get(assignmentId);

        assertThat(progress.total()).isEqualTo(2);
        assertThat(progress.summary()).containsEntry("submitted", 1L).containsEntry("notSubmitted", 1L);
    }
}
