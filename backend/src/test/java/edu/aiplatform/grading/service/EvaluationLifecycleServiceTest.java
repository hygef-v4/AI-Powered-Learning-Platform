package edu.aiplatform.grading.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import edu.aiplatform.grading.model.*;
import edu.aiplatform.grading.repository.*;
import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.assignmentwork.contract.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class EvaluationLifecycleServiceTest {
    @Test
    void practiceCreatesNothingAndGradedListenerIsIdempotent() {
        var repo = mock(IEvaluationRepository.class);
        var sub = mock(ISubmissionQuery.class);
        var assignments = mock(IAssignmentQuery.class);
        var rubric = mock(IRubric.class);
        var id = UUID.randomUUID();
        var aid = UUID.randomUUID();
        var cid = UUID.randomUUID();
        var sid = UUID.randomUUID();
        when(sub.getSubmission(id)).thenReturn(Optional.of(new SubmissionView(id, aid, cid, sid,
                new DocumentModel(List.of()), "hash", Instant.now(), false, SubmitMode.values()[0])));
        var a = mock(AssignmentView.class);
        when(a.assignmentId()).thenReturn(aid);
        when(a.gradingMode()).thenReturn(GradingMode.PRACTICE);
        when(assignments.get(aid)).thenReturn(Optional.of(a));
        var service = new EvaluationLifecycleService(repo, sub, assignments, rubric,
                new ObjectMapper().findAndRegisterModules(), Clock.systemUTC(),
                mock(edu.aiplatform.academics.contract.IClassAccess.class));
        service.onSubmitted(id);
        verify(repo, never()).save(any());
        when(a.gradingMode()).thenReturn(GradingMode.GRADED);
        when(rubric.maxPoints(aid)).thenReturn(BigDecimal.TEN);
        when(rubric.listRubrics(aid)).thenReturn(List.of());
        when(repo.findForAssignmentAndClass(aid, cid)).thenReturn(List.of());
        service.onSubmitted(id);
        verify(repo).save(argThat(e -> e.attemptId.equals(id) && e.state == EvaluationStatus.PENDING));
        when(repo.findByAttemptId(id)).thenReturn(
                Optional.of(Evaluation.createPendingEvaluation(id, aid, cid, sid, "hash", BigDecimal.TEN, "[]", Instant.now())));
        service.onSubmitted(id);
        verify(repo, times(1)).save(any());
        for (var state : List.of(EvaluationStatus.FINALIZED, EvaluationStatus.PUBLISHED)) {
            clearInvocations(repo);
            var previous = Evaluation.createPendingEvaluation(UUID.randomUUID(), aid, cid, sid, "old", BigDecimal.TEN, "[]", Instant.now());
            previous.state = state;
            previous.score = new BigDecimal("7.50");
            when(repo.findByAttemptId(id)).thenReturn(Optional.empty());
            when(repo.findForAssignmentAndClass(aid, cid)).thenReturn(List.of(previous));
            when(repo.lockById(previous.id)).thenReturn(Optional.of(previous));
            service.onSubmitted(id);
            assertThat(previous.state).isEqualTo(EvaluationStatus.SUPERSEDED);
            verify(repo).save(argThat(e -> !e.id.equals(previous.id) && e.attemptId.equals(id)
                    && e.state == EvaluationStatus.PENDING && e.score == null));
        }
    }
}
