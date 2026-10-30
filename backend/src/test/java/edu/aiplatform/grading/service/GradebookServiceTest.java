package edu.aiplatform.grading.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.academics.contract.*;
import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.assignmentwork.contract.*;
import edu.aiplatform.audit.contract.*;
import edu.aiplatform.grading.model.*;
import edu.aiplatform.grading.repository.*;
import edu.aiplatform.identity.contract.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

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
        mine.state = EvaluationStatus.FINALIZED;
        assertThat(service.myGrades(actor, cid)).isEmpty();
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
    void publishedStudentGradeExposesSanitizedStructuredFeedbackAndHidesTeacherMetadata() throws Exception {
        var json = new ObjectMapper().findAndRegisterModules();
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
        var rid = UUID.randomUUID();
        var iid = UUID.randomUUID();
        var actor = new Actor(sid, Role.STUDENT);
        when(auth.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.allow());
        when(classes.isActiveStudent(sid, cid)).thenReturn(true);

        var rubrics = List.of(new RubricView(
                rid, aid, "part-1",
                List.of(new RubricCriterion(UUID.randomUUID(), "Thiết kế lớp", "", CriterionKind.values()[0],
                        List.of(new RubricItem(iid, "Xác định đúng thuộc tính", new BigDecimal("2.00"))))),
                new BigDecimal("2.00")));
        var mine = Evaluation.pending(UUID.randomUUID(), aid, cid, sid, "hash", new BigDecimal("2.00"),
                json.writeValueAsString(rubrics), Instant.now());
        mine.rubricItemScores = Map.of(rid, Map.of(iid, new BigDecimal("1.50")));
        mine.adjustment = new BigDecimal("-0.25");
        mine.adjustmentReason = "Nộp trễ";
        mine.score = new BigDecimal("1.25");
        mine.feedback = "Nhận xét tổng quát";
        mine.structuredFeedback = new GradingFeedback(
                1,
                new GradingFeedback.Summary(List.of("Đúng lớp chính"), List.of("Thiếu kiểu dữ liệu"), List.of("Thêm kiểu dữ liệu")),
                List.of(new GradingFeedback.Item(
                        rid, iid, "Lớp SinhVien có thuộc tính maSV", "Đạt 1.50/2.00 do thiếu kiểu dữ liệu ở 1 thuộc tính",
                        "Bổ sung kiểu String cho maSV", "block-secret", true, "Lý do nội bộ cho giảng viên")));
        mine.reviewedItemScores = Map.of(rid, Map.of(iid, new BigDecimal("1.50")));
        mine.acknowledgedReviewItems = Map.of(rid, Set.of(iid));
        mine.state = EvaluationStatus.PUBLISHED;
        mine.publishedAt = Instant.now();

        when(repo.findByAttemptId(mine.attemptId)).thenReturn(Optional.of(mine));
        var service = new GradebookService(repo, new GradingAccess(auth, classes, audit, Clock.systemUTC()),
                assignments, subs, classes, contacts, rubric, audit, Clock.systemUTC(),
                mock(org.springframework.beans.factory.ObjectProvider.class));

        var view = service.forAttempt(actor, mine.attemptId);
        assertThat(view.score()).isEqualByComparingTo("1.25");
        assertThat(view.rubricScore()).isEqualByComparingTo("1.50");
        assertThat(view.structuredSummary()).isNotNull();
        assertThat(view.structuredSummary().strengths()).containsExactly("Đúng lớp chính");
        assertThat(view.rubricItems()).singleElement().satisfies(item -> {
            assertThat(item.criterionTitle()).isEqualTo("Thiết kế lớp");
            assertThat(item.itemText()).isEqualTo("Xác định đúng thuộc tính");
            assertThat(item.awardedPoints()).isEqualByComparingTo("1.50");
            assertThat(item.maxPoints()).isEqualByComparingTo("2.00");
            assertThat(item.evidence()).isEqualTo("Lớp SinhVien có thuộc tính maSV");
            assertThat(item.reason()).isEqualTo("Đạt 1.50/2.00 do thiếu kiểu dữ liệu ở 1 thuộc tính");
            assertThat(item.suggestion()).isEqualTo("Bổ sung kiểu String cho maSV");
        });

        String serialized = json.writeValueAsString(view);
        assertThat(serialized)
                .doesNotContain("requiresReview")
                .doesNotContain("reviewReason")
                .doesNotContain("Lý do nội bộ cho giảng viên")
                .doesNotContain("evidenceBlockId")
                .doesNotContain("block-secret")
                .doesNotContain("reviewedItemScores")
                .doesNotContain("acknowledgedReviewItems");
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
