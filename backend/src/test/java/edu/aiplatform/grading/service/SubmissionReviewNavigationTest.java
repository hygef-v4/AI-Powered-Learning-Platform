package edu.aiplatform.grading.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.academics.contract.*;
import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.assignmentwork.contract.*;
import edu.aiplatform.grading.model.*;
import edu.aiplatform.grading.contract.*;
import edu.aiplatform.grading.repository.IEvaluationRepository;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class SubmissionReviewNavigationTest {
    private final Actor actor = new Actor(UUID.randomUUID(), Role.TEACHER);
    private final UUID classId = UUID.randomUUID(), assignmentId = UUID.randomUUID();
    private final IEvaluationRepository evaluationRepository = mock(IEvaluationRepository.class);
    private final GradingAccess gradingAccess = mock(GradingAccess.class);
    private final IAssignmentQuery assignmentQuery = mock(IAssignmentQuery.class);
    private final ISubmissionQuery submissionQuery = mock(ISubmissionQuery.class);
    private final IClassAccess classAccess = mock(IClassAccess.class);
    private final IAccountLookup accountLookup = mock(IAccountLookup.class);
    private final IGroupSubmissionSource groupSource = mock(IGroupSubmissionSource.class);
    private final ObjectProvider<IGroupSubmissionSource> groupSubmissionSourceProvider = mock(ObjectProvider.class);
    private final SubmissionReviewService submissionReviewService = new SubmissionReviewService(evaluationRepository, gradingAccess, assignmentQuery,
            submissionQuery, classAccess, accountLookup, mock(IActivityQuery.class), groupSubmissionSourceProvider, new ObjectMapper());

    private List<Evaluation> fixture() {
        var assignment = mock(AssignmentView.class);
        when(assignment.gradingMode()).thenReturn(GradingMode.GRADED);
        when(assignment.scope()).thenReturn(AssignmentScope.CLASS);
        when(assignment.classId()).thenReturn(classId);
        when(assignment.submissionMode()).thenReturn(SubmissionMode.INDIVIDUAL);
        when(assignmentQuery.get(assignmentId)).thenReturn(Optional.of(assignment));
        when(classAccess.getClassRef(classId)).thenReturn(Optional.of(new ClassRef(classId, UUID.randomUUID(),
                "C", "Class", ClassStatus.OPEN, actor.accountId(), false)));
        var values = new ArrayList<Evaluation>();
        var statuses = new ArrayList<StudentSubmissionStatus>();
        for (int i = 0; i < 4; i++) {
            var evaluation = Evaluation.createPendingEvaluation(UUID.randomUUID(), assignmentId, classId, UUID.randomUUID(),
                    "hash", BigDecimal.TEN, "[]", Instant.now());
            evaluation.state = i == 2 ? EvaluationStatus.DRAFT : i == 3 ? EvaluationStatus.PUBLISHED : EvaluationStatus.PENDING;
            values.add(evaluation);
            statuses.add(new StudentSubmissionStatus(evaluation.accountId, evaluation.attemptId, 1,
                    AttemptStatus.SUBMITTED, Instant.now(), i != 1));
            when(evaluationRepository.findById(evaluation.id)).thenReturn(Optional.of(evaluation));
            when(submissionQuery.getSubmission(evaluation.attemptId)).thenReturn(Optional.of(new SubmissionView(
                    evaluation.attemptId, assignmentId, classId, evaluation.accountId, new DocumentModel(List.of()),
                    "hash", Instant.now(), i != 1, null)));
        }
        when(classAccess.listActiveStudents(classId)).thenReturn(values.stream().map(e -> e.accountId).toList());
        when(evaluationRepository.findForAssignmentAndClass(assignmentId, classId)).thenReturn(values);
        when(submissionQuery.listSubmissions(assignmentId, classId)).thenReturn(statuses);
        return values;
    }

    @Test void filteredNavigationUsesListingOrderAndExcludesOtherStatesAndLateValues() {
        var values = fixture();
        assertThat(submissionReviewService.loadEvaluation(actor, values.get(0).id, "PENDING", true).navigationIds())
                .containsExactly(values.get(0).id);
        assertThat(submissionReviewService.loadEvaluation(actor, values.get(0).id, "PENDING", null).navigationIds())
                .containsExactly(values.get(0).id, values.get(1).id);
        assertThat(submissionReviewService.loadEvaluation(actor, values.get(0).id, "ALL", false).navigationIds())
                .containsExactly(values.get(1).id);
    }

    @Test void unfilteredCallPreservesLegacyOrderAndInvalidFilterIsRejected() {
        var values = fixture();
        assertThat(submissionReviewService.loadEvaluation(actor, values.get(0).id).navigationIds())
                .containsExactlyElementsOf(values.stream().map(e -> e.id).sorted().toList());
        assertThatThrownBy(() -> submissionReviewService.loadEvaluation(actor, values.get(0).id, "WRONG", null))
                .isInstanceOf(ApiException.class).hasMessageContaining("Unknown status filter");
    }

    @Test void removedStudentsAreExcludedAndDeniedScopeCannotReadNavigation() {
        var values = fixture();
        when(classAccess.listActiveStudents(classId)).thenReturn(List.of(values.get(0).accountId));
        assertThat(submissionReviewService.loadEvaluation(actor, values.get(0).id, "ALL", null).navigationIds()).containsExactly(values.get(0).id);
        doThrow(ApiException.notFound("GRADE_NOT_FOUND")).when(gradingAccess).requireTeacher(actor, classId, Actions.GRADING_REVIEW);
        assertThatThrownBy(() -> submissionReviewService.loadEvaluation(actor, values.get(0).id, "ALL", null)).isInstanceOf(ApiException.class);
    }

    @Test void groupNavigationIncludesOnlyFilteredRootsAndNeverMembers() {
        var values = fixture();
        var assignment = assignmentQuery.get(assignmentId).orElseThrow();
        when(assignment.submissionMode()).thenReturn(SubmissionMode.GROUP);
        when(groupSubmissionSourceProvider.getIfAvailable()).thenReturn(groupSource);
        var root = values.get(0);
        root.kind = EvaluationKind.GROUP_DOCUMENT;
        root.groupDocumentId = UUID.randomUUID();
        var otherRoot = values.get(1);
        otherRoot.kind = EvaluationKind.GROUP_DOCUMENT;
        otherRoot.groupDocumentId = UUID.randomUUID();
        values.get(2).kind = EvaluationKind.MEMBER;
        values.get(2).groupDocumentId = root.groupDocumentId;
        when(groupSource.getGroupSubmission(root.groupDocumentId)).thenReturn(Optional.of(new GroupSubmissionView(
                root.groupDocumentId, UUID.randomUUID(), assignmentId, classId, new DocumentModel(List.of()),
                List.of(values.get(2).accountId), List.of(), Instant.now(), true, "hash")));
        when(groupSource.listGroupSubmissions(assignmentId, classId)).thenReturn(List.of(
                new GroupSubmissionStatus(root.groupDocumentId, UUID.randomUUID(), "Late group", true, Instant.now(), true),
                new GroupSubmissionStatus(otherRoot.groupDocumentId, UUID.randomUUID(), "On-time group", true, Instant.now(), false)));
        assertThat(submissionReviewService.loadEvaluation(actor, root.id, "PENDING", true).navigationIds()).containsExactly(root.id);
        assertThat(submissionReviewService.loadEvaluation(actor, root.id, "PENDING", null).navigationIds()).containsExactly(root.id, otherRoot.id);
    }
}
