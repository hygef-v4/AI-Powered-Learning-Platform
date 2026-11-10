package edu.aiplatform.assignments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.BlockOrigin;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.dto.AssignmentViews;
import edu.aiplatform.assignments.model.Assignment;
import edu.aiplatform.assignments.model.AssignmentAiDraft;
import edu.aiplatform.assignments.model.AssignmentRubric;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** AI sửa đề đang có (yêu cầu 2026-11-09): gửi đủ ngữ cảnh, áp dụng giữ ảnh, id và định dạng của khối cũ. */
class AssignmentAiRevisionTest {

    private final AssignmentFixtures f = new AssignmentFixtures();

    private final UUID questionOne = UUID.randomUUID();
    private final UUID paragraph = UUID.randomUUID();
    private final UUID image = UUID.randomUUID();
    private final UUID questionTwo = UUID.randomUUID();
    private final UUID removed = UUID.randomUUID();

    private DocumentModel chineseSkeleton() {
        ObjectNode headingOne = JsonNodeFactory.instance.objectNode().put("style", "H1").put("text", "系统用例分析");
        ObjectNode body = JsonNodeFactory.instance.objectNode();
        body.putArray("runs").addObject().put("text", "请列出核心功能用例").put("bold", true);
        ObjectNode picture = JsonNodeFactory.instance.objectNode().put("fileId", "file-1").put("alt", "图");
        ObjectNode headingTwo = JsonNodeFactory.instance.objectNode().put("style", "H1").put("text", "数据模型设计");
        ObjectNode extra = JsonNodeFactory.instance.objectNode();
        extra.putArray("runs").addObject().put("text", "多余的段落");
        return new DocumentModel(List.of(
                new DocumentBlock(questionOne, BlockOrigin.TEACHER, BlockType.HEADING, headingOne),
                new DocumentBlock(paragraph, BlockOrigin.TEACHER, BlockType.PARAGRAPH, body),
                new DocumentBlock(image, BlockOrigin.TEACHER, BlockType.IMAGE, picture),
                new DocumentBlock(questionTwo, BlockOrigin.TEACHER, BlockType.HEADING, headingTwo),
                new DocumentBlock(removed, BlockOrigin.TEACHER, BlockType.PARAGRAPH, extra)));
    }

    @Test
    void snapshotCarriesWholeDocumentRubricsAndShortBlockRefs() {
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, chineseSkeleton(), true);

        var snapshot = AssignmentDraftRevision.snapshot(a, f.rubricsOf(a.id()));

