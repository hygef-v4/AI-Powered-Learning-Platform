package edu.aiplatform.grading.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import edu.aiplatform.grading.model.*;
import edu.aiplatform.grading.repository.*;
import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.assignmentwork.contract.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;

class GroupEvaluationTest {
    @Test
    void resubmissionReusesGroupIdentityAndResetsGradesWithHistory() {
        var repo = mock(IEvaluationRepository.class);
        var assignments = mock(IAssignmentQuery.class);
        var rubric = mock(IRubric.class);
        var classes = mock(edu.aiplatform.academics.contract.IClassAccess.class);
        var subjectId = UUID.randomUUID();
        var aid = UUID.randomUUID();
        var cid = UUID.randomUUID();
        var doc = UUID.randomUUID();
        var sid = UUID.randomUUID();
        var a = mock(AssignmentView.class);
        when(a.gradingMode()).thenReturn(GradingMode.GRADED);
        when(a.submissionMode()).thenReturn(SubmissionMode.GROUP);
        when(a.scope()).thenReturn(AssignmentScope.SUBJECT);
        when(a.subjectId()).thenReturn(subjectId);
        when(classes.getClassRef(cid)).thenReturn(Optional
                .of(new edu.aiplatform.academics.contract.ClassRef(cid, subjectId, "C", "Class", null, null, false)));
        when(assignments.get(aid)).thenReturn(Optional.of(a));
        when(rubric.maxPoints(aid)).thenReturn(BigDecimal.TEN);
        when(rubric.listRubrics(aid)).thenReturn(List.of());
        var root = Evaluation.pending(null, aid, cid, null, "old", BigDecimal.TEN, "[]", Instant.now());
        root.kind = EvaluationKind.GROUP_DOCUMENT;
        root.groupDocumentId = doc;
        root.score = BigDecimal.ONE;
        root.state = EvaluationStatus.PUBLISHED;
        var member = Evaluation.pending(null, aid, cid, sid, "old", BigDecimal.TEN, "[]", Instant.now());
        member.kind = EvaluationKind.MEMBER;
        member.groupDocumentId = doc;
        member.score = BigDecimal.ONE;
        member.state = EvaluationStatus.PUBLISHED;
        when(repo.findForAssignmentAndClass(aid, cid)).thenReturn(List.of(root, member));
        when(repo.lockById(root.id)).thenReturn(Optional.of(root));
        when(repo.lockById(member.id)).thenReturn(Optional.of(member));
        var service = new EvaluationLifecycleService(repo, mock(ISubmissionQuery.class), assignments, rubric,
                new ObjectMapper().findAndRegisterModules(), Clock.systemUTC(), classes);
        service.onGroupSubmitted(doc, "new", aid, cid, List.of(sid));
        assertThat(root.state).isEqualTo(EvaluationStatus.PENDING);
        assertThat(root.submissionHash).isEqualTo("new");
        assertThat(root.score).isNull();
        assertThat(root.history()).hasSize(1);
        assertThat(member.state).isEqualTo(EvaluationStatus.PENDING);
        verify(repo).save(argThat(e -> e.id.equals(root.id)));
        verify(repo).save(argThat(e -> e.id.equals(member.id)));
        verify(repo, times(2)).save(any());
    }
}
