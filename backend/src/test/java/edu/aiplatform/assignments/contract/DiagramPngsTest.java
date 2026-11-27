package edu.aiplatform.assignments.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Ảnh sơ đồ trình duyệt vẽ lúc tải DOCX (yêu cầu 2026-11-27). */
class DiagramPngsTest {

    private static DocumentBlock diagram(String png) {
        ObjectNode content = JsonNodeFactory.instance.objectNode().put("diagramType", "OTHER").put("xml", "<mxfile/>");
        if (png != null) {
            content.put("pngPreview", png);
        }
        return new DocumentBlock(UUID.randomUUID(), BlockOrigin.TEACHER, BlockType.DIAGRAM, content);
    }

    @Test
    void fillsOnlyDiagramsWithoutASavedPngAndLeavesTheOriginalUntouched() {
        DocumentBlock missing = diagram(null);
        DocumentBlock saved = diagram("data:image/png;base64,SAVED");
        DocumentModel model = new DocumentModel(List.of(missing, saved));

        DocumentModel filled = DiagramPngs.fillMissing(model, Map.of(
                missing.id().toString(), "data:image/png;base64,NEW",
                saved.id().toString(), "data:image/png;base64,OTHER"));

        assertThat(filled.blocks().get(0).content().path("pngPreview").asText()).isEqualTo("data:image/png;base64,NEW");
        assertThat(filled.blocks().get(1).content().path("pngPreview").asText()).isEqualTo("data:image/png;base64,SAVED");
        assertThat(missing.content().has("pngPreview")).as("không sửa tài liệu đã lưu").isFalse();
    }

    @Test
    void ignoresNonPngValuesAndEmptyBodies() {
        DocumentBlock missing = diagram(null);
        DocumentModel model = new DocumentModel(List.of(missing));

        assertThat(DiagramPngs.fillMissing(model, Map.of(missing.id().toString(), "data:image/svg+xml;base64,AAAA"))
                .blocks().get(0).content().has("pngPreview")).isFalse();
        assertThat(DiagramPngs.fillMissing(model, Map.of())).isSameAs(model);
        assertThat(DiagramPngs.Body.read(null)).isEmpty();
        assertThat(DiagramPngs.Body.read(new DiagramPngs.Body(null))).isEmpty();
    }
}
