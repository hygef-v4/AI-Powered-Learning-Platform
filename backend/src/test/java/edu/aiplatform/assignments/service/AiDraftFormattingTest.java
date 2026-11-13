package edu.aiplatform.assignments.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.assignments.contract.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiDraftFormattingTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void generationAppliesRequestedFormattingWithoutChangingTextOrRubrics() throws Exception {
        var proposal = AiDraftProposal.normalize(json.readTree("""
                {"businessContext":"Bệnh viện","businessContextFormat":{"align":"CENTER","bold":true},
                 "sections":[{"level":1,"title":"Thiết kế","titleFormat":{"italic":true},"guidance":"Vẽ sơ đồ",
                  "guidanceFormat":{"align":"JUSTIFY","runs":[{"text":"Vẽ ","bold":true},{"text":"sơ đồ","color":"#123456"}]},
                  "rubric":[{"title":"Đúng","items":[{"text":"Đủ lớp","points":2}]}]}]}
                """), true);
        var built = AiDraftProposal.build(proposal);
        assertThat(built.skeleton().blocks()).hasSize(3);
        JsonNode context = built.skeleton().blocks().get(0).content();
        assertThat(context.path("align").asText()).isEqualTo("CENTER");
        assertThat(context.path("runs").get(0).path("bold").asBoolean()).isTrue();
        JsonNode body = built.skeleton().blocks().get(2).content();
        assertThat(body.path("align").asText()).isEqualTo("JUSTIFY");
        assertThat(body.path("runs").get(1).path("color").asText()).isEqualTo("#123456");
        assertThat(built.rubricByHeading()).hasSize(1);
        assertThat(new DocumentModelService(json).validateSkeleton(built.skeleton()).ok()).isTrue();
    }

    @Test
    void revisionCanRemoveBoldAndAlignWhileUntouchedRunsSurvive() throws Exception {
        ObjectNode content = json.createObjectNode().put("align", "LEFT");
        content.putArray("runs").addObject().put("text", "Hello").put("bold", true).put("italic", true);
        UUID id = UUID.randomUUID();
        var current = new DocumentModel(List.of(new DocumentBlock(id, BlockOrigin.TEACHER, BlockType.PARAGRAPH, content)));
        var refs = json.createObjectNode().put("b1", id.toString());
        var proposal = AssignmentDraftRevision.normalize(json.readTree("""
                {"blocks":[{"ref":"b1","type":"PARAGRAPH","text":"Hello","format":{"align":"RIGHT","bold":false}}]}
                """), true);
        var built = AssignmentDraftRevision.build(proposal, refs, current, "Test");
        JsonNode next = built.skeleton().blocks().get(0).content();
        assertThat(next.path("align").asText()).isEqualTo("RIGHT");
        assertThat(next.path("runs").get(0).path("bold").asBoolean()).isFalse();
        assertThat(next.path("runs").get(0).path("italic").asBoolean()).isTrue();
        assertThat(built.skeleton().blocks().get(0).id()).isEqualTo(id);
    }

    @Test
    void rejectsUnsafeStylesAndMismatchedRunText() throws Exception {
        var format = AiDraftFormatting.normalize(json.readTree("""
                {"align":"absolute","color":"url(secret)","html":"<script>","bold":"true",
                 "runs":[{"text":"Different","bold":true}]}
                """), "Original");
        assertThat(format.isEmpty()).isTrue();
        assertThat(json.readTree(AiDraftProposal.OUTPUT_SCHEMA).at("/properties/sections/items/properties/guidanceFormat/properties/align").isObject()).isTrue();
        assertThat(json.readTree(AssignmentDraftRevision.OUTPUT_SCHEMA).at("/properties/operations/items/properties/format").isObject()).isTrue();
    }
}
