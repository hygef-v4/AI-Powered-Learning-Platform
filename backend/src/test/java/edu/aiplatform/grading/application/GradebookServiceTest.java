package edu.aiplatform.grading.application;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import edu.aiplatform.grading.domain.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.academics.contract.*;
import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.assignmentwork.contract.*;
import edu.aiplatform.audit.contract.*;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;

class GradebookServiceTest {
    @Test
    void studentCannotSeeDraftOrAnotherStudentsGrades() {
        var repo = mock(IEvaluationRepository.class);
        var auth = mock(IAuthorization.class);
        var classes = mock(IClassAccess.class);
        var audit = mock(IAudit.class);
        var assignments = mock(IAssignmentQuery.class);
        var subs = mock(ISubmissionQuery.class);
        var contacts = mock(IAccountLookup.class);
        var rubric = mock(IRubric.class);
        var cid = UUID.randomUUID();
        var aid = UUID.randomUUID();
        var sid = UUID.randomUUID();
        var actor = new Actor(sid, Role.STUDENT);
        when(auth.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.allow());
        when(classes.isActiveStudent(sid, cid)).thenReturn(true);
        var mine = Evaluation.pending(UUID.randomUUID(), aid, cid, sid, "hash", BigDecimal.TEN, "[]", Instant.now());
        var other = Evaluation.pending(UUID.randomUUID(), aid, cid, UUID.randomUUID(), "hash", BigDecimal.TEN, "[]",
                Instant.now());
        other.score = BigDecimal.TEN;
        other.state = EvaluationStatus.PUBLISHED;
        when(repo.findForClass(cid)).thenReturn(List.of(mine, other));
        var service = new GradebookService(repo, new GradingAccess(auth, classes, audit, Clock.systemUTC()),
                assignments, subs, classes, contacts, rubric, audit, Clock.systemUTC(),
                mock(org.springframework.beans.factory.ObjectProvider.class));
        assertThat(service.myGrades(actor, cid)).isEmpty();
        mine.score = BigDecimal.ONE;
        mine.state = EvaluationStatus.PUBLISHED;
        assertThat(service.myGrades(actor, cid)).hasSize(1);
        assertThat(service.myGrades(actor, cid).get(0).score()).isEqualByComparingTo("1");
        var next = Evaluation.pending(UUID.randomUUID(), aid, cid, sid, "new-hash", BigDecimal.TEN, "[]",
                mine.createdAt.plusSeconds(1));
        when(repo.findForClass(cid)).thenReturn(List.of(mine, next, other));
        assertThat(service.myGrades(actor, cid)).isEmpty();
        when(repo.findByAttemptId(mine.attemptId)).thenReturn(Optional.of(mine));
        assertThat(service.forAttempt(actor, mine.attemptId).score()).isEqualByComparingTo("1");
        when(classes.isActiveStudent(sid, cid)).thenReturn(false);
        assertThatThrownBy(() -> service.myGrades(actor, cid)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void groupSubmissionMetadataComesFromGroupContract() {
        var repo = mock(IEvaluationRepository.class);
        var auth = mock(IAuthorization.class);
        var classes = mock(IClassAccess.class);
        var audit = mock(IAudit.class);
        var assignments = mock(IAssignmentQuery.class);
        var subs = mock(ISubmissionQuery.class);
        var contacts = mock(IAccountLookup.class);
        var rubric = mock(IRubric.class);
        var provider = mock(edu.aiplatform.grading.contract.IGroupSubmissionSource.class);
        org.springframework.beans.factory.ObjectProvider<edu.aiplatform.grading.contract.IGroupSubmissionSource> groups = mock(
                org.springframework.beans.factory.ObjectProvider.class);
        when(groups.getIfAvailable()).thenReturn(provider);
        var cid = UUID.randomUUID();
        var aid = UUID.randomUUID();
        var sid = UUID.randomUUID();
        var doc = UUID.randomUUID();
        var subject = UUID.randomUUID();
        var submittedAt = Instant.now();
        var a = mock(AssignmentView.class);
        when(a.assignmentId()).thenReturn(aid);
        when(a.scope()).thenReturn(AssignmentScope.SUBJECT);
        when(a.subjectId()).thenReturn(subject);
        when(a.title()).thenReturn("Group assignment");
        when(a.gradingMode()).thenReturn(GradingMode.GRADED);
        when(a.submissionMode()).thenReturn(SubmissionMode.GROUP);
        when(assignments.get(aid)).thenReturn(Optional.of(a));
        when(assignments.listForClass(cid)).thenReturn(List.of(a));
        when(rubric.maxPoints(aid)).thenReturn(BigDecimal.TEN);
        when(classes.getClassRef(cid))
                .thenReturn(Optional.of(new ClassRef(cid, subject, "C", "Class", null, null, false)));
        when(classes.listActiveStudents(cid)).thenReturn(List.of(sid));
        when(auth.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.allow());
        var member = Evaluation.pending(null, aid, cid, sid, "hash", BigDecimal.TEN, "[]", Instant.now());
        member.kind = EvaluationKind.MEMBER;
        member.groupDocumentId = doc;
        member.score = BigDecimal.ONE;
        member.state = EvaluationStatus.PUBLISHED;
        when(repo.findForClass(cid)).thenReturn(List.of(member));
        when(provider.listGroupSubmissions(aid, cid))
                .thenReturn(List.of(new edu.aiplatform.grading.contract.GroupSubmissionStatus(doc, UUID.randomUUID(),
                        "Group", true, submittedAt, true)));
        var service = new GradebookService(repo, new GradingAccess(auth, classes, audit, Clock.systemUTC()),
                assignments, subs, classes, contacts, rubric, audit, Clock.systemUTC(), groups);
        var cell = service.list(new Actor(UUID.randomUUID(), Role.TEACHER), cid, null, 1, 20).rows().get(0).grades()
                .get(0);
        assertThat(cell.late()).isTrue();
        assertThat(cell.submittedAt()).isEqualTo(submittedAt);
        assertThat(cell.score()).isEqualByComparingTo("1");
        verifyNoInteractions(subs);
    }
}
