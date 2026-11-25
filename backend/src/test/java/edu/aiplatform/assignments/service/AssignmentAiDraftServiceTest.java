package edu.aiplatform.assignments.service;

import edu.aiplatform.assignments.dto.AssignmentViews;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import edu.aiplatform.ai.contract.AiTaskResult;
import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.ai.contract.AiTaskState;
import edu.aiplatform.ai.contract.AiTaskView;
import edu.aiplatform.ai.contract.ApplyOutcome;
import edu.aiplatform.ai.contract.AssignmentSourceText;
import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.files.contract.StoredFileInfo;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.model.Assignment;
import edu.aiplatform.assignments.model.AssignmentAiDraft;
import edu.aiplatform.assignments.model.AssignmentRubric;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** F6, F15; BR-U07-50…53; P5: đề xuất không sửa thẳng bài; áp dụng cần version khớp. */
class AssignmentAiDraftServiceTest {

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(AssignmentScope.class)
    void mcpCreationProducesDraftAndSeparateReviewableProposalWithoutProviderCall(AssignmentScope scope) throws Exception {
        var writes = new edu.aiplatform.ai.contract.IMcpWriteRequest() {
            public java.util.Map<String,Object> execute(edu.aiplatform.identity.contract.Actor actor,String operation,
                    String ref,String fingerprint,java.util.function.Supplier<java.util.Map<String,Object>> mutation) {return mutation.get();}
        };
        var service = new AssignmentMcpAuthoringService(f.authoring,f.aiDrafts,f.access,writes,f.json);
        var content=(java.util.Map<String,Object>)service.call(scope==AssignmentScope.CLASS?f.teacher:f.manager,"create_assignment_draft",java.util.Map.of(
                scope==AssignmentScope.CLASS?"classId":"subjectId",(scope==AssignmentScope.CLASS?f.classId:f.subjectId).toString(),
                "title","MCP QA draft","requestRef","create-qa", "draft",f.json.readTree(OUTPUT))).content();
        var id=UUID.fromString((String)content.get("assignmentId"));
        assertThat(f.store.get(id).status()).isEqualTo(AssignmentStatus.DRAFT);
        assertThat(f.store.get(id).skeleton().blocks()).isEmpty();
        assertThat(content.get("proposalId")).isNotNull();
        verify(f.drafts).insert(argThat(d -> d.assignmentId().equals(id) && d.status()==AssignmentAiDraft.Status.READY));
        verify(f.aiTasks,never()).submit(any());
    }

    private final AssignmentFixtures f = new AssignmentFixtures();

    @Test
    void templatesAreNonRetiredSubjectAssignmentsOfTheSameSubject() {
        Assignment mine = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, new DocumentModel(List.of()), false);
        Assignment published = f.put(AssignmentScope.SUBJECT, AssignmentStatus.OPEN, new DocumentModel(List.of()), false);
        Assignment draft = f.put(AssignmentScope.SUBJECT, AssignmentStatus.DRAFT, new DocumentModel(List.of()), false);
        f.put(AssignmentScope.SUBJECT, AssignmentStatus.RETIRED, new DocumentModel(List.of()), false);
        f.put(AssignmentScope.CLASS, AssignmentStatus.OPEN, new DocumentModel(List.of()), false);

