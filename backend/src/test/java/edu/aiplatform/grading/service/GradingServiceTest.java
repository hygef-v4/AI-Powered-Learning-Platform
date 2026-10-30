package edu.aiplatform.grading.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.academics.contract.*;
import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.assignmentwork.contract.*;
import edu.aiplatform.audit.contract.*;
import edu.aiplatform.grading.service.GradingCommands.*;
import edu.aiplatform.grading.model.*;
import edu.aiplatform.grading.repository.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.jobs.contract.*;
import edu.aiplatform.shared.error.ApiException;
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
    final UUID rubricId = UUID.randomUUID(), itemId = UUID.randomUUID(), blockId = UUID.randomUUID();
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
                        new DocumentModel(List.of(new DocumentBlock(blockId, BlockOrigin.STUDENT, BlockType.PARAGRAPH,
                                json.createObjectNode().put("text", "Answer")))),
                        "hash", Instant.now(), false, SubmitMode.values()[0])));
        when(submissions.listSubmissions(assignmentId, classId))
                .thenReturn(List.of(new StudentSubmissionStatus(studentId, evaluation.attemptId, 1,
                        AttemptStatus.SUBMITTED, Instant.now(), false)));
        service = new GradingService(repo, new GradingAccess(auth, classes, audit, Clock.systemUTC()), audit, events,
                submissions, json, Clock.systemUTC());
    }

    Save command(long version) {
        return new Save(version, Map.of(rubricId, Set.of(itemId)), BigDecimal.ZERO, null, "feedback", null, null);
    }

    GradingFeedback sampleFeedback(String reason, boolean requiresReview, String reviewReason) {
        return new GradingFeedback(
                1,
                new GradingFeedback.Summary(List.of("Đúng cấu trúc"), List.of("Cần chi tiết hơn"), List.of("Bổ sung ví dụ")),
                List.of(new GradingFeedback.Item(
                        rubricId, itemId, "Minh chứng trong bài", reason, "Gợi ý cải thiện", blockId.toString(),
                        requiresReview, reviewReason)));
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
                ApiException.class, ex -> assertThat(ex.status().value()).isEqualTo(404));
        when(auth.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.allow());
        assertThatThrownBy(() -> service.save(teacher, evaluation.id, command(9))).hasMessageContaining("Reload");
    }

    @Test
    void staleVersionDoesNotWriteAuditOrPublish() {
        assertThatThrownBy(() -> service.save(teacher, evaluation.id, command(9)))
                .isInstanceOfSatisfying(ApiException.class,
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
                    .isInstanceOf(ApiException.class);
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
                .isInstanceOf(ApiException.class);
        assertThat(evaluation.score).isNull();
    }

    @Test
    void groupMembersInheritFractionalBreakdownAndResubmissionClearsIt() throws Exception {
        evaluation.kind = EvaluationKind.GROUP_DOCUMENT;
        evaluation.groupDocumentId = UUID.randomUUID();
        var member = Evaluation.pending(null, assignmentId, classId, studentId, "hash", new BigDecimal("4"),
                evaluation.rubricSnapshot, Instant.now());
        member.kind = EvaluationKind.MEMBER;
        member.groupDocumentId = evaluation.groupDocumentId;
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

    @Test
    void savesStructuredFeedbackAndDistinguishesAbsentFromExplicitClear() {
        var feedback = sampleFeedback("Đạt 2.00/4.00 điểm", false, null);
        service.save(teacher, evaluation.id, new Save(
                0, Map.of(), BigDecimal.ZERO, null, "free-text", null, null,
                Map.of(rubricId, Map.of(itemId, new BigDecimal("2.00"))),
                feedback, false, null, null));
        assertThat(evaluation.structuredFeedback).isEqualTo(feedback);
        assertThat(evaluation.reviewedItemScores.get(rubricId).get(itemId)).isEqualByComparingTo("2.00");

        // Legacy/old-client Save with structuredFeedback=null and clearStructuredFeedback=false preserves existing feedback
        service.save(teacher, evaluation.id, new Save(
                0, Map.of(), BigDecimal.ZERO, null, "updated free-text", null, null,
                Map.of(rubricId, Map.of(itemId, new BigDecimal("2.00")))));
        assertThat(evaluation.structuredFeedback).isEqualTo(feedback);

        // Explicit clearStructuredFeedback=true removes structuredFeedback and review metadata
        service.save(teacher, evaluation.id, new Save(
                0, Map.of(), BigDecimal.ZERO, null, "cleared structured", null, null,
                Map.of(rubricId, Map.of(itemId, new BigDecimal("2.00"))),
                null, true, null, null));
        assertThat(evaluation.structuredFeedback).isNull();
        assertThat(evaluation.reviewedItemScores).isEmpty();
        assertThat(evaluation.acknowledgedReviewItems).isEmpty();
    }

    @Test
    void scoreChangeInvalidatesReviewedScoreAndBlocksFinalizationUntilEditedOrConfirmed() {
        var initialFeedback = sampleFeedback("Đạt 2.00/4.00 điểm vì thiếu nửa sau", false, null);
        service.save(teacher, evaluation.id, new Save(
                0, Map.of(), BigDecimal.ZERO, null, "draft 1", null, null,
                Map.of(rubricId, Map.of(itemId, new BigDecimal("2.00"))),
                initialFeedback, false, null, null));
        assertThat(evaluation.reviewedItemScores.get(rubricId).get(itemId)).isEqualByComparingTo("2.00");

        // Teacher changes item score to 3.00 without editing the item feedback text or confirming
        service.save(teacher, evaluation.id, new Save(
                0, Map.of(), BigDecimal.ZERO, null, "draft 2", null, null,
                Map.of(rubricId, Map.of(itemId, new BigDecimal("3.00"))),
                initialFeedback, false, null, null));
        // Draft save is allowed, but reviewedItemScores stays at 2.00 so mismatch is tracked
        assertThat(evaluation.score).isEqualByComparingTo("3.00");
        assertThat(evaluation.reviewedItemScores.get(rubricId).get(itemId)).isEqualByComparingTo("2.00");

        // Finalizing while score != reviewedScore is rejected on the server
        assertThatThrownBy(() -> service.finalizeOne(teacher, new ItemVersion(evaluation.id, 0)))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.code()).isEqualTo("STRUCTURED_FEEDBACK_SCORE_MISMATCH"));
        assertThat(evaluation.state).isEqualTo(EvaluationStatus.DRAFT);

        // Explicit teacher confirmation via acknowledgedReviewItems resolves the score mismatch
        service.save(teacher, evaluation.id, new Save(
                0, Map.of(), BigDecimal.ZERO, null, "draft 2 confirmed", null, null,
                Map.of(rubricId, Map.of(itemId, new BigDecimal("3.00"))),
                initialFeedback, false, Map.of(rubricId, Set.of(itemId)), null));
        assertThat(evaluation.reviewedItemScores.get(rubricId).get(itemId)).isEqualByComparingTo("3.00");

        service.finalizeOne(teacher, new ItemVersion(evaluation.id, 0));
        assertThat(evaluation.state).isEqualTo(EvaluationStatus.FINALIZED);
    }

    @Test
    void unacknowledgedAiRequiresReviewBlocksFinalizationAndRejectsSilentUncheck() throws Exception {
        var proposal = new GradingProposal();
        proposal.id = UUID.randomUUID();
        proposal.evaluationId = evaluation.id;
        proposal.status = "READY";
        proposal.submissionHash = "hash";
        proposal.rubricFingerprint = evaluation.rubricSnapshot;
        proposal.proposal = json.readTree("""
            {
              "checks": {"%s": []},
              "itemScoresByRubric": {"%s": {"%s": 2.00}},
              "score": "2.00",
              "feedback": "Cần xem lại",
              "sections": [],
              "structuredFeedback": {
                "schemaVersion": 1,
                "summary": {"strengths": [], "improvements": [], "nextSteps": []},
                "items": [{
                  "rubricId": "%s",
                  "itemId": "%s",
                  "evidence": null,
                  "reason": "Ảnh mờ chưa đọc rõ",
                  "suggestion": null,
                  "evidenceBlockId": null,
                  "requiresReview": true,
                  "reviewReason": "Ảnh sơ đồ chưa trích xuất được"
                }]
              }
            }
            """.formatted(rubricId, rubricId, itemId, rubricId, itemId));

        // Silent uncheck of requiresReview without acknowledging or editing is rejected
        var silentlyUnchecked = sampleFeedback("Ảnh mờ chưa đọc rõ", false, null);
        assertThatThrownBy(() -> service.saveChecked(teacher, evaluation.id, new Save(
                0, Map.of(), BigDecimal.ZERO, null, "Cần xem lại", null, proposal.id,
                Map.of(rubricId, Map.of(itemId, new BigDecimal("2.00"))),
                silentlyUnchecked, false, null, null), proposal))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.code()).isEqualTo("UNACKNOWLEDGED_REVIEW_FLAG"));

        // Saving draft with requiresReview=true is allowed, but finalizing without acknowledgement is blocked
        var flaggedFeedback = new GradingFeedback(
                1,
                new GradingFeedback.Summary(List.of(), List.of(), List.of()),
                List.of(new GradingFeedback.Item(
                        rubricId, itemId, null, "Ảnh mờ chưa đọc rõ", null, null,
                        true, "Ảnh sơ đồ chưa trích xuất được")));
        service.saveChecked(teacher, evaluation.id, new Save(
                0, Map.of(), BigDecimal.ZERO, null, "Cần xem lại", null, proposal.id,
                Map.of(rubricId, Map.of(itemId, new BigDecimal("2.00"))),
                flaggedFeedback, false, null, null), proposal);
        assertThatThrownBy(() -> service.finalizeOne(teacher, new ItemVersion(evaluation.id, 0)))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.code()).isEqualTo("STRUCTURED_FEEDBACK_REVIEW_REQUIRED"));

        // Forged acknowledgement for unknown itemId is rejected
        assertThatThrownBy(() -> service.save(teacher, evaluation.id, new Save(
                0, Map.of(), BigDecimal.ZERO, null, "Cần xem lại", null, null,
                Map.of(rubricId, Map.of(itemId, new BigDecimal("2.00"))),
                flaggedFeedback, false, Map.of(rubricId, Set.of(UUID.randomUUID())), null)))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.code()).isEqualTo("INVALID_REVIEW_METADATA"));

        // Explicit teacher acknowledgement allows finalization
        service.save(teacher, evaluation.id, new Save(
                0, Map.of(), BigDecimal.ZERO, null, "Đã kiểm tra", null, null,
                Map.of(rubricId, Map.of(itemId, new BigDecimal("2.00"))),
                flaggedFeedback, false, Map.of(rubricId, Set.of(itemId)), null));
        service.finalizeOne(teacher, new ItemVersion(evaluation.id, 0));
        assertThat(evaluation.state).isEqualTo(EvaluationStatus.FINALIZED);
    }

    @Test
    void historyAndGroupInheritancePreserveAndResetStructuredFeedbackWithoutAliasing() {
        evaluation.kind = EvaluationKind.GROUP_DOCUMENT;
        evaluation.groupDocumentId = UUID.randomUUID();
        var member = Evaluation.pending(null, assignmentId, classId, studentId, "hash", new BigDecimal("4"),
                evaluation.rubricSnapshot, Instant.now());
        member.kind = EvaluationKind.MEMBER;
        member.groupDocumentId = evaluation.groupDocumentId;
        when(repo.findForAssignmentAndClass(assignmentId, classId)).thenReturn(List.of(evaluation, member));
        when(repo.lockById(member.id)).thenReturn(Optional.of(member));

        var feedbackV1 = new GradingFeedback(
                1,
                new GradingFeedback.Summary(List.of("Đúng cấu trúc"), List.of("Cần chi tiết hơn"), List.of("Bổ sung ví dụ")),
                List.of(new GradingFeedback.Item(
                        rubricId, itemId, "Minh chứng nhóm", "Giải thích lần 1", "Gợi ý cải thiện", "p",
                        false, null)));
        service.save(teacher, evaluation.id, new Save(
                0, Map.of(), BigDecimal.ZERO, null, "group v1", null, null,
                Map.of(rubricId, Map.of(itemId, new BigDecimal("2.00"))),
                feedbackV1, false, null, null));
        assertThat(member.structuredFeedback).isEqualTo(feedbackV1);
        assertThat(member.structuredFeedback).isNotSameAs(evaluation.structuredFeedback);

        service.finalizeOne(teacher, new ItemVersion(evaluation.id, 0));

        var feedbackV2 = new GradingFeedback(
                1,
                new GradingFeedback.Summary(List.of("Đúng cấu trúc"), List.of("Cần chi tiết hơn"), List.of("Bổ sung ví dụ")),
                List.of(new GradingFeedback.Item(
                        rubricId, itemId, "Minh chứng nhóm", "Giải thích lần 2 sau khi chỉnh điểm", "Gợi ý cải thiện", "p",
                        false, null)));
        service.save(teacher, evaluation.id, new Save(
                0, Map.of(), BigDecimal.ZERO, null, "group v2", "Cập nhật điểm nhóm", null,
                Map.of(rubricId, Map.of(itemId, new BigDecimal("3.00"))),
                feedbackV2, false, null, null));
        assertThat(evaluation.changes).singleElement().satisfies(change -> {
            assertThat(change.previousStructuredFeedback()).isEqualTo(feedbackV1);
        });
        assertThat(member.structuredFeedback).isEqualTo(feedbackV2);

        evaluation.resetForGroupSubmission("new-hash", new BigDecimal("4"), evaluation.rubricSnapshot, Instant.now());
        assertThat(evaluation.structuredFeedback).isNull();
        assertThat(evaluation.reviewedItemScores).isEmpty();
        assertThat(evaluation.acknowledgedReviewItems).isEmpty();
    }
}
