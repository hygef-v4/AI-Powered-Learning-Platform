package edu.aiplatform.assignments.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import edu.aiplatform.ai.contract.AiTaskResult;
import edu.aiplatform.ai.contract.ApplyOutcome;
import edu.aiplatform.ai.contract.AssignmentSourceText;
import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.files.contract.StoredFileInfo;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.domain.Assignment;
import edu.aiplatform.assignments.domain.AssignmentAiDraft;
import edu.aiplatform.assignments.domain.AssignmentRubric;
import edu.aiplatform.shared.error.ApiException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
        when(f.drafts.find(draft.id())).thenReturn(Optional.of(draft));
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
        assertThat(f.aiDrafts.onReady(result(draft, OUTPUT))).isEqualTo(ApplyOutcome.REJECTED);
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
        assertThat(f.aiDrafts.onReady(result(draft, OUTPUT))).isEqualTo(ApplyOutcome.APPLIED);
        verify(f.drafts).markReady(eq(draft.id()), argThat((JsonNode p) -> p.path("sections").size() == 3
                && p.path("sections").get(1).path("rubric").get(0).path("kind").asText().equals("DIAGRAM")
                && p.path("validation").path("status").asText().equals("VALID")), any());
        assertThat(f.store.get(a.id()).version()).isEqualTo(a.version());
        assertThat(f.aiDrafts.onReady(result(draft, "không phải json"))).isEqualTo(ApplyOutcome.REJECTED);
    }

    @Test
    void applyReplacesSkeletonComputesPartsAndFillsRubrics() {
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, new DocumentModel(List.of()), false);
        JsonNode proposal = AiDraftProposal.normalize(read(OUTPUT), true);
        AssignmentAiDraft ready = new AssignmentAiDraft(UUID.randomUUID(), a.id(), AssignmentAiDraft.Origin.MCP, null, null,
                AssignmentAiDraft.Status.READY, proposal, JsonNodeFactory.instance.objectNode().put("origin", "MCP"),
                a.version(), null, f.teacher.accountId(), AssignmentFixtures.NOW, null);
        when(f.drafts.find(ready.id())).thenReturn(Optional.of(ready));
        when(f.drafts.markApplied(eq(ready.id()), any())).thenReturn(1);
        try {
            f.aiDrafts.apply(f.teacher, a.id(), ready.id(), new AssignmentCommands.ApplyDraft(a.version() - 1, true));
            throw new AssertionError("Cần 409");
        } catch (ApiException e) {
            assertThat(e.code()).isEqualTo("ASSIGNMENT_VERSION_CONFLICT");
        }
        AssignmentViews.Detail applied = f.aiDrafts.apply(f.teacher, a.id(), ready.id(),
                new AssignmentCommands.ApplyDraft(a.version(), true, true));
        assertThat(applied.instructions()).isEqualTo("Làm đủ các mục");
        assertThat(applied.title()).isEqualTo("Đề thiết kế hệ thống");
        assertThat(applied.parts()).extracting(AssignmentViews.Part::title).containsExactly("Phân tích › Use case", "Thiết kế");
        assertThat(applied.skeleton().blocks()).filteredOn(b -> b.type() == BlockType.HEADING).hasSize(3);
        assertThat(applied.skeleton().blocks().get(0).type()).isEqualTo(BlockType.PARAGRAPH);
        List<AssignmentRubric> rubrics = f.rubricsOf(a.id());
        assertThat(rubrics).hasSize(2);
        assertThat(rubrics).filteredOn(r -> !r.criteria().isEmpty()).hasSize(2)
                .extracting(AssignmentRubric::totalPoints).containsExactlyInAnyOrder(
                        new java.math.BigDecimal("2.00"), new java.math.BigDecimal("3.00"));
        verify(f.audit).record(argThat(e -> e.action().equals("ASSIGNMENT_AI_DRAFT_APPLIED")));
    }

    @Test
    void invalidGradedProposalCannotBeAppliedAndDoesNotChangeAssignment() {
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, new DocumentModel(List.of()), false);
        JsonNode proposal = AiDraftProposal.normalize(read(OUTPUT), true);
        ((com.fasterxml.jackson.databind.node.ArrayNode) proposal.path("sections").get(2).path("rubric")).removeAll();
        AssignmentAiDraft ready = new AssignmentAiDraft(UUID.randomUUID(), a.id(), AssignmentAiDraft.Origin.MCP, null, null,
                AssignmentAiDraft.Status.READY, proposal, JsonNodeFactory.instance.objectNode().put("origin", "MCP"),
                a.version(), null, f.teacher.accountId(), AssignmentFixtures.NOW, null);
        when(f.drafts.find(ready.id())).thenReturn(Optional.of(ready));

        try {
            f.aiDrafts.apply(f.teacher, a.id(), ready.id(), new AssignmentCommands.ApplyDraft(a.version(), false));
            throw new AssertionError("Đề Graded thiếu rubric phải bị chặn");
        } catch (ApiException e) {
            assertThat(e.code()).isEqualTo("AI_DRAFT_INVALID");
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
        when(f.drafts.find(ready.id())).thenReturn(Optional.of(ready));

        try {
            f.aiDrafts.apply(f.teacher, a.id(), ready.id(), new AssignmentCommands.ApplyDraft(a.version(), false));
            throw new AssertionError("Không được áp dụng đề đã bị đánh dấu INVALID");
        } catch (ApiException e) {
            assertThat(e.code()).isEqualTo("AI_DRAFT_INVALID");
        }
        assertThat(f.store.get(a.id()).skeleton().blocks()).isEmpty();
        assertThat(f.rubricsOf(a.id())).allSatisfy(r -> assertThat(r.criteria()).isEmpty());
    }

    @Test
    void agentDraftNeedsDraftStatusAndExpectedVersion() {
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, new DocumentModel(List.of()), false);
        try {
            f.aiDrafts.saveFromAgent(f.teacher, a.id(), a.version() + 5, read(OUTPUT), "agent");
            throw new AssertionError("Cần 409");
        } catch (ApiException e) {
            assertThat(e.code()).isEqualTo("ASSIGNMENT_VERSION_CONFLICT");
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
