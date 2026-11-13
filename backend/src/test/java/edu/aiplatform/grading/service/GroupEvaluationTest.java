package edu.aiplatform.grading.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import edu.aiplatform.grading.model.*;
import edu.aiplatform.grading.repository.*;
import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.assignmentwork.contract.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;

class GroupEvaluationTest {
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = EvaluationStatus.class, names = {"DRAFT", "FINALIZED", "PUBLISHED"})
    void resubmissionPreservesOfficialGradesAndResetsDrafts(EvaluationStatus state) {
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
        var root = Evaluation.createPendingEvaluation(null, aid, cid, null, "old", BigDecimal.TEN, "[]", Instant.now());
        root.kind = EvaluationKind.GROUP_DOCUMENT;
        root.groupDocumentId = doc;
        root.score = BigDecimal.ONE;
        root.state = state;
        var member = Evaluation.createPendingEvaluation(null, aid, cid, sid, "old", BigDecimal.TEN, "[]", Instant.now());
        member.kind = EvaluationKind.MEMBER;
        member.groupDocumentId = doc;
        member.score = BigDecimal.ONE;
        member.state = state;
        root.publishedAt = Instant.now();
        member.publishedAt = Instant.now();
        root.feedback = "Old feedback";
        root.adjustment = BigDecimal.ONE;
        root.rubricChecks = "{\"old\":[]}";
        var previousGroupMember = Evaluation.createPendingEvaluation(null, aid, cid, sid,
                "other", BigDecimal.TEN, "[]", Instant.now().minusSeconds(10));
        previousGroupMember.kind = EvaluationKind.MEMBER;
        previousGroupMember.groupDocumentId = UUID.randomUUID();
        when(repo.lockById(previousGroupMember.id)).thenReturn(Optional.of(previousGroupMember));
        when(repo.findForAssignmentAndClass(aid, cid)).thenReturn(List.of(root, member, previousGroupMember));
        when(repo.lockById(root.id)).thenReturn(Optional.of(root));
        when(repo.lockById(member.id)).thenReturn(Optional.of(member));
        var service = new EvaluationLifecycleService(repo, mock(ISubmissionQuery.class), assignments, rubric,
                new ObjectMapper().findAndRegisterModules(), Clock.systemUTC(), classes);
        service.onGroupSubmitted(doc, "new", aid, cid, List.of(sid));
        if (state != EvaluationStatus.DRAFT) {
            assertThat(root.state).isEqualTo(state);
            assertThat(member.state).isEqualTo(state);
            assertThat(root.submissionHash).isEqualTo("old");
            assertThat(root.score).isEqualByComparingTo("1");
            assertThat(member.score).isEqualByComparingTo("1");
            verify(repo, never()).save(any());
            return;
        }
        assertThat(root.state).isEqualTo(EvaluationStatus.PENDING);
        assertThat(root.submissionHash).isEqualTo("new");
        assertThat(root.score).isNull();
        assertThat(root.history()).hasSize(1);
        assertThat(member.state).isEqualTo(EvaluationStatus.PENDING);
        assertThat(member.score).isNull();
        assertThat(member.publishedAt).isNull();
        assertThat(root.publishedAt).isNull();
        assertThat(root.rubricItemScores).isEmpty();
        assertThat(root.structuredFeedback).isNull();
        assertThat(root.feedback).isNull();
        assertThat(root.adjustment).isEqualByComparingTo("0");
        assertThat(root.rubricChecks).isEqualTo("{}");
        verify(repo).save(argThat(e -> e.id.equals(root.id)));
        verify(repo).save(argThat(e -> e.id.equals(member.id)));
        assertThat(previousGroupMember.state).isEqualTo(EvaluationStatus.SUPERSEDED);
        verify(repo, times(3)).save(any());
    }
}
