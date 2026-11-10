package edu.aiplatform.grading.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.aiplatform.academics.contract.*;
import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.assignmentwork.contract.ISubmissionQuery;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.grading.model.*;
import edu.aiplatform.grading.repository.*;
import edu.aiplatform.identity.contract.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class GradeDistributionPrivacyTest {
    @Test void optInRemainsPerClassAndRequiresEnrollmentPublishedUsableScoresAndMinimumBuckets() {
        var classId = UUID.randomUUID();
        var otherClass = UUID.randomUUID();
        var assignmentId = UUID.randomUUID();
        var subjectId = UUID.randomUUID();
        var students = new ArrayList<UUID>();
        var values = new ArrayList<Evaluation>();
        for (int i = 0; i < 20; i++) {
            var studentId = UUID.randomUUID();
            students.add(studentId);
            var evaluation = Evaluation.createPendingEvaluation(UUID.randomUUID(), assignmentId, classId, studentId,
                    "hash", BigDecimal.TEN, "[]", Instant.now());
            evaluation.state = EvaluationStatus.PUBLISHED;
            evaluation.score = i < 4 ? BigDecimal.ONE : BigDecimal.TEN;
            values.add(evaluation);
        }
        var actor = new Actor(students.get(0), Role.STUDENT);
        var auth = mock(IAuthorization.class);
        var classes = mock(IClassAccess.class);
        var audit = mock(IAudit.class);
        var assignments = mock(IAssignmentQuery.class);
        var evaluations = mock(IEvaluationRepository.class);
        var visibility = mock(GradeDistributionVisibilityRepository.class);
        when(auth.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.allow());
        when(classes.isActiveStudent(actor.accountId(), classId)).thenReturn(true);
        for (var id : List.of(classId, otherClass)) {
            when(classes.getClassRef(id)).thenReturn(Optional.of(new ClassRef(id, subjectId, "C", "Class",
                    ClassStatus.OPEN, UUID.randomUUID(), true)));
            when(classes.listActiveStudents(id)).thenReturn(students);
        }
        when(classes.isActiveStudent(actor.accountId(), otherClass)).thenReturn(true);
        when(assignments.get(assignmentId)).thenReturn(Optional.of(new AssignmentView(assignmentId,
                AssignmentScope.SUBJECT, null, subjectId, 1, "Task", SubmissionMode.INDIVIDUAL, GradingMode.GRADED,
                false, AssignmentStatus.OPEN, null, null, null, 1, false, null, List.of())));
        when(evaluations.findForAssignmentAndClass(assignmentId, classId)).thenReturn(values);
        when(visibility.findVisibility(any(), eq(assignmentId))).thenReturn(new GradeDistributionVisibilityRepository.Visibility(false, 0));
        var service = new GradebookService(evaluations, new GradingAccess(auth, classes, audit, Clock.systemUTC()),
                assignments, mock(ISubmissionQuery.class), classes, mock(IAccountLookup.class), mock(IRubric.class),
                audit, Clock.systemUTC(), mock(ObjectProvider.class), visibility);
        assertThat(service.loadDistribution(actor, classId, assignmentId, true)).isEmpty();
        when(visibility.findVisibility(classId, assignmentId)).thenReturn(new GradeDistributionVisibilityRepository.Visibility(true, 1));
        assertThat(service.loadDistribution(actor, classId, assignmentId, true)).singleElement()
                .satisfies(bucket -> assertThat(bucket.count()).isEqualTo(16));
        assertThat(service.loadDistribution(actor, otherClass, assignmentId, true)).isEmpty();
        values.get(values.size() - 1).state = EvaluationStatus.FINALIZED;
        assertThat(service.loadDistribution(actor, classId, assignmentId, true)).isEmpty();
        values.get(values.size() - 1).state = EvaluationStatus.PUBLISHED;
        values.get(values.size() - 1).maxScore = BigDecimal.ZERO;
        assertThat(service.loadDistribution(actor, classId, assignmentId, true)).isEmpty();
        when(classes.isActiveStudent(actor.accountId(), classId)).thenReturn(false);
        assertThatThrownBy(() -> service.loadDistribution(actor, classId, assignmentId, true)).isInstanceOf(RuntimeException.class);
    }
}
