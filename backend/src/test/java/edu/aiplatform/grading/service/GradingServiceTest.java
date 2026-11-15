package edu.aiplatform.grading.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    final IEvaluationRepository evaluationRepository = mock(IEvaluationRepository.class);
    final IAuthorization auth = mock(IAuthorization.class);
    final IAudit audit = mock(IAudit.class);
    final IEventPublisher eventPublisher = mock(IEventPublisher.class);
    final ISubmissionQuery submissionQuery = mock(ISubmissionQuery.class);
    final IClassAccess classAccess = mock(IClassAccess.class);
    final UUID teacherId = UUID.randomUUID(), classId = UUID.randomUUID(), assignmentId = UUID.randomUUID(),
            studentId = UUID.randomUUID();
    final Actor teacher = new Actor(teacherId, Role.TEACHER);
    final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    final UUID rubricId = UUID.randomUUID(), itemId = UUID.randomUUID(), blockId = UUID.randomUUID();
    final edu.aiplatform.grading.contract.IGroupSubmissionSource groupSource = mock(edu.aiplatform.grading.contract.IGroupSubmissionSource.class);
    final org.springframework.beans.factory.ObjectProvider<edu.aiplatform.grading.contract.IGroupSubmissionSource> groupProvider = mock(org.springframework.beans.factory.ObjectProvider.class);
    Evaluation evaluation;
    GradingService gradingService;

    @BeforeEach
    void setup() throws Exception {
        var rubrics = List.of(new RubricView(
                rubricId, assignmentId, "p", List.of(new RubricCriterion(UUID.randomUUID(), "Criterion", "",
                        CriterionKind.values()[0], List.of(new RubricItem(itemId, "Item", new BigDecimal("4"))))),
                new BigDecimal("4")));
        evaluation = Evaluation.createPendingEvaluation(UUID.randomUUID(), assignmentId, classId, studentId, "hash",
                new BigDecimal("4"), objectMapper.writeValueAsString(rubrics), Instant.now());
        when(evaluationRepository.findById(evaluation.id)).thenReturn(Optional.of(evaluation));
        when(evaluationRepository.lockById(evaluation.id)).thenReturn(Optional.of(evaluation));
        when(evaluationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(auth.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.allow());
        when(classAccess.getClassRef(classId)).thenReturn(Optional.of(new ClassRef(classId,
                UUID.randomUUID(), "SE1", "Class", ClassStatus.OPEN, teacherId, false)));
        when(submissionQuery.getSubmission(evaluation.attemptId))
                .thenReturn(Optional.of(new SubmissionView(evaluation.attemptId, assignmentId, classId, studentId,
                        new DocumentModel(List.of(new DocumentBlock(blockId, BlockOrigin.STUDENT, BlockType.PARAGRAPH,
                                objectMapper.createObjectNode().put("text", "Answer")))),
                        "hash", Instant.now(), false, SubmitMode.values()[0])));
        when(submissionQuery.listSubmissions(assignmentId, classId))
                .thenReturn(List.of(new StudentSubmissionStatus(studentId, evaluation.attemptId, 1,
                        AttemptStatus.SUBMITTED, Instant.now(), false)));
        gradingService = new GradingService(evaluationRepository, new GradingAccess(auth, classAccess, audit, Clock.systemUTC()), audit, eventPublisher,
                submissionQuery, objectMapper, Clock.systemUTC(), groupProvider);
        when(groupProvider.getIfAvailable()).thenReturn(groupSource);
        when(groupSource.getGroupSubmission(any())).thenAnswer(inv -> Optional.of(
                new edu.aiplatform.grading.contract.GroupSubmissionView(evaluation.groupDocumentId, UUID.randomUUID(),
                        assignmentId, classId, new DocumentModel(List.of()), List.of(studentId), List.of(),
                        Instant.now(), false, "hash")));
    }

    @Test
    void manualFeedbackCanSaveAndFinalizeWithOptionalEvidenceAndExplanation() {
        var manual = new GradingFeedback(1,
                new GradingFeedback.Summary(List.of("Clear model"), List.of(), List.of()),
                List.of(new GradingFeedback.Item(rubricId, itemId, null, "", "Add multiplicity",
                        null, false, null)));
        gradingService.saveEvaluation(teacher, evaluation.id, new Save(
                0, Map.of(), BigDecimal.ZERO, null, "Manual feedback", null, null,
                Map.of(rubricId, Map.of(itemId, new BigDecimal("2.00"))),
                manual, false, null, null));
        assertThat(evaluation.structuredFeedback.items().get(0).reason()).isEmpty();
        assertThat(evaluation.structuredFeedback.items().get(0).evidence()).isNull();
        gradingService.finalizeEvaluation(teacher, new ItemVersion(evaluation.id, 0));
        assertThat(evaluation.state).isEqualTo(EvaluationStatus.DRAFT);
        assertThat(evaluation.score).isEqualByComparingTo("2.00");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = EvaluationStatus.class, names = {"FINALIZED", "PUBLISHED"})
    void savedGradesAllowCorrectionsWithoutWithdrawingPublication(EvaluationStatus state) {
        gradingService.saveEvaluation(teacher, evaluation.id, command(0));
        evaluation.state = state;
        var input = new Save(0, Map.of(), BigDecimal.ZERO, null, "replacement", "correction", null);
        gradingService.saveEvaluation(teacher, evaluation.id, input);
        assertThat(evaluation.feedback).isEqualTo("replacement");
        assertThat(evaluation.state).isEqualTo(state == EvaluationStatus.PUBLISHED
                ? EvaluationStatus.PUBLISHED : EvaluationStatus.DRAFT);
    }

    Save command(long version) {
        return new Save(version, Map.of(rubricId, Set.of(itemId)), BigDecimal.ZERO, null, "feedback", null, null);
    }

    @Test
    void publishesWithoutFinalizeAndSavesCorrectionsWithoutRepublishing() {
        gradingService.saveEvaluation(teacher, evaluation.id, command(0));
        gradingService.publishEvaluation(teacher, new ItemVersion(evaluation.id, 0));
        var publishedAt = evaluation.publishedAt;
        gradingService.publishEvaluation(teacher, new ItemVersion(evaluation.id, 0));
        gradingService.saveEvaluation(teacher, evaluation.id,
                new Save(0, Map.of(), BigDecimal.ZERO, null, "Corrected", null, null));
        assertThat(evaluation.state).isEqualTo(EvaluationStatus.PUBLISHED);
        assertThat(evaluation.feedback).isEqualTo("Corrected");
        assertThat(evaluation.publishedAt).isEqualTo(publishedAt);
        verify(eventPublisher, times(1)).publish(any());
    }

    @Test
    void publishedSaveCannotBypassReviewOfFlaggedFeedback() {
        gradingService.saveEvaluation(teacher, evaluation.id, command(0));
        evaluation.state = EvaluationStatus.PUBLISHED;
        evaluation.publishedAt = Instant.now();
        var before = evaluation.feedback;
        assertThatThrownBy(() -> gradingService.saveEvaluation(teacher, evaluation.id,
                new Save(0, Map.of(), BigDecimal.ZERO, null, "Replacement", null, null,
                        Map.of(), sampleFeedback("Review", true, "Unclear diagram"), false,
                        null, null))).isInstanceOfSatisfying(ApiException.class,
                                error -> assertThat(error.code()).isEqualTo("STRUCTURED_FEEDBACK_REVIEW_REQUIRED"));
        assertThat(evaluation.feedback).isEqualTo(before);
    }

    @Test
    void archiveStopsPublishedCorrectionsBeforeAnyMutation() {
        gradingService.saveEvaluation(teacher, evaluation.id, command(0));
        gradingService.publishEvaluation(teacher, new ItemVersion(evaluation.id, 0));
        when(classAccess.getClassRef(classId)).thenReturn(Optional.of(new ClassRef(classId,
                UUID.randomUUID(), "SE1", "Class", ClassStatus.ARCHIVED, teacherId, false)));
        clearInvocations(evaluationRepository);
        assertThatThrownBy(() -> gradingService.saveEvaluation(teacher, evaluation.id,
                new Save(0, Map.of(), null, null, "Must not save", null, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        error -> assertThat(error.code()).isEqualTo("GRADING_CLASS_READ_ONLY"));
        assertThat(evaluation.score).isEqualByComparingTo("4");
        assertThat(evaluation.feedback).isEqualTo("feedback");
        verify(evaluationRepository, never()).save(any());
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
        gradingService.saveEvaluation(teacher, evaluation.id, command(0));
        assertThat(evaluation.score()).isEqualByComparingTo("4");
        assertThat(evaluation.status()).isEqualTo(EvaluationStatus.DRAFT);
    }

    @Test
    void unknownRubricItemRejectedWithoutMutatingGrade() {
        assertThatThrownBy(() -> gradingService.saveEvaluation(teacher, evaluation.id,
                new Save(0, Map.of(rubricId, Set.of(UUID.randomUUID())), null, null, null, null, null)))
                .hasMessageContaining("rubric");
        assertThat(evaluation.score()).isNull();
    }

    @Test
    void changedTeacherAndStaleVersionCannotSave() {
        when(auth.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.deny("OUT_OF_CLASS_SCOPE"));
        assertThatThrownBy(() -> gradingService.saveEvaluation(teacher, evaluation.id, command(0))).isInstanceOfSatisfying(
                ApiException.class, ex -> assertThat(ex.status().value()).isEqualTo(404));
        when(auth.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.allow());
        assertThatThrownBy(() -> gradingService.saveEvaluation(teacher, evaluation.id, command(9))).hasMessageContaining("Reload");
    }

    @Test
    void staleVersionDoesNotWriteAuditOrPublish() {
        assertThatThrownBy(() -> gradingService.saveEvaluation(teacher, evaluation.id, command(9)))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.status().value()).isEqualTo(409));
        verify(evaluationRepository, never()).save(any());
        verifyNoInteractions(audit, eventPublisher);
    }

    @Test
    void oldPublishedAttemptCannotBeEditedAfterResubmission() {
        when(submissionQuery.listSubmissions(assignmentId, classId))
                .thenReturn(List.of(new StudentSubmissionStatus(studentId, UUID.randomUUID(), 2,
                        AttemptStatus.SUBMITTED, Instant.now(), false)));
        assertThatThrownBy(() -> gradingService.saveEvaluation(teacher, evaluation.id, command(0))).hasMessageContaining("latest");
    }

    @Test
    void legacyFinalizeValidatesWithoutLockingAndPublicationEmitsOnce() {
        gradingService.saveEvaluation(teacher, evaluation.id, command(0));
        gradingService.finalizeEvaluation(teacher, new ItemVersion(evaluation.id, 0));
        assertThat(evaluation.status()).isEqualTo(EvaluationStatus.DRAFT);
        gradingService.publishEvaluation(teacher, new ItemVersion(evaluation.id, 0));
        assertThat(evaluation.status()).isEqualTo(EvaluationStatus.PUBLISHED);
        gradingService.publishEvaluation(teacher, new ItemVersion(evaluation.id, 0));
        verify(eventPublisher, times(1))
                .publish(argThat(e -> e.eventType().equals("grade.published") && !e.payload().containsKey("score")));
    }

    @Test
    void commonGroupUpdatesFollowRootButPreserveManualOverrides() {
        evaluation.kind = EvaluationKind.GROUP_DOCUMENT;
        evaluation.groupDocumentId = UUID.randomUUID();
        var member = Evaluation.createPendingEvaluation(null, assignmentId, classId, studentId, "hash", new BigDecimal("4"),
                evaluation.rubricSnapshot, Instant.now());
        member.kind = EvaluationKind.MEMBER;
        member.groupDocumentId = evaluation.groupDocumentId;
        when(evaluationRepository.findForAssignmentAndClass(assignmentId, classId)).thenReturn(List.of(evaluation, member));
        when(evaluationRepository.lockById(member.id)).thenReturn(Optional.of(member));
        gradingService.saveEvaluation(teacher, evaluation.id, command(0));
        gradingService.saveEvaluation(teacher, evaluation.id, new Save(0, Map.of(), BigDecimal.ZERO, null, "new feedback", null, null));
        assertThat(member.score).isEqualByComparingTo("0");
        assertThat(member.feedback).isEqualTo("new feedback");
        gradingService.saveMemberGrade(teacher, evaluation.id, studentId, new MemberSave(0, BigDecimal.ONE, "manual contribution"));
        gradingService.saveEvaluation(teacher, evaluation.id, command(0));
        assertThat(member.score).isEqualByComparingTo("1");
    }

    @Test
    void savesPartialItemPointsAndKeepsChecklistBackwardsCompatible() throws Exception {
        var command = objectMapper.readValue("""
            {"version":0,"checkedItemsByRubric":{"%s":["%s"]},
             "itemScoresByRubric":{"%s":{"%s":"0.50"}}}
            """.formatted(rubricId, itemId, rubricId, itemId), Save.class);
        gradingService.saveEvaluation(teacher, evaluation.id, command);
        assertThat(evaluation.score).isEqualByComparingTo("0.50");
        assertThat(objectMapper.valueToTree(evaluation).path("rubricItemScores").path(rubricId.toString())
                .path(itemId.toString()).decimalValue()).isEqualByComparingTo("0.50");
    }

    @Test
    void rejectsPartialScoresOutsideItemBoundsOrPrecisionWithoutChangingGrade() throws Exception {
        for (String value : List.of("-0.01", "4.01", "0.001")) {
            var command = objectMapper.readValue("""
                {"version":0,"checkedItemsByRubric":{},"itemScoresByRubric":{"%s":{"%s":"%s"}}}
                """.formatted(rubricId, itemId, value), Save.class);
            assertThatThrownBy(() -> gradingService.saveEvaluation(teacher, evaluation.id, command))
                    .isInstanceOf(ApiException.class);
            assertThat(evaluation.score).isNull();
        }
        verify(evaluationRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownItemInPartialScores() throws Exception {
        var command = objectMapper.readValue("""
            {"version":0,"checkedItemsByRubric":{},"itemScoresByRubric":{"%s":{"%s":"0.5"}}}
            """.formatted(rubricId, UUID.randomUUID()), Save.class);
        assertThatThrownBy(() -> gradingService.saveEvaluation(teacher, evaluation.id, command))
                .isInstanceOf(ApiException.class);
        assertThat(evaluation.score).isNull();
    }

    @Test
    void publishedGroupCorrectionsPropagateAndRetainMemberOverrides() {
        evaluation.kind = EvaluationKind.GROUP_DOCUMENT;
        evaluation.groupDocumentId = UUID.randomUUID();
        var member = Evaluation.createPendingEvaluation(null, assignmentId, classId, studentId, "hash", new BigDecimal("4"),
                evaluation.rubricSnapshot, Instant.now());
        member.kind = EvaluationKind.MEMBER;
        member.groupDocumentId = evaluation.groupDocumentId;
        when(evaluationRepository.findForAssignmentAndClass(assignmentId, classId)).thenReturn(List.of(evaluation, member));
        when(evaluationRepository.lockById(member.id)).thenReturn(Optional.of(member));
        gradingService.saveEvaluation(teacher, evaluation.id, new Save(0, Map.of(), null, null, "partial", null, null,
                Map.of(rubricId, Map.of(itemId, new BigDecimal("0.50")))));
        gradingService.publishEvaluation(teacher, new ItemVersion(evaluation.id, 0));
        gradingService.saveEvaluation(teacher, evaluation.id, new Save(0, Map.of(), null, null, "reviewed", null, null,
                Map.of(rubricId, Map.of(itemId, new BigDecimal("0.75")))));
        assertThat(member.score).isEqualByComparingTo("0.75");
        assertThat(member.state).isEqualTo(EvaluationStatus.PUBLISHED);
        gradingService.saveMemberGrade(teacher, evaluation.id, studentId,
                new MemberSave(0, BigDecimal.ONE, "contribution", ContributionLevel.NEEDS_SUPPORT));
        gradingService.saveEvaluation(teacher, evaluation.id, command(0));
        assertThat(member.score).isEqualByComparingTo("1");
        assertThat(member.memberOverride).isTrue();
        assertThat(member.state).isEqualTo(EvaluationStatus.PUBLISHED);
        assertThat(member.contributionLevel).isEqualTo(ContributionLevel.NEEDS_SUPPORT);
    }

    @Test
    void savesStructuredFeedbackAndDistinguishesAbsentFromExplicitClear() {
        var feedback = sampleFeedback("Đạt 2.00/4.00 điểm", false, null);
        gradingService.saveEvaluation(teacher, evaluation.id, new Save(
                0, Map.of(), BigDecimal.ZERO, null, "free-text", null, null,
                Map.of(rubricId, Map.of(itemId, new BigDecimal("2.00"))),
                feedback, false, null, null));
        assertThat(evaluation.structuredFeedback).isEqualTo(feedback);
        assertThat(evaluation.reviewedItemScores.get(rubricId).get(itemId)).isEqualByComparingTo("2.00");

        // Legacy/old-client Save with structuredFeedback=null and clearStructuredFeedback=false preserves existing feedback
        gradingService.saveEvaluation(teacher, evaluation.id, new Save(
                0, Map.of(), BigDecimal.ZERO, null, "updated free-text", null, null,
                Map.of(rubricId, Map.of(itemId, new BigDecimal("2.00")))));
        assertThat(evaluation.structuredFeedback).isEqualTo(feedback);

        // Explicit clearStructuredFeedback=true removes structuredFeedback and review metadata
        gradingService.saveEvaluation(teacher, evaluation.id, new Save(
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
        gradingService.saveEvaluation(teacher, evaluation.id, new Save(
                0, Map.of(), BigDecimal.ZERO, null, "draft 1", null, null,
                Map.of(rubricId, Map.of(itemId, new BigDecimal("2.00"))),
                initialFeedback, false, null, null));
        assertThat(evaluation.reviewedItemScores.get(rubricId).get(itemId)).isEqualByComparingTo("2.00");

        // Teacher changes item score to 3.00 without editing the item feedback text or confirming
        gradingService.saveEvaluation(teacher, evaluation.id, new Save(
                0, Map.of(), BigDecimal.ZERO, null, "draft 2", null, null,
                Map.of(rubricId, Map.of(itemId, new BigDecimal("3.00"))),
                initialFeedback, false, null, null));
        // Draft save is allowed, but reviewedItemScores stays at 2.00 so mismatch is tracked
        assertThat(evaluation.score).isEqualByComparingTo("3.00");
        assertThat(evaluation.reviewedItemScores.get(rubricId).get(itemId)).isEqualByComparingTo("2.00");

        // Finalizing while score != reviewedScore is rejected on the server
        assertThatThrownBy(() -> gradingService.finalizeEvaluation(teacher, new ItemVersion(evaluation.id, 0)))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.code()).isEqualTo("STRUCTURED_FEEDBACK_SCORE_MISMATCH"));
        assertThat(evaluation.state).isEqualTo(EvaluationStatus.DRAFT);

        // Explicit teacher confirmation via acknowledgedReviewItems resolves the score mismatch
        gradingService.saveEvaluation(teacher, evaluation.id, new Save(
                0, Map.of(), BigDecimal.ZERO, null, "draft 2 confirmed", null, null,
                Map.of(rubricId, Map.of(itemId, new BigDecimal("3.00"))),
                initialFeedback, false, Map.of(rubricId, Set.of(itemId)), null));
        assertThat(evaluation.reviewedItemScores.get(rubricId).get(itemId)).isEqualByComparingTo("3.00");

        gradingService.finalizeEvaluation(teacher, new ItemVersion(evaluation.id, 0));
        assertThat(evaluation.state).isEqualTo(EvaluationStatus.DRAFT);
    }

    @Test
    void unacknowledgedAiRequiresReviewBlocksFinalizationAndRejectsSilentUncheck() throws Exception {
        var proposal = new GradingProposal();
        proposal.id = UUID.randomUUID();
        proposal.evaluationId = evaluation.id;
        proposal.status = "READY";
        proposal.submissionHash = "hash";
        proposal.rubricFingerprint = evaluation.rubricSnapshot;
        proposal.proposal = objectMapper.readTree("""
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
        assertThatThrownBy(() -> gradingService.saveCheckedEvaluation(teacher, evaluation.id, new Save(
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
        gradingService.saveCheckedEvaluation(teacher, evaluation.id, new Save(
                0, Map.of(), BigDecimal.ZERO, null, "Cần xem lại", null, proposal.id,
                Map.of(rubricId, Map.of(itemId, new BigDecimal("2.00"))),
                flaggedFeedback, false, null, null), proposal);
        assertThatThrownBy(() -> gradingService.finalizeEvaluation(teacher, new ItemVersion(evaluation.id, 0)))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.code()).isEqualTo("STRUCTURED_FEEDBACK_REVIEW_REQUIRED"));

        // Forged acknowledgement for unknown itemId is rejected
        assertThatThrownBy(() -> gradingService.saveEvaluation(teacher, evaluation.id, new Save(
                0, Map.of(), BigDecimal.ZERO, null, "Cần xem lại", null, null,
                Map.of(rubricId, Map.of(itemId, new BigDecimal("2.00"))),
                flaggedFeedback, false, Map.of(rubricId, Set.of(UUID.randomUUID())), null)))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.code()).isEqualTo("INVALID_REVIEW_METADATA"));

        // Explicit teacher acknowledgement allows finalization
        gradingService.saveEvaluation(teacher, evaluation.id, new Save(
                0, Map.of(), BigDecimal.ZERO, null, "Đã kiểm tra", null, null,
                Map.of(rubricId, Map.of(itemId, new BigDecimal("2.00"))),
                flaggedFeedback, false, Map.of(rubricId, Set.of(itemId)), null));
        gradingService.finalizeEvaluation(teacher, new ItemVersion(evaluation.id, 0));
        assertThat(evaluation.state).isEqualTo(EvaluationStatus.DRAFT);
    }

    @Test
    void historyAndGroupInheritancePreserveAndResetStructuredFeedbackWithoutAliasing() {
        evaluation.kind = EvaluationKind.GROUP_DOCUMENT;
        evaluation.groupDocumentId = UUID.randomUUID();
        var member = Evaluation.createPendingEvaluation(null, assignmentId, classId, studentId, "hash", new BigDecimal("4"),
                evaluation.rubricSnapshot, Instant.now());
        member.kind = EvaluationKind.MEMBER;
        member.groupDocumentId = evaluation.groupDocumentId;
        when(evaluationRepository.findForAssignmentAndClass(assignmentId, classId)).thenReturn(List.of(evaluation, member));
        when(evaluationRepository.lockById(member.id)).thenReturn(Optional.of(member));

        var feedbackV1 = new GradingFeedback(
                1,
                new GradingFeedback.Summary(List.of("Đúng cấu trúc"), List.of("Cần chi tiết hơn"), List.of("Bổ sung ví dụ")),
                List.of(new GradingFeedback.Item(
                        rubricId, itemId, "Minh chứng nhóm", "Giải thích lần 1", "Gợi ý cải thiện", "p",
                        false, null)));
        gradingService.saveEvaluation(teacher, evaluation.id, new Save(
                0, Map.of(), BigDecimal.ZERO, null, "group v1", null, null,
                Map.of(rubricId, Map.of(itemId, new BigDecimal("2.00"))),
                feedbackV1, false, null, null));
        assertThat(member.structuredFeedback).isEqualTo(feedbackV1);
        assertThat(member.structuredFeedback).isNotSameAs(evaluation.structuredFeedback);

        gradingService.finalizeEvaluation(teacher, new ItemVersion(evaluation.id, 0));

        var feedbackV2 = new GradingFeedback(
                1,
                new GradingFeedback.Summary(List.of("Đúng cấu trúc"), List.of("Cần chi tiết hơn"), List.of("Bổ sung ví dụ")),
                List.of(new GradingFeedback.Item(
                        rubricId, itemId, "Minh chứng nhóm", "Giải thích lần 2 sau khi chỉnh điểm", "Gợi ý cải thiện", "p",
                        false, null)));
        gradingService.saveEvaluation(teacher, evaluation.id, new Save(
                0, Map.of(), BigDecimal.ZERO, null, "group v2", "Cập nhật điểm nhóm", null,
                Map.of(rubricId, Map.of(itemId, new BigDecimal("3.00"))),
                feedbackV2, false, null, null));
        assertThat(evaluation.changes).hasSize(1);
        assertThat(evaluation.structuredFeedback).isEqualTo(feedbackV2);
        assertThat(member.structuredFeedback).isEqualTo(feedbackV2);
        assertThat(member.structuredFeedback).isNotSameAs(evaluation.structuredFeedback);
        assertThat(member.state).isEqualTo(EvaluationStatus.DRAFT);
    }
    @Test
    void rejectsChangedGroupSubmissionBeforeSaving() {
        evaluation.kind = EvaluationKind.GROUP_DOCUMENT;
        evaluation.groupDocumentId = UUID.randomUUID();
        evaluation.submissionHash = "older";
        assertThatThrownBy(() -> gradingService.saveEvaluation(teacher, evaluation.id, command(0)))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("SUBMISSION_CHANGED"));
        verify(evaluationRepository, never()).save(any());
    }
    @Test
    void savedChecksFollowExplicitPartialScores() {
        var input = new Save(0, Map.of(rubricId, Set.of(itemId)), BigDecimal.ZERO, null,
                "partial", null, null, Map.of(rubricId, Map.of(itemId, new BigDecimal("1.25"))), null, null, null, null);
        gradingService.saveEvaluation(teacher, evaluation.id, input);
        assertThat(evaluation.score).isEqualByComparingTo("1.25");
        assertThat(evaluation.rubricChecks).doesNotContain(itemId.toString());
    }
    @Test
    void overridingLegacyMemberUsesTheGroupsRubricBreakdownAndAllowsOptionalReason() {
        evaluation.kind = EvaluationKind.GROUP_DOCUMENT;
        evaluation.groupDocumentId = UUID.randomUUID();
        var member = Evaluation.createPendingEvaluation(null, assignmentId, classId, studentId, "hash",
                new BigDecimal("4"), evaluation.rubricSnapshot, Instant.now());
        member.kind = EvaluationKind.MEMBER;
        member.groupDocumentId = evaluation.groupDocumentId;
        when(evaluationRepository.findForAssignmentAndClass(assignmentId, classId)).thenReturn(List.of(evaluation, member));
        when(evaluationRepository.lockById(member.id)).thenReturn(Optional.of(member));
        gradingService.saveEvaluation(teacher, evaluation.id, command(0));
        member.memberOverride = true;
        member.score = BigDecimal.ONE;
        member.adjustment = BigDecimal.ZERO;
        gradingService.saveMemberGrade(teacher, evaluation.id, studentId, new MemberSave(0, new BigDecimal("2"), null));
        assertThat(member.score).isEqualByComparingTo("2");
        assertThat(member.adjustment).isEqualByComparingTo("-2");
        for (var level : ContributionLevel.values()) {
            gradingService.saveMemberGrade(teacher, evaluation.id, studentId,
                    new MemberSave(0, new BigDecimal("2"), "  Hoàn thành API  ", level));
            assertThat(member.contributionLevel).isEqualTo(level);
            assertThat(member.adjustmentReason).isEqualTo("Hoàn thành API");
            assertThat(member.score).isEqualByComparingTo("2");
        }
        verify(audit).record(argThat(event -> event.action().equals("GRADE_CHANGED")
                && event.details().get("previousContributionLevel").equals("MEETS_EXPECTATIONS")
                && event.details().get("contributionLevel").equals("PARTIALLY_MEETS")));
        assertThatThrownBy(() -> gradingService.saveMemberGrade(teacher, evaluation.id, studentId,
                new MemberSave(99, BigDecimal.ONE, "stale", ContributionLevel.MEETS_EXPECTATIONS)))
                .isInstanceOfSatisfying(ApiException.class,
                        error -> assertThat(error.code()).isEqualTo("GRADE_VERSION_CONFLICT"));
        assertThat(member.contributionLevel).isEqualTo(ContributionLevel.NOT_COMPLETED);
        gradingService.saveMemberGrade(teacher, evaluation.id, studentId,
                new MemberSave(0, new BigDecimal("2"), null, null));
        assertThat(member.contributionLevel).isNull();
        assertThat(member.adjustmentReason).isNull();
        when(classAccess.getClassRef(classId)).thenReturn(Optional.of(new ClassRef(classId,
                UUID.randomUUID(), "SE1", "Class", ClassStatus.ARCHIVED, teacherId, false)));
        assertThatThrownBy(() -> gradingService.saveMemberGrade(teacher, evaluation.id, studentId,
                new MemberSave(0, BigDecimal.ONE, "archived", ContributionLevel.NOT_COMPLETED)))
                .isInstanceOfSatisfying(ApiException.class,
                        error -> assertThat(error.code()).isEqualTo("GRADING_CLASS_READ_ONLY"));
        assertThat(member.contributionLevel).isNull();
        when(auth.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.deny("OUT_OF_CLASS_SCOPE"));
        assertThatThrownBy(() -> gradingService.saveMemberGrade(teacher, evaluation.id, studentId,
                new MemberSave(0, BigDecimal.ONE, "forbidden", ContributionLevel.NOT_COMPLETED)))
                .isInstanceOf(ApiException.class);
        assertThat(member.contributionLevel).isNull();
    }
}
