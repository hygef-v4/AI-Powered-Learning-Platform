package edu.aiplatform.grading.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import edu.aiplatform.grading.domain.*;
import edu.aiplatform.grading.application.GradingCommands.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.academics.contract.*;
import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.assignmentwork.contract.*;
import edu.aiplatform.audit.contract.*;
import edu.aiplatform.jobs.contract.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GradingServiceTest {
    final IEvaluationRepository repo = mock(IEvaluationRepository.class);
    final IAuthorization auth = mock(IAuthorization.class);
    final IAudit audit = mock(IAudit.class);
    final IEventPublisher events = mock(IEventPublisher.class);
    final ISubmissionQuery submissions = mock(ISubmissionQuery.class);
    final IClassAccess classes = mock(IClassAccess.class);
    final UUID teacherId = UUID.randomUUID(), classId = UUID.randomUUID(), assignmentId = UUID.randomUUID(),
            studentId = UUID.randomUUID();
    final Actor teacher = new Actor(teacherId, Role.TEACHER);
    final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    final UUID rubricId = UUID.randomUUID(), itemId = UUID.randomUUID();
    Evaluation evaluation;
    GradingService service;

    @BeforeEach
    void setup() throws Exception {
        var rubrics = List.of(new RubricView(
                rubricId, assignmentId, "p", List.of(new RubricCriterion(UUID.randomUUID(), "Criterion", "",
                        CriterionKind.values()[0], List.of(new RubricItem(itemId, "Item", new BigDecimal("4"))))),
                new BigDecimal("4")));
        evaluation = Evaluation.pending(UUID.randomUUID(), assignmentId, classId, studentId, "hash",
                new BigDecimal("4"), json.writeValueAsString(rubrics), Instant.now());
        when(repo.findById(evaluation.id)).thenReturn(Optional.of(evaluation));
        when(repo.lockById(evaluation.id)).thenReturn(Optional.of(evaluation));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(auth.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.allow());
        when(submissions.getSubmission(evaluation.attemptId))
                .thenReturn(Optional.of(new SubmissionView(evaluation.attemptId, assignmentId, classId, studentId,
                        new DocumentModel(List.of()), "hash", Instant.now(), false, SubmitMode.values()[0])));
        when(submissions.listSubmissions(assignmentId, classId))
                .thenReturn(List.of(new StudentSubmissionStatus(studentId, evaluation.attemptId, 1,
                        AttemptStatus.SUBMITTED, Instant.now(), false)));
        service = new GradingService(repo, new GradingAccess(auth, classes, audit, Clock.systemUTC()), audit, events,
                submissions, json, Clock.systemUTC());
    }

    Save command(long version) {
        return new Save(version, Map.of(rubricId, Set.of(itemId)), BigDecimal.ZERO, null, "feedback", null, null);
    }

    @Test
    void serverComputesChecklistAndNeverCallsAi() {
        service.save(teacher, evaluation.id, command(0));
        assertThat(evaluation.score()).isEqualByComparingTo("4");
        assertThat(evaluation.status()).isEqualTo(EvaluationStatus.DRAFT);
    }

    @Test
    void unknownRubricItemRejectedWithoutMutatingGrade() {
        assertThatThrownBy(() -> service.save(teacher, evaluation.id,
                new Save(0, Map.of(rubricId, Set.of(UUID.randomUUID())), null, null, null, null, null)))
                .hasMessageContaining("rubric");
        assertThat(evaluation.score()).isNull();
    }

    @Test
    void changedTeacherAndStaleVersionCannotSave() {
        when(auth.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.deny("OUT_OF_CLASS_SCOPE"));
        assertThatThrownBy(() -> service.save(teacher, evaluation.id, command(0))).isInstanceOfSatisfying(
                edu.aiplatform.shared.error.ApiException.class, ex -> assertThat(ex.status().value()).isEqualTo(404));
        when(auth.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.allow());
        assertThatThrownBy(() -> service.save(teacher, evaluation.id, command(9))).hasMessageContaining("Reload");
    }

    @Test
    void staleVersionDoesNotWriteAuditOrPublish() {
        assertThatThrownBy(() -> service.save(teacher, evaluation.id, command(9)))
                .isInstanceOfSatisfying(edu.aiplatform.shared.error.ApiException.class,
                        ex -> assertThat(ex.status().value()).isEqualTo(409));
        verify(repo, never()).save(any());
        verifyNoInteractions(audit, events);
    }

    @Test
    void oldPublishedAttemptCannotBeEditedAfterResubmission() {
        when(submissions.listSubmissions(assignmentId, classId))
                .thenReturn(List.of(new StudentSubmissionStatus(studentId, UUID.randomUUID(), 2,
                        AttemptStatus.SUBMITTED, Instant.now(), false)));
        assertThatThrownBy(() -> service.save(teacher, evaluation.id, command(0))).hasMessageContaining("latest");
    }

    @Test
    void finalizeAndPublishAreSeparateAndEmitOnlyOnTransition() {
        service.save(teacher, evaluation.id, command(0));
        service.finalizeOne(teacher, new ItemVersion(evaluation.id, 0));
        assertThat(evaluation.status()).isEqualTo(EvaluationStatus.FINALIZED);
        service.publishOne(teacher, new ItemVersion(evaluation.id, 0));
        assertThat(evaluation.status()).isEqualTo(EvaluationStatus.PUBLISHED);
        assertThatThrownBy(() -> service.publishOne(teacher, new ItemVersion(evaluation.id, 0)))
                .isInstanceOf(RuntimeException.class);
        verify(events, times(1))
                .publish(argThat(e -> e.eventType().equals("grade.published") && !e.payload().containsKey("score")));
    }

    @Test
    void commonGroupUpdatesFollowRootButPreserveManualOverrides() {
        evaluation.kind = EvaluationKind.GROUP_DOCUMENT;
        evaluation.groupDocumentId = UUID.randomUUID();
        var member = Evaluation.pending(null, assignmentId, classId, studentId, "hash", new BigDecimal("4"),
                evaluation.rubricSnapshot, Instant.now());
        member.kind = EvaluationKind.MEMBER;
        member.groupDocumentId = evaluation.groupDocumentId;
        when(repo.findForAssignmentAndClass(assignmentId, classId)).thenReturn(List.of(evaluation, member));
        when(repo.lockById(member.id)).thenReturn(Optional.of(member));
        service.save(teacher, evaluation.id, command(0));
        service.save(teacher, evaluation.id, new Save(0, Map.of(), BigDecimal.ZERO, null, "new feedback", null, null));
        assertThat(member.score).isEqualByComparingTo("0");
        assertThat(member.feedback).isEqualTo("new feedback");
        service.saveMember(teacher, evaluation.id, studentId, new MemberSave(0, BigDecimal.ONE, "manual contribution"));
        service.save(teacher, evaluation.id, command(0));
        assertThat(member.score).isEqualByComparingTo("1");
    }
    @Test
    void savesPartialItemPointsAndKeepsChecklistBackwardsCompatible() throws Exception {
        var command = json.readValue("""
            {"version":0,"checkedItemsByRubric":{"%s":["%s"]},
             "itemScoresByRubric":{"%s":{"%s":"0.50"}}}
            """.formatted(rubricId, itemId, rubricId, itemId), Save.class);
        service.save(teacher, evaluation.id, command);
        assertThat(evaluation.score).isEqualByComparingTo("0.50");
        assertThat(json.valueToTree(evaluation).path("rubricItemScores").path(rubricId.toString())
                .path(itemId.toString()).decimalValue()).isEqualByComparingTo("0.50");
    }

    @Test
    void rejectsPartialScoresOutsideItemBoundsOrPrecisionWithoutChangingGrade() throws Exception {
        for (String value : List.of("-0.01", "4.01", "0.001")) {
            var command = json.readValue("""
                {"version":0,"checkedItemsByRubric":{},"itemScoresByRubric":{"%s":{"%s":"%s"}}}
                """.formatted(rubricId, itemId, value), Save.class);
            assertThatThrownBy(() -> service.save(teacher, evaluation.id, command))
                    .isInstanceOf(edu.aiplatform.shared.error.ApiException.class);
            assertThat(evaluation.score).isNull();
        }
        verify(repo, never()).save(any());
    }

    @Test
    void rejectsUnknownItemInPartialScores() throws Exception {
        var command = json.readValue("""
            {"version":0,"checkedItemsByRubric":{},"itemScoresByRubric":{"%s":{"%s":"0.5"}}}
            """.formatted(rubricId, UUID.randomUUID()), Save.class);
        assertThatThrownBy(() -> service.save(teacher, evaluation.id, command))
                .isInstanceOf(edu.aiplatform.shared.error.ApiException.class);
        assertThat(evaluation.score).isNull();
    }

    @Test
    void groupMembersInheritFractionalBreakdownAndResubmissionClearsIt() throws Exception {
        evaluation.kind = EvaluationKind.GROUP_DOCUMENT; evaluation.groupDocumentId = UUID.randomUUID();
        var member = Evaluation.pending(null, assignmentId, classId, studentId, "hash", new BigDecimal("4"),
                evaluation.rubricSnapshot, Instant.now());
        member.kind = EvaluationKind.MEMBER; member.groupDocumentId = evaluation.groupDocumentId;
        when(repo.findForAssignmentAndClass(assignmentId, classId)).thenReturn(List.of(evaluation, member));
        when(repo.lockById(member.id)).thenReturn(Optional.of(member));
        service.save(teacher, evaluation.id, new Save(0, Map.of(), null, null, "partial", null, null,
                Map.of(rubricId, Map.of(itemId, new BigDecimal("0.50")))));
        assertThat(member.score).isEqualByComparingTo("0.50");
        assertThat(member.rubricItemScores).isEqualTo(evaluation.rubricItemScores);
        service.finalizeOne(teacher, new ItemVersion(evaluation.id, 0));
        service.save(teacher, evaluation.id, new Save(0, Map.of(), null, null, "reviewed", "correction", null,
                Map.of(rubricId, Map.of(itemId, new BigDecimal("0.75")))));
        assertThat(evaluation.changes.get(0).previousItemScores().get(rubricId).get(itemId)).isEqualByComparingTo("0.50");
        evaluation.resetForGroupSubmission("new", new BigDecimal("4"), evaluation.rubricSnapshot, Instant.now());
        assertThat(evaluation.rubricItemScores).isEmpty();
    }

}