        assertThat(f.aiDrafts.listTemplates(f.teacher, mine.id()))
                .extracting(AssignmentViews.AiTemplateOption::assignmentId)
                .containsExactlyInAnyOrder(published.id(), draft.id());
    }

    private static final String OUTPUT = """
            {"schemaVersion":2,"title":"Đề thiết kế hệ thống","businessContext":"FPT runs events and tracks registration capacity.","instructions":"Làm đủ các mục","sections":[
             {"level":1,"title":"Phân tích","guidance":"","rubric":[]},
             {"level":2,"title":"Use case","guidance":"Vẽ use case","rubric":[{"title":"Đúng","description":"",
               "kind":"DIAGRAM","items":[{"text":"Đủ actor","points":2}]}]},
             {"level":1,"title":"Thiết kế","guidance":"Mô tả lớp","rubric":[{"title":"Coverage","description":"",
               "kind":"TEXT","items":[{"text":"Covers the stated constraints","points":3}]}]}]}
            """;

    private AssignmentAiDraft pending(Assignment a) {
        AssignmentAiDraft draft = new AssignmentAiDraft(UUID.randomUUID(), a.id(), AssignmentAiDraft.Origin.AI_TASK, null,
                "r1", AssignmentAiDraft.Status.PENDING, null, JsonNodeFactory.instance.objectNode().put("origin", "AI_TASK"),
                a.version(), null, f.teacher.accountId(), AssignmentFixtures.NOW, null);
        when(f.drafts.findDraft(draft.id())).thenReturn(Optional.of(draft));
        return draft;
    }

    private AiTaskResult result(AssignmentAiDraft draft, String output) {
        return new AiTaskResult(UUID.randomUUID(), AssignmentAiDraftService.TASK_TYPE, f.teacher,
                AssignmentAiDraftService.TARGET_PREFIX + draft.id(), draft.baseVersion(), output);
    }

    @Test
    void resultForPublishedAssignmentIsRejected() {
        Assignment open = f.put(AssignmentScope.CLASS, AssignmentStatus.OPEN, new DocumentModel(List.of()), true);
        AssignmentAiDraft draft = pending(open);
        assertThat(f.aiDrafts.handleReadyResult(result(draft, OUTPUT))).isEqualTo(ApplyOutcome.REJECTED);
        verify(f.drafts).markFailed(eq(draft.id()), anyString(), any());
    }

    @Test
    void sourcePreviewIsBoundedAndDoesNotCallAiOrChangeAssignment() {
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, new DocumentModel(List.of()), false);
        when(f.artifacts.attach(any(), eq(f.teacher), eq(ArtifactPurpose.ASSIGNMENT_SOURCE)))
                .thenReturn(new StoredFileInfo("private-file", f.teacher.accountId(), ArtifactPurpose.ASSIGNMENT_SOURCE,
                        "brief.txt", "text/plain", 1500, "hash"));
        when(f.sourceTexts.extract(anyList(), eq(30_000)))
                .thenReturn(List.of(new AssignmentSourceText("brief.txt", "x".repeat(1500), false)));

        var preview = f.aiDrafts.previewSources(f.teacher, a.id(), List.of("signed-ref"));

        assertThat(preview).hasSize(1);
        assertThat(preview.get(0).previewText()).hasSize(1000);
        assertThat(preview.get(0).extractedChars()).isEqualTo(1500);
        assertThat(f.store.get(a.id()).version()).isEqualTo(a.version());
        verify(f.aiTasks, never()).submit(any());
    }

    @Test
    void validResultIsStoredAsProposalWithoutTouchingAssignment() {
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, new DocumentModel(List.of()), false);
        AssignmentAiDraft draft = pending(a);
        when(f.drafts.markReady(eq(draft.id()), any(), any())).thenReturn(1);
        assertThat(f.aiDrafts.handleReadyResult(result(draft, OUTPUT))).isEqualTo(ApplyOutcome.APPLIED);
        verify(f.drafts).markReady(eq(draft.id()), argThat((JsonNode p) -> p.path("sections").size() == 3
                && p.path("sections").get(1).path("rubric").get(0).path("kind").asText().equals("DIAGRAM")
                && p.path("validation").path("status").asText().equals("VALID")), any());
        assertThat(f.store.get(a.id()).version()).isEqualTo(a.version());
        assertThat(f.aiDrafts.handleReadyResult(result(draft, "không phải json"))).isEqualTo(ApplyOutcome.REJECTED);
    }

    @Test
    void applyReplacesSkeletonComputesPartsAndFillsRubrics() {
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, new DocumentModel(List.of()), false);
        JsonNode proposal = AiDraftProposal.normalize(read(OUTPUT), true);
        AssignmentAiDraft ready = new AssignmentAiDraft(UUID.randomUUID(), a.id(), AssignmentAiDraft.Origin.MCP, null, null,
                AssignmentAiDraft.Status.READY, proposal, JsonNodeFactory.instance.objectNode().put("origin", "MCP"),
                a.version(), null, f.teacher.accountId(), AssignmentFixtures.NOW, null);
        when(f.drafts.findDraft(ready.id())).thenReturn(Optional.of(ready));
        when(f.drafts.markApplied(eq(ready.id()), any())).thenReturn(1);
        try {
            f.aiDrafts.applyDraft(f.teacher, a.id(), ready.id(), new AssignmentCommands.ApplyDraft(a.version() - 1, true));
            throw new AssertionError("Cần 409");
        } catch (ApiException e) {
            assertThat(e.readCode()).isEqualTo("ASSIGNMENT_VERSION_CONFLICT");
        }
        AssignmentViews.Detail applied = f.aiDrafts.applyDraft(f.teacher, a.id(), ready.id(),
                new AssignmentCommands.ApplyDraft(a.version(), true, true));
        assertThat(applied.instructions()).isEqualTo("Làm đủ các mục");
        assertThat(applied.title()).isEqualTo("Đề thiết kế hệ thống");
        assertThat(applied.parts()).extracting(AssignmentViews.Part::title).containsExactly("Phân tích › Use case", "Thiết kế");
        assertThat(applied.skeleton().blocks()).filteredOn(b -> b.type() == BlockType.HEADING).hasSize(3);
        assertThat(applied.skeleton().blocks().get(0).type()).isEqualTo(BlockType.PARAGRAPH);
        List<AssignmentRubric> rubrics = f.rubricsOf(a.id());
        // Hai mục chấm và rubric Toàn bài (trống).
        assertThat(rubrics).hasSize(3);
        assertThat(rubrics).filteredOn(r -> !r.criteria().isEmpty()).hasSize(2)
                .extracting(AssignmentRubric::totalPoints).containsExactlyInAnyOrder(
                        new java.math.BigDecimal("2.00"), new java.math.BigDecimal("3.00"));
    }

    @Test
    void invalidGradedProposalCannotBeAppliedAndDoesNotChangeAssignment() {
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, new DocumentModel(List.of()), false);
        JsonNode proposal = AiDraftProposal.normalize(read(OUTPUT), true);
        ((com.fasterxml.jackson.databind.node.ArrayNode) proposal.path("sections").get(2).path("rubric")).removeAll();
        AssignmentAiDraft ready = new AssignmentAiDraft(UUID.randomUUID(), a.id(), AssignmentAiDraft.Origin.MCP, null, null,
                AssignmentAiDraft.Status.READY, proposal, JsonNodeFactory.instance.objectNode().put("origin", "MCP"),
                a.version(), null, f.teacher.accountId(), AssignmentFixtures.NOW, null);
        when(f.drafts.findDraft(ready.id())).thenReturn(Optional.of(ready));

        try {
            f.aiDrafts.applyDraft(f.teacher, a.id(), ready.id(), new AssignmentCommands.ApplyDraft(a.version(), false));
            throw new AssertionError("Đề Graded thiếu rubric phải bị chặn");
        } catch (ApiException e) {
            assertThat(e.readCode()).isEqualTo("AI_DRAFT_INVALID");
        }
        assertThat(f.store.get(a.id()).skeleton().blocks()).isEmpty();
        assertThat(f.rubricsOf(a.id())).allSatisfy(r -> assertThat(r.criteria()).isEmpty());
    }

    @Test
    void proposalPreviouslyMarkedInvalidCannotBeAppliedEvenIfCurrentChecksPass() {
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, new DocumentModel(List.of()), false);
        com.fasterxml.jackson.databind.node.ObjectNode proposal = (com.fasterxml.jackson.databind.node.ObjectNode)
                AiDraftProposal.normalize(read(OUTPUT), true);
        proposal.with("validation").put("status", "INVALID");
        AssignmentAiDraft ready = new AssignmentAiDraft(UUID.randomUUID(), a.id(), AssignmentAiDraft.Origin.MCP, null, null,
                AssignmentAiDraft.Status.READY, proposal, JsonNodeFactory.instance.objectNode().put("origin", "MCP"),
                a.version(), null, f.teacher.accountId(), AssignmentFixtures.NOW, null);
        when(f.drafts.findDraft(ready.id())).thenReturn(Optional.of(ready));

        try {
            f.aiDrafts.applyDraft(f.teacher, a.id(), ready.id(), new AssignmentCommands.ApplyDraft(a.version(), false));
            throw new AssertionError("Không được áp dụng đề đã bị đánh dấu INVALID");
        } catch (ApiException e) {
            assertThat(e.readCode()).isEqualTo("AI_DRAFT_INVALID");
        }
        assertThat(f.store.get(a.id()).skeleton().blocks()).isEmpty();
        assertThat(f.rubricsOf(a.id())).allSatisfy(r -> assertThat(r.criteria()).isEmpty());
    }

    @Test
    void sameTotalWithWrongQuestionPointsIsRejectedAtApply() {
        var assignment = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, new DocumentModel(List.of()), false);
        var proposal = (com.fasterxml.jackson.databind.node.ObjectNode) read("""
                {"schemaVersion":2,"title":"Draft","businessContext":"A lending service has reservations and returns.",
                 "sections":[
                  {"level":1,"title":"Cover","rubric":[{"title":"Cover","items":[{"text":"Identify document","points":0.5}]}]},
                  {"level":1,"title":"Context","rubric":[{"title":"Context","items":[{"text":"Model actors","points":3}]}]},
                  {"level":1,"title":"Use case","rubric":[{"title":"Scope","items":[{"text":"Model flows","points":3.5}]}]},
                  {"level":1,"title":"ERD","rubric":[{"title":"Data","items":[{"text":"Model entities","points":3}]}]}]}
                """);
        var provenance = f.json.createObjectNode();
        provenance.putObject("draftOptions").put("questionCount", 4).put("targetPoints", 10)
                .putArray("questionPoints").add(0.5).add(2.5).add(4).add(3);
        var ready = new AssignmentAiDraft(UUID.randomUUID(), assignment.id(), AssignmentAiDraft.Origin.AI_TASK,
                null, "points-test", AssignmentAiDraft.Status.READY, proposal, provenance, assignment.version(), null,
                f.teacher.accountId(), AssignmentFixtures.NOW, null);
        when(f.drafts.findDraft(ready.id())).thenReturn(Optional.of(ready));
        when(f.drafts.markApplied(eq(ready.id()), any())).thenReturn(1);

        assertThatThrownBy(() -> f.aiDrafts.applyDraft(f.teacher, assignment.id(), ready.id(),
                new AssignmentCommands.ApplyDraft(assignment.version(), true)))
                .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.readCode()).isEqualTo("AI_DRAFT_INVALID"));
        assertThat(f.store.get(assignment.id()).skeleton().blocks()).isEmpty();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
    void resolvedQuestionPointsAreSentAndKeptForValidation(boolean preserveTemplate) throws Exception {
        var assignment = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, new DocumentModel(List.of()), false);
        var template = f.put(AssignmentScope.SUBJECT, AssignmentStatus.DRAFT, new DocumentModel(List.of(
                AssignmentFixtures.heading("H1", "Context"), AssignmentFixtures.heading("H1", "Use case"))), true);
        var secondRubric = f.rubricsOf(template.id()).get(1);
        var secondCriterion = secondRubric.criteria().get(0);
        var secondItem = secondCriterion.items().get(0);
        var newItem = new edu.aiplatform.assignments.contract.RubricItem(secondItem.itemId(), secondItem.text(),
                new BigDecimal("6.25"));
        f.rubricStore.put(secondRubric.id(), secondRubric.replaceCriteria(List.of(
                new edu.aiplatform.assignments.contract.RubricCriterion(secondCriterion.criterionId(),
                        secondCriterion.title(), secondCriterion.description(), secondCriterion.kind(), List.of(newItem))),
                AssignmentFixtures.NOW));
        var inserted = new AtomicReference<AssignmentAiDraft>();
        doAnswer(call -> {
            inserted.set(call.getArgument(0));
            return null;
        }).when(f.drafts).insert(any());
        when(f.drafts.findDraft(any())).thenAnswer(call -> Optional.ofNullable(inserted.get()));
        when(f.aiTasks.submit(any())).thenReturn(new AiTaskView(UUID.randomUUID(), AiTaskState.QUEUED,
                AiSource.PERSONAL, AiProvider.GEMINI, "mock", null, null, null));

        var command = preserveTemplate ? preservePointsRequest(template.id(), null)
                : new AssignmentCommands.AiDraftRequest("Soạn đề theo mẫu", List.of(), List.of(),
                        AiSource.PERSONAL, AiProvider.GEMINI, "explicit-points", new AssignmentCommands.DraftOptions(
                        "AUTO", "EN", null, null, List.of(new BigDecimal("5.00"), new BigDecimal("6.25"))),
                        null, template.id(), null, false);
        f.aiDrafts.request(f.teacher, assignment.id(), command);

        verify(f.aiTasks).submit(argThat(request -> request.prompt().user().contains("topLevelQuestionCount=2")
                && request.prompt().user().contains("targetTotalPoints=11.25")
                && request.prompt().user().contains("confirmedQuestionPointsInOrder=[5.00, 6.25]")
                && request.prompt().user().contains("<format_template>")));
        assertThat(inserted.get().provenance().path("draftOptions").path("questionPoints"))
                .extracting(JsonNode::decimalValue).containsExactly(new BigDecimal("5.00"), new BigDecimal("6.25"));
        assertThat(inserted.get().provenance().path("templateReviewRequired").asBoolean()).isTrue();

        f.rubricStore.put(secondRubric.id(), secondRubric);
        var response = """
                {"schemaVersion":2,"title":"Draft","businessContext":"A lending service tracks tools.","sections":[
                {"level":1,"title":"Context","rubric":[{"title":"Scope","items":[{"text":"Model actors","points":5}]}]},
                {"level":1,"title":"Use case","rubric":[{"title":"Flows","items":[{"text":"Model flows","points":6.25}]}]}]}
                """;
        when(f.drafts.markReady(eq(inserted.get().id()), any(), any())).thenReturn(1);
        assertThat(f.aiDrafts.handleReadyResult(result(inserted.get(), response))).isEqualTo(ApplyOutcome.APPLIED);
        verify(f.drafts).markReady(eq(inserted.get().id()), argThat((JsonNode proposal) ->
                proposal.path("validation").path("status").asText().equals("VALID")), any());
    }

    @Test
    void keepTemplatePointsRejectsIncompleteUnavailableOrIncompatibleRequestsBeforeDispatch() {
        var assignment = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, new DocumentModel(List.of()), false);
        var skeleton = new DocumentModel(List.of(AssignmentFixtures.heading("H1", "Context")));
        var incomplete = f.put(AssignmentScope.SUBJECT, AssignmentStatus.DRAFT, skeleton, false);
        var complete = f.put(AssignmentScope.SUBJECT, AssignmentStatus.DRAFT, skeleton, true);
        assertThatThrownBy(() -> f.aiDrafts.request(f.teacher, assignment.id(), preservePointsRequest(incomplete.id(), null)))
                .isInstanceOfSatisfying(ApiException.class,
                        error -> assertThat(error.readCode()).isEqualTo("AI_TEMPLATE_POINTS_UNAVAILABLE"));
        assertThatThrownBy(() -> f.aiDrafts.request(f.teacher, assignment.id(), preservePointsRequest(UUID.randomUUID(), null)))
                .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.readCode()).isEqualTo("AI_TEMPLATE_INVALID"));
        assertThatThrownBy(() -> f.aiDrafts.request(f.teacher, assignment.id(), preservePointsRequest(complete.id(), BigDecimal.TEN)))
                .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.readCode()).isEqualTo("AI_OPTIONS_INVALID"));

        f.store.put(assignment.id(), assignment.updateDetails(assignment.title(), assignment.instructions(), true,
                GradingMode.PRACTICE, assignment.requiredDiagrams(), List.of(), false, null, AssignmentFixtures.NOW));
        assertThatThrownBy(() -> f.aiDrafts.request(f.teacher, assignment.id(), preservePointsRequest(complete.id(), null)))
                .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.readCode()).isEqualTo("AI_OPTIONS_INVALID"));
        verify(f.drafts, never()).insert(any());
        verify(f.aiTasks, never()).submit(any());
    }

    private static AssignmentCommands.AiDraftRequest preservePointsRequest(UUID templateId, BigDecimal targetPoints) {
        return new AssignmentCommands.AiDraftRequest("Soạn đề theo mẫu", List.of(), List.of(),
                AiSource.PERSONAL, AiProvider.GEMINI, "points-request", new AssignmentCommands.DraftOptions(
                "AUTO", "EN", null, targetPoints), null, templateId, null, true);
    }

    @Test
    void explicitQuestionPointsDetermineCountAndTotalAndRejectConflicts() {
        List<BigDecimal> points = List.of(new BigDecimal("0.50"), new BigDecimal("2.50"),
                new BigDecimal("4.00"), new BigDecimal("3.00"));
        var options = AssignmentAiDraftService.normalizeOptions(
                new AssignmentCommands.DraftOptions("AUTO", "EN", null, null, points));
        assertThat(options.targetPoints()).isEqualByComparingTo("10.00");
        assertThat(options.questionCount()).isEqualTo(4);
        assertThat(options.questionPoints()).containsExactlyElementsOf(points);
        for (var invalid : List.of(new AssignmentCommands.DraftOptions("AUTO", "EN", 2, null, points),
                new AssignmentCommands.DraftOptions("AUTO", "EN", null, new BigDecimal("9.50"), points),
                new AssignmentCommands.DraftOptions("AUTO", "EN", null, null, List.of(new BigDecimal("0.125"))))) {
            assertThatThrownBy(() -> AssignmentAiDraftService.normalizeOptions(invalid))
                    .isInstanceOfSatisfying(ApiException.class,
                            error -> assertThat(error.readCode()).isEqualTo("AI_OPTIONS_INVALID"));
        }
    }

    @Test
    void authoringConflictIsStoredForReviewWithoutApplyingTheProposal() throws Exception {
        var assignment = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, new DocumentModel(List.of()), false);
        var draft = pending(assignment);
        var output = (com.fasterxml.jackson.databind.node.ObjectNode) read(OUTPUT);
        output.putArray("authoringWarnings").addObject().put("code", "TEMPLATE_CONFLICT")
                .put("message", "Question 1 names both Order and Dish for the state diagram.");
        when(f.drafts.markReady(eq(draft.id()), any(), any())).thenReturn(1);

        assertThat(f.aiDrafts.handleReadyResult(result(draft, f.json.writeValueAsString(output))))
                .isEqualTo(ApplyOutcome.APPLIED);
        verify(f.drafts).markReady(eq(draft.id()), argThat((JsonNode proposal) -> {
            var validation = proposal.path("validation");
            var issue = validation.path("issues").path(0);
            return validation.path("status").asText().equals("NEEDS_REVIEW")
                    && issue.path("code").asText().equals("TEMPLATE_CONFLICT")
                    && issue.path("message").asText().contains("Order and Dish");
        }), any());
        assertThat(f.store.get(assignment.id()).version()).isEqualTo(assignment.version());
    }

    @Test
    void agentDraftNeedsDraftStatusAndExpectedVersion() {
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, new DocumentModel(List.of()), false);
        try {
            f.aiDrafts.saveFromAgent(f.teacher, a.id(), a.version() + 5, read(OUTPUT), "agent");
            throw new AssertionError("Cần 409");
        } catch (ApiException e) {
            assertThat(e.readCode()).isEqualTo("ASSIGNMENT_VERSION_CONFLICT");
        }
        AssignmentAiDraft saved = f.aiDrafts.saveFromAgent(f.teacher, a.id(), a.version(), read(OUTPUT), "agent");
        assertThat(saved.status()).isEqualTo(AssignmentAiDraft.Status.READY);
        assertThat(saved.provenance().path("agentLabel").asText()).isEqualTo("agent");
        assertThat(f.store.get(a.id()).skeleton().blocks()).isEmpty();
    }

    private JsonNode read(String text) {
        try {
            return f.json.readTree(text);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
