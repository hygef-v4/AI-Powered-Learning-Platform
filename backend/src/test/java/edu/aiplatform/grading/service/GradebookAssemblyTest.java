package edu.aiplatform.grading.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.aiplatform.academics.contract.*;
import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.assignmentwork.contract.*;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.grading.model.*;
import edu.aiplatform.grading.repository.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.grading.controller.GradebookController;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class GradebookAssemblyTest {
    @Test
    void exportRetainsAssignmentTitleEvenWithNoStudents() {
        fixture(0);
        var export = gradebookService.buildExport(actor, classId, assignmentId, "xlsx");
        assertThat(export.assignmentTitle()).isEqualTo("Design");
        assertThat(export.rows()).hasSize(1);
        assertThat(gradebookService.buildExport(actor, classId, null, "csv").assignmentTitle()).isEqualTo("Design");
    }

    @Test
    void exportUsesVietnameseHeadersStatusesAndSubmissionTime() {
        var evaluation = fixture(1).get(0);
        var rows = gradebookService.buildExportRows(actor, classId, assignmentId, "xlsx");
        assertThat(rows.get(0)).containsExactly("Sinh viên", "Email", "Bài tập", "Trạng thái",
                "Thời gian nộp", "Nộp muộn", "Điểm", "Điểm tối đa", "Nhận xét");
        assertThat(rows.get(1)).contains("Đã công bố", "01/11/2026 08:00:00", "Không");
        for (var state : List.of(EvaluationStatus.PENDING, EvaluationStatus.DRAFT, EvaluationStatus.FINALIZED)) {
            evaluation.state = state;
            String expected = switch (state) {
                case PENDING -> "Chờ chấm";
                case DRAFT -> "Đã chấm";
                default -> "Đã chốt điểm";
            };
            assertThat(gradebookService.buildExportRows(actor, classId, assignmentId, "csv").get(1).get(3))
                    .isEqualTo(expected);
        }
        when(evaluationRepository.findForClass(classId)).thenReturn(List.of());
        when(submissionQuery.listSubmissions(assignmentId, classId)).thenReturn(List.of());
        assertThat(gradebookService.buildExportRows(actor, classId, assignmentId, "xlsx").get(1).get(3))
                .isEqualTo("Chưa nộp");
    }

    final UUID classId = UUID.randomUUID(), assignmentId = UUID.randomUUID();
    final Actor actor = new Actor(UUID.randomUUID(), Role.TEACHER);
    final IEvaluationRepository evaluationRepository = mock(IEvaluationRepository.class);
    final IClassAccess classAccess = mock(IClassAccess.class);
    final IAssignmentQuery assignmentQuery = mock(IAssignmentQuery.class);
    final ISubmissionQuery submissionQuery = mock(ISubmissionQuery.class);
    final IAccountLookup accountLookup = mock(IAccountLookup.class);
    final IRubric rubric = mock(IRubric.class);
    final IAudit audit = mock(IAudit.class);
    final ObjectProvider<edu.aiplatform.grading.contract.IGroupSubmissionSource> groupSubmissionSourceProvider = mock(ObjectProvider.class);
    final GradingAccess gradingAccess = mock(GradingAccess.class);
    final GradebookService gradebookService = new GradebookService(evaluationRepository, gradingAccess, assignmentQuery,
            submissionQuery, classAccess, accountLookup, rubric, audit, Clock.systemUTC(), groupSubmissionSourceProvider,
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
        when(assignmentQuery.listForClass(classId)).thenReturn(List.of(assignment));
        when(assignmentQuery.get(assignmentId)).thenReturn(Optional.of(assignment));
        when(classAccess.getClassRef(classId)).thenReturn(Optional.of(new ClassRef(classId, UUID.randomUUID(),
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
            when(accountLookup.getContact(student)).thenReturn(Optional.of(contacts.get(student)));
            var e = Evaluation.createPendingEvaluation(UUID.randomUUID(), assignmentId, classId, student, "hash",
                    BigDecimal.TEN, "[]", Instant.parse("2026-11-01T00:00:00Z"));
            e.score = new BigDecimal("7.50");
            e.state = EvaluationStatus.PUBLISHED;
            e.feedback = "Feedback " + i;
            values.add(e);
            statuses.add(new StudentSubmissionStatus(student, e.attemptId, 1, AttemptStatus.SUBMITTED,
                    Instant.parse("2026-11-01T01:00:00Z"), false));
        }
        when(classAccess.listActiveStudents(classId)).thenReturn(students);
        when(accountLookup.getContacts(anyCollection())).thenAnswer(invocation -> {
            Map<UUID, AccountContact> selected = new HashMap<>();
            for (UUID id : invocation.<Collection<UUID>>getArgument(0))
                if (contacts.containsKey(id)) selected.put(id, contacts.get(id));
            return selected;
        });
        when(evaluationRepository.findForClass(classId)).thenReturn(values);
        when(submissionQuery.listSubmissions(assignmentId, classId)).thenReturn(statuses);
        return values;
    }

    @Test void studentAtEndOf501LearnerRosterLoadsSourcesOnce() {
        var values = fixture(501);
        var target = values.get(500).accountId;
        var row = new GradebookController(gradebookService, mock(GradebookExporter.class)).loadStudentGrades(actor, classId, target);
        assertThat(row.student().accountId()).isEqualTo(target);
        assertThat(row.student().name()).isEqualTo("Student 500");
        assertThat(row.grades()).hasSize(1);
        assertThat(row.grades().get(0).score()).isEqualByComparingTo("7.50");
        verify(evaluationRepository).findForClass(classId);
        verify(submissionQuery).listSubmissions(assignmentId, classId);
        verify(accountLookup).getContacts(List.of(target));
    }

    @Test void studentLookupKeepsReceiptSelectionAndMissingContactFallback() {
        var values = fixture(2);
        var target = values.get(1).accountId;
        var old = Evaluation.createPendingEvaluation(UUID.randomUUID(), assignmentId, classId, target,
                "old", BigDecimal.TEN, "[]", values.get(1).createdAt.plusSeconds(10));
        old.state = EvaluationStatus.PUBLISHED;
        old.score = BigDecimal.ONE;
        values.add(old);
        when(accountLookup.getContacts(List.of(target))).thenReturn(Map.of());
        var row = gradebookService.loadStudentGrades(actor, classId, target);
        assertThat(row.student().name()).isEqualTo("Unavailable");
        assertThat(row.grades().get(0).score()).isEqualByComparingTo("7.50");
        values.get(1).state = EvaluationStatus.SUPERSEDED;
        assertThat(gradebookService.loadStudentGrades(actor, classId, target).grades().get(0).evaluationId()).isNull();
    }

    @Test void studentLookupRejectsAbsentOrRemovedLearnersAndMissingClass() {
        var values = fixture(1);
        assertThatThrownBy(() -> gradebookService.loadStudentGrades(actor, classId, UUID.randomUUID()))
                .isInstanceOfSatisfying(edu.aiplatform.shared.error.ApiException.class,
                        ex -> assertThat(ex.code()).isEqualTo("STUDENT_NOT_FOUND"));
        when(classAccess.listActiveStudents(classId)).thenReturn(List.of());
        assertThatThrownBy(() -> gradebookService.loadStudentGrades(actor, classId, values.get(0).accountId))
                .isInstanceOfSatisfying(edu.aiplatform.shared.error.ApiException.class,
                        ex -> assertThat(ex.code()).isEqualTo("STUDENT_NOT_FOUND"));
        when(classAccess.getClassRef(classId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> gradebookService.loadStudentGrades(actor, classId, values.get(0).accountId))
                .isInstanceOfSatisfying(edu.aiplatform.shared.error.ApiException.class,
                        ex -> assertThat(ex.code()).isEqualTo("CLASS_NOT_FOUND"));
    }

    @Test void studentLookupAuthorizesBeforeReadingAnyGrades() {
        doThrow(edu.aiplatform.shared.error.ApiException.forbidden("CLASS_FORBIDDEN"))
                .when(gradingAccess).requireTeacher(actor, classId, Actions.GRADEBOOK_READ);
        assertThatThrownBy(() -> gradebookService.loadStudentGrades(actor, classId, UUID.randomUUID()))
                .isInstanceOf(edu.aiplatform.shared.error.ApiException.class);
        verifyNoInteractions(evaluationRepository, classAccess, accountLookup, assignmentQuery, submissionQuery);
    }

    @Test void studentLookupKeepsGroupMemberOverrideWithoutIndividualReceipt() {
        var values = fixture(2);
        var member = values.get(1);
        member.kind = EvaluationKind.MEMBER;
        member.groupDocumentId = UUID.randomUUID();
        member.score = new BigDecimal("8.25");
        when(assignmentQuery.get(assignmentId).orElseThrow().submissionMode()).thenReturn(SubmissionMode.GROUP);
        var source = mock(edu.aiplatform.grading.contract.IGroupSubmissionSource.class);
        when(groupSubmissionSourceProvider.getIfAvailable()).thenReturn(source);
        when(source.listGroupSubmissions(assignmentId, classId)).thenReturn(List.of(
                new edu.aiplatform.grading.contract.GroupSubmissionStatus(member.groupDocumentId,
                        UUID.randomUUID(), "Group", true, Instant.parse("2026-11-01T01:00:00Z"), true)));
        var row = gradebookService.loadStudentGrades(actor, classId, member.accountId);
        assertThat(row.grades().get(0).score()).isEqualByComparingTo("8.25");
        assertThat(row.grades().get(0).late()).isTrue();
        verify(source).listGroupSubmissions(assignmentId, classId);
        verify(submissionQuery, never()).listSubmissions(any(), any());
    }

    @Test void exportLoadsA201StudentClassOnceAndKeepsOfficialScores() {
        fixture(201);
        var rows = gradebookService.buildExportRows(actor, classId, assignmentId, "csv");
        assertThat(rows).hasSize(202);
        assertThat(rows.subList(1, rows.size())).extracting(row -> row.get(1)).doesNotHaveDuplicates();
        assertThat(rows.get(201)).contains("Student 200", "s200@school.test", "7.50", "Feedback 200");
        verify(evaluationRepository, times(1)).findForClass(classId);
        verify(submissionQuery, times(1)).listSubmissions(assignmentId, classId);
        verify(accountLookup, times(1)).getContacts(anyCollection());
        verify(accountLookup, never()).getContact(any());
    }

    @Test void listUsesLatestReceiptAndPreservesSearchPaginationAndMissingContactFallback() {
        var values = fixture(3);
        var old = Evaluation.createPendingEvaluation(UUID.randomUUID(), assignmentId, classId, values.get(0).accountId,
                "old", BigDecimal.TEN, "[]", values.get(0).createdAt.plusSeconds(10));
        old.state = EvaluationStatus.PUBLISHED;
        old.score = BigDecimal.ONE;
        values.add(old);
        values.get(1).state = EvaluationStatus.SUPERSEDED;
        var page = gradebookService.listGrades(actor, classId, "s0@school.test", 1, 1);
        assertThat(page.total()).isEqualTo(1);
        assertThat(page.rows().get(0).grades().get(0).score()).isEqualByComparingTo("7.50");
        assertThat(gradebookService.listGrades(actor, classId, null, 2, 1).rows().get(0).grades().get(0).evaluationId()).isNull();
        when(accountLookup.getContacts(anyCollection())).thenReturn(Map.of());
        when(accountLookup.getContact(any())).thenReturn(Optional.empty());
        assertThat(gradebookService.listGrades(actor, classId, null, 1, 1).rows().get(0).student().name()).isEqualTo("Unavailable");
    }

    @Test void exportHidesDraftScoresAndEmptyClassExportsOnlyHeaders() {
        var values = fixture(1);
        values.get(0).state = EvaluationStatus.DRAFT;
        var row = gradebookService.buildExportRows(actor, classId, assignmentId, "xlsx").get(1);
        assertThat(row.get(6)).isEmpty();
        assertThat(row.get(8)).isEmpty();
        when(classAccess.listActiveStudents(classId)).thenReturn(List.of());
        assertThat(gradebookService.buildExportRows(actor, classId, null, "csv")).hasSize(1);
    }
}
