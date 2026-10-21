package edu.aiplatform.assignments.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.assignments.contract.BlockOrigin;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.DocxImportPreview;
import edu.aiplatform.assignments.contract.IDiagramLinker;
import edu.aiplatform.assignments.contract.ValidationIssue;
import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.files.contract.FileRef;
import edu.aiplatform.files.contract.IArtifact;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** F7; BR-U07-60…62; P4: xuất có alt text, hyperlink, chunk mxfile; nhập khôi phục sơ đồ theo thứ tự ưu tiên. */
class DocxRoundTripTest {

    private final IArtifact artifacts = mock(IArtifact.class);
    private final DocxExporter exporter = new DocxExporter(artifacts);
    private final DocxImporter importer = new DocxImporter(artifacts);
    private final Actor teacher = new Actor(UUID.randomUUID(), Role.TEACHER);
    private final UUID assignmentId = UUID.randomUUID();

    private final IDiagramLinker linker = new IDiagramLinker() {
        @Override
        public String linkFor(UUID blockId) {
            return assignmentId + "/" + blockId;
        }

        @Override
        public String viewUrlFor(UUID blockId) {
            return "https://lms.example/assignments/" + assignmentId + "/diagrams/" + blockId;
        }
    };

    private DocumentModel skeleton(DocumentBlock diagram) {
        ObjectNode list = JsonNodeFactory.instance.objectNode().put("ordered", true);
        list.putArray("items").addObject().put("level", 0).putArray("runs").addObject().put("text", "Bước một");
        ObjectNode table = JsonNodeFactory.instance.objectNode().put("headerRow", true);
        var row = table.putArray("rows").addArray();
        row.addObject().putArray("runs").addObject().put("text", "Cột A");
        row.addObject().putArray("runs").addObject().put("text", "Cột B");
        return new DocumentModel(List.of(AssignmentFixtures.heading("H1", "Phân tích"),
                AssignmentFixtures.heading("H2", "Use case"), AssignmentFixtures.paragraph("Mô tả hệ thống"),
                new DocumentBlock(UUID.randomUUID(), BlockOrigin.TEACHER, BlockType.LIST, list),
                new DocumentBlock(UUID.randomUUID(), BlockOrigin.TEACHER, BlockType.TABLE, table), diagram));
    }

    @Test
    void exportedDiagramComesBackByCodeWithStylesPreserved() {
        DocumentBlock diagram = AssignmentFixtures.diagram(DocumentModelServiceTest.DIAGRAM);
        byte[] docx = exporter.export("Đề 1", "Làm theo nhóm mục", skeleton(diagram), linker, List.of());
        DocxImportPreview preview = importer.preview(teacher, docx, BlockOrigin.TEACHER, true,
                ref -> ref.equals(assignmentId + "/" + diagram.id()) ? Optional.of(DocumentModelServiceTest.DIAGRAM) : Optional.empty());
        List<DocumentBlock> blocks = preview.document().blocks();
        assertThat(blocks).extracting(DocumentBlock::type).contains(BlockType.HEADING, BlockType.PARAGRAPH, BlockType.LIST,
                BlockType.TABLE, BlockType.DIAGRAM);
        assertThat(blocks).filteredOn(b -> b.type() == BlockType.HEADING).extracting(b -> b.content().path("style").asText())
                .containsExactly("TITLE", "H1", "H2");
        assertThat(blocks).filteredOn(b -> b.type() == BlockType.DIAGRAM).singleElement().satisfies(b -> {
            assertThat(b.content().path("xml").asText()).isEqualTo(DocumentModelServiceTest.DIAGRAM);
            assertThat(b.content().path("alt").asText()).isEqualTo("Lớp");
        });
        assertThat(preview.warnings()).extracting(ValidationIssue::code).contains("DIAGRAM_RESTORED_BY_CODE");
        assertThat(blocks).allMatch(b -> b.origin() == BlockOrigin.TEACHER);
    }

    @Test
    void withoutAccessToSourceTheDiagramComesFromThePngChunk() {
        DocumentBlock diagram = AssignmentFixtures.diagram(DocumentModelServiceTest.DIAGRAM);
        byte[] docx = exporter.export("Đề", null, skeleton(diagram), linker, List.of());
        DocxImportPreview preview = importer.preview(teacher, docx, BlockOrigin.TEACHER, true, ref -> Optional.empty());
        assertThat(preview.warnings()).extracting(ValidationIssue::code).contains("DIAGRAM_RESTORED_BY_PNG");
        assertThat(preview.document().blocks()).filteredOn(b -> b.type() == BlockType.DIAGRAM).singleElement()
                .satisfies(b -> assertThat(DiagramXml.cellCount(b.content().path("xml").asText())).isEqualTo(1));
    }

    @Test
    void diagramBecomesImageWhenAssignmentHasDiagramsOff() {
        when(artifacts.store(eq(teacher), eq(ArtifactPurpose.DOCUMENT_IMAGE), any())).thenReturn(new FileRef("ref-1"));
        DocumentBlock diagram = AssignmentFixtures.diagram(DocumentModelServiceTest.DIAGRAM);
        byte[] docx = exporter.export("Đề", null, skeleton(diagram), linker, List.of());
        DocxImportPreview preview = importer.preview(teacher, docx, BlockOrigin.STUDENT, false, ref -> Optional.empty());
        assertThat(preview.document().blocks()).noneMatch(b -> b.type() == BlockType.DIAGRAM);
        assertThat(preview.document().blocks()).filteredOn(b -> b.type() == BlockType.IMAGE).singleElement()
                .satisfies(b -> assertThat(b.content().path("fileRef").asText()).isEqualTo("ref-1"));
        assertThat(preview.warnings()).extracting(ValidationIssue::code).contains("DIAGRAM_KEPT_AS_IMAGE");
    }

    @Test
    void exportedPngCarriesMxfileChunk() {
        byte[] png = PngChunks.diagramPlaceholder(DocumentModelServiceTest.DIAGRAM);
        assertThat(PngChunks.isPng(png)).isTrue();
        assertThat(PngChunks.readMxfile(png)).contains(DocumentModelServiceTest.DIAGRAM);
        assertThat(PngChunks.size(png)).hasValueSatisfying(size -> assertThat(size).containsExactly(480, 270));
    }

    @Test
    void notADocxIsRejected() {
        assertThatThrownBy(() -> importer.preview(teacher, "không phải zip".getBytes(), BlockOrigin.TEACHER, true, ref -> Optional.empty()))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("DOCX_INVALID");
    }
}