        JsonNode blocks = snapshot.document().path("blocks");
        assertThat(blocks).hasSize(5);
        assertThat(blocks.get(0).path("text").asText()).isEqualTo("系统用例分析");
        assertThat(blocks.get(0).path("rubric")).isNotEmpty();
        assertThat(blocks.get(1).path("text").asText()).isEqualTo("请列出核心功能用例");
        assertThat(blocks.get(2).path("type").asText()).isEqualTo("IMAGE");
        assertThat(snapshot.document().path("title").asText()).isEqualTo("Bài 1");
        assertThat(snapshot.blockRefs().path("b3").asText()).isEqualTo(image.toString());
        var prompt = AssignmentDraftRevision.buildPrompt(a, "Dịch sang tiếng Anh", snapshot.document());
        assertThat(prompt.user()).contains("Dịch sang tiếng Anh").contains("数据模型设计");
    }

    @Test
    void applyKeepsImageIdsAndFormattingReplacesTextAndRubrics() {
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, chineseSkeleton(), true);
        var snapshot = AssignmentDraftRevision.snapshot(a, f.rubricsOf(a.id()));
        JsonNode raw = read("""
                {"title":"Cemetery system","instructions":"Answer every question","blocks":[
                 {"ref":"b1","type":"HEADING","style":"H1","text":"Use case analysis","rubric":[
                  {"title":"Coverage","kind":"TEXT","items":[{"text":"Lists core use cases","points":15}]}]},
                 {"ref":"b2","type":"PARAGRAPH","text":"List the core use cases"},
                 {"ref":"b3","type":"IMAGE","text":"Figure"},
                 {"ref":"b4","type":"HEADING","style":"H1","text":"Data model design","rubric":[
                  {"title":"Model","items":[{"text":"Defines entities","points":20}]}]},
                 {"type":"PARAGRAPH","text":"A new paragraph"},
                 {"ref":"b99","type":"IMAGE","text":"invented"}],
                 "wholeRubric":[{"title":"Presentation","items":[{"text":"Clear writing","points":5}]}]}
                """);
        ObjectNode proposal = AssignmentDraftRevision.normalize(raw, true);
        proposal.set("validation", AssignmentDraftRevision.validate(proposal, a.gradingMode()));
        assertThat(proposal.path("validation").path("status").asText()).isEqualTo("VALID");
        AssignmentAiDraft ready = ready(a, proposal, snapshot.blockRefs());

        AssignmentViews.Detail applied = f.aiDrafts.apply(f.teacher, a.id(), ready.id(),
                new AssignmentCommands.ApplyDraft(a.version(), true, true));

        assertThat(applied.title()).isEqualTo("Cemetery system");
        assertThat(applied.instructions()).isEqualTo("Answer every question");
        List<DocumentBlock> blocks = applied.skeleton().blocks();
        assertThat(blocks).extracting(DocumentBlock::id).startsWith(questionOne, paragraph, image, questionTwo)
                .doesNotContain(removed).hasSize(5);
        assertThat(blocks.get(1).content().path("runs").get(0).path("text").asText())
                .isEqualTo("List the core use cases");
        assertThat(blocks.get(1).content().path("runs").get(0).path("bold").asBoolean()).isTrue();
        assertThat(blocks.get(2).content().path("hasFile").asBoolean()).isTrue();
        assertThat(blocks.get(2).content().path("alt").asText()).isEqualTo("Figure");
        assertThat(blocks.get(4).type()).isEqualTo(BlockType.PARAGRAPH);
        List<AssignmentRubric> rubrics = f.rubricsOf(a.id());
        assertThat(rubrics).filteredOn(r -> r.partId().equals(questionOne.toString())).singleElement()
                .satisfies(r -> assertThat(r.totalPoints()).isEqualByComparingTo(new BigDecimal("15")));
        assertThat(rubrics).filteredOn(r -> r.partId().equals(RubricService.WHOLE)).singleElement()
                .satisfies(r -> assertThat(r.criteria().get(0).title()).isEqualTo("Presentation"));
    }

    @Test
    void unchangedTextKeepsAllOriginalRuns() {
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, chineseSkeleton(), false);
        var snapshot = AssignmentDraftRevision.snapshot(a, f.rubricsOf(a.id()));
        ObjectNode proposal = AssignmentDraftRevision.normalize(snapshot.document(), true);

        var built = AssignmentDraftRevision.build(proposal, snapshot.blockRefs(), a.skeleton(), a.title());

        assertThat(built.skeleton().blocks()).isEqualTo(a.skeleton().blocks());
    }

    @Test
    void operationsChangeOnlyMentionedBlocksAndKeepUntouchedRubrics() {
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, chineseSkeleton(), true);
        var snapshot = AssignmentDraftRevision.snapshot(a, f.rubricsOf(a.id()));
        JsonNode raw = read("""
                {"title":"","instructions":"Hướng dẫn","operations":[
                 {"op":"UPDATE","ref":"b2","type":"PARAGRAPH","text":"Liệt kê các ca sử dụng"},
                 {"op":"DELETE","ref":"b5","type":"PARAGRAPH","text":""},
                 {"op":"INSERT","ref":"b5","type":"HEADING","style":"H1","text":"Câu mới","rubric":[
                  {"title":"Đúng","items":[{"text":"Trả lời đúng","points":10}]}]},
                 {"op":"INSERT","ref":"b5","type":"PARAGRAPH","text":"Nội dung câu mới"},
                 {"op":"INSERT","ref":"b0","type":"HEADING","style":"H1","text":""},
                 {"op":"UPDATE","ref":"b42","type":"PARAGRAPH","text":"ref lạ bị bỏ qua"}]}
                """);

        ObjectNode merged = AssignmentDraftRevision.merge(raw, snapshot.document());
        ObjectNode proposal = AssignmentDraftRevision.normalize(merged, true);

        assertThat(proposal.path("title").asText()).isEqualTo("Bài 1");
        JsonNode blocks = proposal.path("blocks");
        assertThat(blocks).extracting(b -> b.path("ref").asText("-") + ":" + b.path("text").asText())
                .containsExactly("b1:系统用例分析", "b2:Liệt kê các ca sử dụng", "b3:图", "b4:数据模型设计",
                        "-:Câu mới", "-:Nội dung câu mới");
        assertThat(blocks.get(0).path("rubric")).isNotEmpty();
        assertThat(blocks.get(3).path("rubric")).isNotEmpty();
        assertThat(blocks.get(4).path("rubric").get(0).path("title").asText()).isEqualTo("Đúng");
        assertThat(AssignmentDraftRevision.merge(read("{\"blocks\":[]}"), snapshot.document())).isNull();
    }

    @Test
    void listItemsTravelAsLinesAndKeepTheirFormatting() {
        ObjectNode list = JsonNodeFactory.instance.objectNode().put("ordered", true);
        list.putArray("items").addObject().put("level", 1).putArray("runs").addObject().put("text", "墓位").put("italic", true);
        ((com.fasterxml.jackson.databind.node.ArrayNode) list.path("items")).addObject().putArray("runs")
                .addObject().put("text", "墓区");
        UUID listId = UUID.randomUUID();
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT,
                new DocumentModel(List.of(new DocumentBlock(listId, BlockOrigin.TEACHER, BlockType.LIST, list))), false);
        var snapshot = AssignmentDraftRevision.snapshot(a, f.rubricsOf(a.id()));
        assertThat(snapshot.document().path("blocks").get(0).path("text").asText()).isEqualTo("墓位\n墓区");

        ObjectNode proposal = AssignmentDraftRevision.normalize(read("""
                {"title":"x","instructions":"","blocks":[{"ref":"b1","type":"LIST","text":"Plots\\n\\nSections\\nRows","ordered":true}]}
                """), true);
        var built = AssignmentDraftRevision.build(proposal, snapshot.blockRefs(), a.skeleton(), a.title());

        JsonNode items = built.skeleton().blocks().get(0).content().path("items");
        assertThat(built.skeleton().blocks().get(0).id()).isEqualTo(listId);
        assertThat(items).hasSize(3);
        assertThat(items.get(0).path("runs").get(0).path("text").asText()).isEqualTo("Plots");
        assertThat(items.get(0).path("runs").get(0).path("italic").asBoolean()).isTrue();
        assertThat(items.get(0).path("level").asInt()).isEqualTo(1);
        assertThat(items.get(2).path("runs").get(0).path("text").asText()).isEqualTo("Rows");
    }

    @Test
    void reviseNeedsInstructionAndNeverCallsAiWithoutOne() {
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, chineseSkeleton(), true);

        assertThatThrownBy(() -> f.aiDrafts.request(f.teacher, a.id(), new AssignmentCommands.AiDraftRequest(
                " ", List.of(), List.of(), null, null, "r1", null, null, null, AssignmentDraftRevision.MODE)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("AI_REQUEST_INVALID");
        verify(f.aiTasks, never()).submit(any());
    }

    @Test
    void revisionIsNotAppliedOverEditsMadeWhileAiWasWorking() {
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, chineseSkeleton(), true);
        var snapshot = AssignmentDraftRevision.snapshot(a, f.rubricsOf(a.id()));
        ObjectNode proposal = AssignmentDraftRevision.normalize(snapshot.document(), true);
        AssignmentAiDraft stale = new AssignmentAiDraft(UUID.randomUUID(), a.id(), AssignmentAiDraft.Origin.AI_TASK,
                null, "r1", AssignmentAiDraft.Status.READY, proposal, JsonNodeFactory.instance.objectNode()
                .put("mode", AssignmentDraftRevision.MODE), a.version() - 1, null, f.teacher.accountId(),
                AssignmentFixtures.NOW, null);
        when(f.drafts.find(stale.id())).thenReturn(Optional.of(stale));

        assertThatThrownBy(() -> f.aiDrafts.apply(f.teacher, a.id(), stale.id(),
                new AssignmentCommands.ApplyDraft(a.version(), true, true)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("AI_DRAFT_STALE");
        assertThat(f.store.get(a.id()).skeleton()).isEqualTo(a.skeleton());
    }

    @Test
    void blankInstructionsFromAiKeepTheCurrentOnes() {
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, chineseSkeleton(), false);
        var snapshot = AssignmentDraftRevision.snapshot(a, f.rubricsOf(a.id()));

        ObjectNode merged = AssignmentDraftRevision.merge(
                read("{\"title\":\"\",\"instructions\":\" \",\"operations\":[]}"), snapshot.document());

        assertThat(merged.path("instructions").asText()).isEqualTo("Hướng dẫn");
        assertThat(merged).isEqualTo(snapshot.document());
    }

    @Test
    void emptyRevisionCannotBeApplied() {
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, chineseSkeleton(), true);
        ObjectNode proposal = AssignmentDraftRevision.normalize(read("{\"title\":\"x\",\"instructions\":\"\",\"blocks\":[]}"),
                true);
        AssignmentAiDraft ready = ready(a, proposal, JsonNodeFactory.instance.objectNode());

        assertThatThrownBy(() -> f.aiDrafts.apply(f.teacher, a.id(), ready.id(),
                new AssignmentCommands.ApplyDraft(a.version(), true, true)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("AI_DRAFT_INVALID");
        assertThat(f.store.get(a.id()).skeleton()).isEqualTo(a.skeleton());
    }

    private AssignmentAiDraft ready(Assignment a, JsonNode proposal, JsonNode blockRefs) {
        ObjectNode provenance = JsonNodeFactory.instance.objectNode().put("origin", "AI_TASK")
                .put("mode", AssignmentDraftRevision.MODE);
        provenance.set("blockRefs", blockRefs);
        AssignmentAiDraft ready = new AssignmentAiDraft(UUID.randomUUID(), a.id(), AssignmentAiDraft.Origin.AI_TASK,
                null, "r1", AssignmentAiDraft.Status.READY, proposal, provenance, a.version(), null,
                f.teacher.accountId(), AssignmentFixtures.NOW, null);
        when(f.drafts.find(ready.id())).thenReturn(Optional.of(ready));
        when(f.drafts.markApplied(eq(ready.id()), any())).thenReturn(1);
        return ready;
    }

    private JsonNode read(String text) {
        try {
            return f.json.readTree(text);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
