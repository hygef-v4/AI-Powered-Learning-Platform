package edu.aiplatform.grading.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import edu.aiplatform.academics.contract.*;
import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.assignmentwork.contract.*;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.grading.model.*;
import edu.aiplatform.grading.repository.*;
import edu.aiplatform.identity.contract.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class GradebookAssemblyTest {
    final UUID classId = UUID.randomUUID(), assignmentId = UUID.randomUUID();
    final Actor actor = new Actor(UUID.randomUUID(), Role.TEACHER);
    final IEvaluationRepository repo = mock(IEvaluationRepository.class);
    final IClassAccess classes = mock(IClassAccess.class);
    final IAssignmentQuery assignments = mock(IAssignmentQuery.class);
    final ISubmissionQuery submissions = mock(ISubmissionQuery.class);
    final IAccountLookup accounts = mock(IAccountLookup.class);
    final IRubric rubric = mock(IRubric.class);
    final IAudit audit = mock(IAudit.class);
    final GradebookService service = new GradebookService(repo, mock(GradingAccess.class), assignments,
            submissions, classes, accounts, rubric, audit, Clock.systemUTC(), mock(ObjectProvider.class),
            mock(GradeDistributionVisibilityRepository.class));

    List<Evaluation> fixture(int size) {
        var assignment = mock(AssignmentView.class);
        when(assignment.assignmentId()).thenReturn(assignmentId);
        when(assignment.scope()).thenReturn(AssignmentScope.CLASS);
        when(assignment.classId()).thenReturn(classId);
        when(assignment.title()).thenReturn("Design");
        when(assignment.gradingMode()).thenReturn(GradingMode.GRADED);
        when(assignment.status()).thenReturn(AssignmentStatus.OPEN);
        when(assignment.submissionMode()).thenReturn(SubmissionMode.INDIVIDUAL);
        when(assignments.listForClass(classId)).thenReturn(List.of(assignment));
        when(assignments.get(assignmentId)).thenReturn(Optional.of(assignment));
        when(classes.getClassRef(classId)).thenReturn(Optional.of(new ClassRef(classId, UUID.randomUUID(),
                "C", "Class", ClassStatus.OPEN, actor.accountId(), false)));
        when(rubric.maxPoints(assignmentId)).thenReturn(BigDecimal.TEN);
        var students = new ArrayList<UUID>();
        var contacts = new HashMap<UUID, AccountContact>();
        var values = new ArrayList<Evaluation>();
        var statuses = new ArrayList<StudentSubmissionStatus>();
        for (int i = 0; i < size; i++) {
            var student = UUID.randomUUID();
            students.add(student);
            contacts.put(student, new AccountContact(student, "s" + i + "@school.test", "Student " + i,
                    Role.STUDENT, AccountStatus.ACTIVE));
            when(accounts.getContact(student)).thenReturn(Optional.of(contacts.get(student)));
            var e = Evaluation.pending(UUID.randomUUID(), assignmentId, classId, student, "hash",
                    BigDecimal.TEN, "[]", Instant.parse("2026-11-01T00:00:00Z"));
            e.score = new BigDecimal("7.50");
            e.state = EvaluationStatus.PUBLISHED;
            e.feedback = "Feedback " + i;
            values.add(e);
            statuses.add(new StudentSubmissionStatus(student, e.attemptId, 1, AttemptStatus.SUBMITTED,
                    Instant.parse("2026-11-01T01:00:00Z"), false));
        }
        when(classes.listActiveStudents(classId)).thenReturn(students);
        when(accounts.getContacts(students)).thenReturn(contacts);
        when(repo.findForClass(classId)).thenReturn(values);
        when(submissions.listSubmissions(assignmentId, classId)).thenReturn(statuses);
        return values;
    }

    @Test void exportLoadsA201StudentClassOnceAndKeepsOfficialScores() {
        fixture(201);
        var rows = service.exportRows(actor, classId, assignmentId, "csv");
        assertThat(rows).hasSize(202);
        assertThat(rows.subList(1, rows.size())).extracting(row -> row.get(1)).doesNotHaveDuplicates();
        assertThat(rows.get(201)).contains("Student 200", "s200@school.test", "7.50", "Feedback 200");
        verify(repo, times(1)).findForClass(classId);
        verify(submissions, times(1)).listSubmissions(assignmentId, classId);
        verify(accounts, times(1)).getContacts(anyCollection());
        verify(accounts, never()).getContact(any());
    }

    @Test void listUsesLatestReceiptAndPreservesSearchPaginationAndMissingContactFallback() {
        var values = fixture(3);
        var old = Evaluation.pending(UUID.randomUUID(), assignmentId, classId, values.get(0).accountId,
                "old", BigDecimal.TEN, "[]", values.get(0).createdAt.plusSeconds(10));
        old.state = EvaluationStatus.PUBLISHED;
        old.score = BigDecimal.ONE;
        values.add(old);
        values.get(1).state = EvaluationStatus.SUPERSEDED;
        var page = service.list(actor, classId, "s0@school.test", 1, 1);
        assertThat(page.total()).isEqualTo(1);
        assertThat(page.rows().get(0).grades().get(0).score()).isEqualByComparingTo("7.50");
        assertThat(service.list(actor, classId, null, 2, 1).rows().get(0).grades().get(0).evaluationId()).isNull();
        when(accounts.getContacts(anyCollection())).thenReturn(Map.of());
        when(accounts.getContact(any())).thenReturn(Optional.empty());
        assertThat(service.list(actor, classId, null, 1, 1).rows().get(0).student().name()).isEqualTo("Unavailable");
    }

    @Test void exportHidesDraftScoresAndEmptyClassExportsOnlyHeaders() {
        var values = fixture(1);
        values.get(0).state = EvaluationStatus.DRAFT;
        var row = service.exportRows(actor, classId, assignmentId, "xlsx").get(1);
        assertThat(row.get(6)).isEmpty();
        assertThat(row.get(8)).isEmpty();
        when(classes.listActiveStudents(classId)).thenReturn(List.of());
        assertThat(service.exportRows(actor, classId, null, "csv")).hasSize(1);
    }
}
