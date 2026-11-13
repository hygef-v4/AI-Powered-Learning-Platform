package edu.aiplatform.assignments.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.service.JsonSchemaChecker;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.model.ImportPage;
import edu.aiplatform.assignments.service.FigureCropper.Box;
import edu.aiplatform.assignments.service.FigureCropper.CroppedFigure;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ExamImportMapperTest {

    private final ObjectMapper json = new ObjectMapper();
    private final ImportPage page = new ImportPage(1, "image/jpeg", 1000, 1400, new byte[]{1}, null);
    private final List<Box> cropped = new ArrayList<>();

    /** Kết quả AI giả cho đề SWR302 trong ảnh mẫu của người dùng. */
    private static final String SWR302 = """
            {"title":"SWR302_SU25_PE1_407173","blocks":[
             {"type":"HEADING","level":1,"runs":[{"text":"SWR302_SU25_PE1_407173"}],"items":[],"rows":[]},
             {"type":"PARAGRAPH","runs":[{"text":"To enhance the convenience experience for visitors ..."}],
               "items":[],"rows":[]},
             {"type":"PARAGRAPH","runs":[{"text":"All the answers must be written in the provided template file",
               "bold":true,"color":"#E00000"},{"text":" [không đọc rõ] ","color":"#222222"}],"items":[],"rows":[]},
             {"type":"PARAGRAPH","runs":[{"text":"Question 1","bold":true},{"text":" (0.5 point): Complete all parts."}],
               "items":[],"rows":[]},
             {"type":"LIST","ordered":true,"runs":[],"rows":[],"items":[{"runs":[{"text":"a) Use case"}]},
               {"runs":[{"text":"  "}]},{"runs":[{"text":"b) Class"}],"level":9}]},
             {"type":"TABLE","headerRow":true,"runs":[],"items":[],
               "rows":[[{"runs":[{"text":"Ticket"}]},{"runs":[{"text":"Price"}]}],[{"runs":[{"text":"Standard"}]}]]},
             {"type":"FIGURE","runs":[],"items":[],"rows":[]},
             {"type":"FIGURE","runs":[],"items":[],"rows":[]},
             {"type":"PARAGRAPH","runs":[{"text":"   "}],"items":[],"rows":[]}],
             "figures":[{"page":1,"box":{"x":0.1,"y":0.6,"w":0.5,"h":0.2},"caption":"Sơ đồ cổng vào"},
               {"page":4,"box":{"x":0,"y":0,"w":1,"h":1},"caption":""}],
             "warnings":[{"kind":"HEADER_FOOTER_REMOVED","page":1,"message":"Bỏ 'Page 1 | 1'"},
               {"kind":"STRANGE","message":"x"}]}
            """;

    private ExamImportMapper.MappedImport map(String output, JsonNode pageWarnings) throws Exception {
        return ExamImportMapper.mapResult(json.readTree(output), Map.of(1, page), pageWarnings, (p, box) -> {
            cropped.add(box);
            return Optional.of(new CroppedFigure("ref-" + cropped.size(), 500));
        });
    }

    @Test
    void sampleOutputMatchesTheSchemaSentToTheModel() throws Exception {
        JsonSchemaChecker checker = new JsonSchemaChecker(json);
        assertThat(checker.validSchema(ExamImportPrompt.OUTPUT_SCHEMA)).isTrue();
        com.fasterxml.jackson.databind.node.ObjectNode valid = (com.fasterxml.jackson.databind.node.ObjectNode)
                json.readTree(SWR302);
        ((com.fasterxml.jackson.databind.node.ObjectNode) valid.at("/blocks/4/items/2")).put("level", 4);
        ((com.fasterxml.jackson.databind.node.ArrayNode) valid.path("warnings")).remove(1);
        assertThat(checker.check(ExamImportPrompt.OUTPUT_SCHEMA, valid)).isEmpty();
        assertThat(checker.check(ExamImportPrompt.OUTPUT_SCHEMA, json.readTree(SWR302))).as("level 9, kind lạ")
                .isPresent();
    }

    @Test
    void transcribedExamBecomesEditableBlocksInOrder() throws Exception {
        ExamImportMapper.MappedImport mapped = map(SWR302, null);

        List<DocumentBlock> blocks = mapped.document().blocks();
        assertThat(blocks).extracting(DocumentBlock::type).containsExactly(BlockType.PARAGRAPH, BlockType.PARAGRAPH,
                BlockType.PARAGRAPH, BlockType.PARAGRAPH, BlockType.LIST, BlockType.TABLE, BlockType.IMAGE,
                BlockType.PARAGRAPH);
        JsonNode title = blocks.get(0).content();
        assertThat(title.path("align").asText()).isEqualTo("CENTER");
        assertThat(title.at("/runs/0/bold").asBoolean()).as("tiêu đề in đậm, không thành mục câu hỏi").isTrue();
        JsonNode red = blocks.get(2).content().path("runs");
        assertThat(red.at("/0/color").asText()).isEqualTo("#E00000");
        assertThat(red.at("/0/bold").asBoolean()).isTrue();
        assertThat(red.at("/1/color").isMissingNode()).as("bỏ màu xám đậm").isTrue();
        assertThat(blocks.get(4).content().path("items")).hasSize(2);
        assertThat(blocks.get(4).content().at("/items/1/level").asInt()).isEqualTo(4);
        assertThat(blocks.get(5).content().at("/rows/1")).as("làm đều số cột").hasSize(2);
        assertThat(blocks.get(6).content().path("fileRef").asText()).isEqualTo("ref-1");
        assertThat(blocks.get(6).content().path("alt").asText()).isEqualTo("Sơ đồ cổng vào");
        assertThat(blocks.get(7).content().at("/runs/0/text").asText()).isEqualTo("[Hình: không có mô tả]");
        assertThat(cropped).singleElement().isEqualTo(new Box(0.1, 0.6, 0.5, 0.2));
    }

    @Test
    void proposalCarriesTitleCountsAndWarnings() throws Exception {
        JsonNode pageWarnings = json.readTree("[{\"kind\":\"LOW_RESOLUTION\",\"page\":1,\"message\":\"Mờ\"}]");

        JsonNode proposal = map(SWR302, pageWarnings).proposal();

        assertThat(proposal.path("kind").asText()).isEqualTo("IMPORT");
        assertThat(proposal.path("title").asText()).isEqualTo("SWR302_SU25_PE1_407173");
        assertThat(proposal.at("/counts/headings").asInt()).isEqualTo(1);
        assertThat(proposal.at("/counts/tables").asInt()).isEqualTo(1);
        assertThat(proposal.at("/counts/images").asInt()).isEqualTo(1);
        assertThat(proposal.at("/counts/illegible").asInt()).isEqualTo(1);
        assertThat(proposal.path("warnings")).extracting(w -> w.path("kind").asText())
                .containsExactly("LOW_RESOLUTION", "FIGURE_CROP_FAILED", "HEADER_FOOTER_REMOVED", "OTHER");
    }

    @Test
    void schemaRequiresTheFieldsThatCarryContent() throws Exception {
        // Gemini (2026-11-11) bỏ mọi mảng/đối tượng con không bắt buộc: khối chỉ có type, mất hết chữ và vị trí hình.
        JsonNode schema = json.readTree(ExamImportPrompt.OUTPUT_SCHEMA);
        assertThat(schema.path("required")).extracting(JsonNode::asText).contains("blocks", "figures");
        assertThat(schema.at("/properties/blocks/items/required")).extracting(JsonNode::asText)
                .containsExactlyInAnyOrder("type", "runs", "items", "rows");
        assertThat(schema.at("/properties/figures/items/required")).extracting(JsonNode::asText)
                .containsExactlyInAnyOrder("page", "box", "caption");
        JsonSchemaChecker checker = new JsonSchemaChecker(json);
        assertThat(checker.check(ExamImportPrompt.OUTPUT_SCHEMA, json.readTree(
                "{\"title\":\"\",\"blocks\":[{\"type\":\"PARAGRAPH\"}],\"figures\":[],\"warnings\":[]}")))
                .as("khối thiếu runs").isPresent();
    }

    @Test
    void failedCropBecomesAPlaceholderWithAWarning() throws Exception {
        String output = "{\"title\":\"\",\"blocks\":[{\"type\":\"FIGURE\",\"page\":1,"
                + "\"box\":{\"x\":0,\"y\":0,\"w\":1,\"h\":1},\"caption\":\"ERD\"}],\"warnings\":[]}";

        ExamImportMapper.MappedImport mapped = ExamImportMapper.mapResult(json.readTree(output), Map.of(1, page),
                null, (p, box) -> Optional.empty());

        assertThat(mapped.document().blocks()).singleElement().satisfies(block -> {
            assertThat(block.type()).isEqualTo(BlockType.PARAGRAPH);
            assertThat(block.content().at("/runs/0/text").asText()).isEqualTo("[Hình: ERD]");
        });
        assertThat(mapped.proposal().at("/warnings/0/kind").asText()).isEqualTo("FIGURE_CROP_FAILED");
        assertThat(mapped.proposal().at("/warnings/0/page").asInt()).isEqualTo(1);
    }

    @Test
    void clearColorsAreKeptAndDarkGreysAreDropped() {
        assertThat(ExamImportMapper.isClearColor("#E00000")).isTrue();
        assertThat(ExamImportMapper.isClearColor("#1F4E79")).isTrue();
        assertThat(ExamImportMapper.isClearColor("#000000")).isFalse();
        assertThat(ExamImportMapper.isClearColor("#333333")).isFalse();
        assertThat(ExamImportMapper.isClearColor("#808080")).isFalse();
        assertThat(ExamImportMapper.isClearColor("red")).isFalse();
    }

    @Test
    void longTitleIsCutToTheAssignmentTitleLimit() throws Exception {
        String output = "{\"title\":\"" + "T".repeat(400) + "\",\"blocks\":[{\"type\":\"PARAGRAPH\","
                + "\"runs\":[{\"text\":\"x\"}]}],\"warnings\":[]}";

        assertThat(map(output, null).proposal().path("title").asText()).hasSize(200);
    }
}
