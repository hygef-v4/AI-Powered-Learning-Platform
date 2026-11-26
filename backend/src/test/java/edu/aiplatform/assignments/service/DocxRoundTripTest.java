package edu.aiplatform.assignments.service;

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
import edu.aiplatform.assignments.contract.CriterionKind;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.DocxImportPreview;
import edu.aiplatform.assignments.contract.IDiagramLinker;
import edu.aiplatform.assignments.contract.RubricCriterion;
import edu.aiplatform.assignments.contract.RubricItem;
import edu.aiplatform.assignments.contract.ValidationIssue;
import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.files.contract.FileRef;
import edu.aiplatform.files.contract.IArtifact;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
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
        byte[] docx = exporter.export("Đề 1", "Làm theo nhóm mục", skeleton(diagram), linker);
        DocxImportPreview preview = importer.preview(teacher, docx, BlockOrigin.TEACHER, true,
                ref -> ref.equals(assignmentId + "/" + diagram.id()) ? Optional.of(DocumentModelServiceTest.DIAGRAM) : Optional.empty());
        List<DocumentBlock> blocks = preview.document().blocks();
        assertThat(blocks).extracting(DocumentBlock::type).contains(BlockType.HEADING, BlockType.PARAGRAPH, BlockType.LIST,
                BlockType.TABLE, BlockType.DIAGRAM);
        assertThat(blocks).filteredOn(b -> b.type() == BlockType.HEADING).extracting(b -> b.content().path("style").asText())
                .containsExactly("H1", "H2"); // Tên bài (Title, không đánh số) thành văn bản thường.
        assertThat(blocks).filteredOn(b -> b.type() == BlockType.DIAGRAM).singleElement().satisfies(b -> {
            assertThat(b.content().path("xml").asText()).isEqualTo(DocumentModelServiceTest.DIAGRAM);
            assertThat(b.content().path("alt").asText()).isEqualTo("Lớp");
        });
        assertThat(preview.warnings()).extracting(ValidationIssue::code).contains("DIAGRAM_RESTORED_BY_CODE");
        assertThat(blocks).allMatch(b -> b.origin() == BlockOrigin.TEACHER);
    }

    @Test
    void importKeepsTheRealDiagramPngButNotTheGeneratedPlaceholder() {
        DocumentBlock placeholder = AssignmentFixtures.diagram(DocumentModelServiceTest.DIAGRAM);
        DocumentBlock real = AssignmentFixtures.diagram(DocumentModelServiceTest.DIAGRAM);
        // Ảnh thật của sơ đồ (ở đây là ảnh trắng 480x270 không trùng ảnh hệ thống tự dựng vì không có chunk mxfile).
        byte[] saved = PngChunks.drawDiagramPlaceholder(null, false);
        ((ObjectNode) real.content()).put("pngPreview", "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(saved));

        List<DocumentBlock> fromPlaceholder = importer.preview(teacher, exporter.export("Đề", null, skeleton(placeholder), linker),
                BlockOrigin.TEACHER, true, ref -> Optional.empty()).document().blocks();
        List<DocumentBlock> fromReal = importer.preview(teacher, exporter.export("Đề", null, skeleton(real), linker),
                BlockOrigin.TEACHER, true, ref -> Optional.empty()).document().blocks();

        assertThat(fromPlaceholder).filteredOn(b -> b.type() == BlockType.DIAGRAM).singleElement()
                .satisfies(b -> assertThat(b.content().has("pngPreview")).isFalse());
        assertThat(fromReal).filteredOn(b -> b.type() == BlockType.DIAGRAM).singleElement()
                .satisfies(b -> assertThat(b.content().path("pngPreview").asText()).startsWith("data:image/png;base64,"));
    }

    @Test
    void withoutAccessToSourceTheDiagramComesFromThePngChunk() {
        DocumentBlock diagram = AssignmentFixtures.diagram(DocumentModelServiceTest.DIAGRAM);
        byte[] docx = exporter.export("Đề", null, skeleton(diagram), linker);
        DocxImportPreview preview = importer.preview(teacher, docx, BlockOrigin.TEACHER, true, ref -> Optional.empty());
        assertThat(preview.warnings()).extracting(ValidationIssue::code).contains("DIAGRAM_RESTORED_BY_PNG");
        assertThat(preview.document().blocks()).filteredOn(b -> b.type() == BlockType.DIAGRAM).singleElement()
                .satisfies(b -> assertThat(DiagramXml.countCells(b.content().path("xml").asText())).isEqualTo(1));
    }

    @Test
    void diagramBecomesImageWhenAssignmentHasDiagramsOff() {
        when(artifacts.store(eq(teacher), eq(ArtifactPurpose.DOCUMENT_IMAGE), any())).thenReturn(new FileRef("ref-1"));
        DocumentBlock diagram = AssignmentFixtures.diagram(DocumentModelServiceTest.DIAGRAM);
        byte[] docx = exporter.export("Đề", null, skeleton(diagram), linker);
        DocxImportPreview preview = importer.preview(teacher, docx, BlockOrigin.STUDENT, false, ref -> Optional.empty());
        assertThat(preview.document().blocks()).noneMatch(b -> b.type() == BlockType.DIAGRAM);
        assertThat(preview.document().blocks()).filteredOn(b -> b.type() == BlockType.IMAGE).singleElement()
                .satisfies(b -> assertThat(b.content().path("fileRef").asText()).isEqualTo("ref-1"));
        assertThat(preview.warnings()).extracting(ValidationIssue::code).contains("DIAGRAM_KEPT_AS_IMAGE");
    }

    @Test
    void rubricIsExportedAsItsOwnDocumentWithoutTheSkeleton() throws Exception {
        RubricCriterion criterion = new RubricCriterion(UUID.randomUUID(), "Đủ actor", "", CriterionKind.TEXT,
                List.of(new RubricItem(UUID.randomUUID(), "Đủ actor", new BigDecimal("1.5"))));
        byte[] docx = exporter.exportRubric("Đề 1", List.of(new DocxExporter.RubricSection("Use case", List.of(criterion),
                new BigDecimal("1.5"))));
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(doc.getParagraphs()).extracting(XWPFParagraph::getText)
                    .contains("Đề 1", "Rubric", "Use case (1.5 điểm)").doesNotContain("Mô tả hệ thống");
            XWPFTable table = doc.getTables().get(0);
            assertThat(table.getRow(0).getCell(0).getText()).isEqualTo("Mục chấm");
            assertThat(table.getRow(1).getCell(0).getText()).isEqualTo("Đủ actor");
            assertThat(table.getRow(1).getCell(1).getText()).isEqualTo("1.5");
        }
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(exporter.exportRubric("Đề 1", List.of())))) {
            assertThat(doc.getTables()).isEmpty();
            assertThat(doc.getParagraphs()).extracting(XWPFParagraph::getText).contains("Chưa có mục chấm.");
        }
    }

    @Test
    void rubricExportSurvivesMissingTitlesAndPointsAndShowsTheTotal() throws Exception {
        RubricCriterion untitled = new RubricCriterion(UUID.randomUUID(), null, null, CriterionKind.TEXT,
                List.of(new RubricItem(UUID.randomUUID(), null, null),
                        new RubricItem(UUID.randomUUID(), "Đúng quan hệ", new BigDecimal("2.00"))));
        byte[] docx = exporter.exportRubric(null, List.of(
                new DocxExporter.RubricSection(null, List.of(untitled), null),
                new DocxExporter.RubricSection("I.1. Sơ đồ lớp", List.of(untitled), new BigDecimal("2.00"))));
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(doc.getParagraphs()).extracting(XWPFParagraph::getText)
                    .contains("Tổng điểm: 2", "I.1. Sơ đồ lớp (2 điểm)");
            assertThat(doc.getTables()).hasSize(2);
            assertThat(doc.getTables().get(1).getRow(2).getCell(1).getText()).isEqualTo("2");
        }
    }

    @Test
    void emptyTableFillsThePageAndAnUndrawnDiagramIsAnEmptyFrame() throws Exception {
        ObjectNode table = JsonNodeFactory.instance.objectNode();
        var rows = table.putArray("rows");
        for (int i = 0; i < 3; i++) {
            var row = rows.addArray();
            for (int j = 0; j < 4; j++) {
                row.addObject().putArray("runs");
            }
        }
        DocumentBlock undrawn = AssignmentFixtures.diagram("");
        byte[] docx = exporter.export("Đề", null, new DocumentModel(List.of(AssignmentFixtures.heading("H1", "Câu 1"),
                new DocumentBlock(UUID.randomUUID(), BlockOrigin.TEACHER, BlockType.TABLE, table), undrawn)), linker);
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docx))) {
            XWPFTable exported = doc.getTables().get(0);
            assertThat(exported.getRows()).hasSize(3);
            assertThat(exported.getWidthType()).isEqualTo(org.apache.poi.xwpf.usermodel.TableWidthType.PCT);
            assertThat(exported.getRow(0).getTableCells()).hasSize(4)
                    .allSatisfy(cell -> assertThat(cell.getWidthType())
                            .isEqualTo(org.apache.poi.xwpf.usermodel.TableWidthType.PCT));
            assertThat(doc.getParagraphs()).extracting(XWPFParagraph::getText)
                    .anySatisfy(text -> assertThat(text).contains("Chưa có sơ đồ"))
                    .noneSatisfy(text -> assertThat(text).contains("bấm vào ảnh"));
        }
        assertThat(PngChunks.drawDiagramPlaceholder(null, false)).isNotEqualTo(PngChunks.drawDiagramPlaceholder(null, true));
    }

    @Test
    void exportUsesTheSavedDiagramPngAndAWhiteImageForAnEmptyDiagram() throws Exception {
        byte[] saved = PngChunks.drawDiagramPlaceholder(null, false);
        DocumentBlock drawn = AssignmentFixtures.diagram(DocumentModelServiceTest.DIAGRAM);
        ((ObjectNode) drawn.content()).put("pngPreview",
                "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(saved));
        DocumentBlock empty = AssignmentFixtures.diagram(
                "<mxfile><diagram id=\"p\"><mxGraphModel><root><mxCell id=\"0\"/><mxCell id=\"1\" parent=\"0\"/>"
                        + "</root></mxGraphModel></diagram></mxfile>");
        // Draw.io xuất SVG cả khi sơ đồ trống: không vì thế mà dùng hình đại diện.
        ((ObjectNode) empty.content()).put("svgPreview", "<svg xmlns=\"http://www.w3.org/2000/svg\"/>");

        byte[] docx = exporter.export("Đề", null, new DocumentModel(List.of(drawn, empty)), linker);

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docx))) {
            List<byte[]> pictures = doc.getAllPictures().stream()
                    .map(org.apache.poi.xwpf.usermodel.XWPFPictureData::getData).toList();
            assertThat(pictures).hasSize(2);
            assertThat(PngChunks.readMxfile(pictures.get(0))).contains(DocumentModelServiceTest.DIAGRAM);
            assertThat(pictures.get(0)).hasSizeGreaterThan(saved.length);
            assertThat(java.util.Arrays.equals(pictures.get(0), PngChunks.drawDiagramPlaceholder(DocumentModelServiceTest.DIAGRAM)))
                    .isFalse();
            assertThat(pictures.get(1)).isNotEqualTo(PngChunks.drawDiagramPlaceholder(null, false));
            assertThat(PngChunks.readMxfile(pictures.get(1))).isPresent();
        }
        assertThat(PngChunks.readPngDataUri("data:image/png;base64,bm90IGEgcG5n")).isEmpty();
        assertThat(PngChunks.readPngDataUri("data:image/svg+xml;base64,AAAA")).isEmpty();
    }

    @Test
    void exportKeepsTheFormattingShownInTheEditor() throws Exception {
        ObjectNode title = JsonNodeFactory.instance.objectNode().put("align", "CENTER");
        title.putArray("runs").addObject().put("text", "Đề giữa kỳ").put("italic", true);
        ObjectNode heading = JsonNodeFactory.instance.objectNode().put("style", "H1").put("text", "Phân tích")
                .put("align", "RIGHT");
        heading.putArray("runs").addObject().put("text", "Phân tích").put("underline", true).put("color", "#C00000");
        ObjectNode paragraph = JsonNodeFactory.instance.objectNode();
        // Ký tự điều khiển dán từ nơi khác vào: phải bỏ, nếu còn thì Word báo tệp hỏng.
        paragraph.putArray("runs").addObject().put("text", "Tô nền").put("highlight", "#FFF2CC")
                .put("color", "not-a-color");
        ObjectNode table = JsonNodeFactory.instance.objectNode();
        table.putArray("widths").add(70).add(30);
        var row = table.putArray("rows").addArray();
        row.addObject().put("align", "CENTER").putArray("runs").addObject().put("text", "A");
        row.addObject().putArray("runs").addObject().put("text", "B");
        DocumentModel model = new DocumentModel(List.of(
                new DocumentBlock(UUID.randomUUID(), BlockOrigin.TEACHER, BlockType.HEADING, heading),
                new DocumentBlock(UUID.randomUUID(), BlockOrigin.TEACHER, BlockType.PARAGRAPH, paragraph),
                new DocumentBlock(UUID.randomUUID(), BlockOrigin.TEACHER, BlockType.TABLE, table)), title);

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(exporter.export("Tên thuần", null, model, linker)))) {
            XWPFParagraph first = doc.getParagraphs().get(0);
            assertThat(first.getText()).isEqualTo("Đề giữa kỳ");
            assertThat(first.getAlignment()).isEqualTo(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
            assertThat(first.getRuns().get(0).isItalic()).isTrue();
            XWPFParagraph exportedHeading = doc.getParagraphs().get(1);
            assertThat(exportedHeading.getText()).isEqualTo("I. Phân tích");
            assertThat(exportedHeading.getAlignment()).isEqualTo(org.apache.poi.xwpf.usermodel.ParagraphAlignment.RIGHT);
            assertThat(exportedHeading.getRuns().get(1).getColor()).isEqualTo("C00000");
            XWPFParagraph body = doc.getParagraphs().get(2);
            assertThat(body.getText()).isEqualTo("Tô nền");
            assertThat(body.getRuns().get(0).getCTR().xmlText()).contains("w:fill=\"FFF2CC\"");
            XWPFTable exported = doc.getTables().get(0);
            assertThat(exported.getRow(0).getCell(0).getWidth()).isGreaterThan(exported.getRow(0).getCell(1).getWidth());
            assertThat(exported.getRow(0).getCell(0).getParagraphs().get(0).getAlignment())
                    .isEqualTo(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
        }
    }

    @Test
    void importKeepsBoldItalicAndHighlightOfExportedAndWordFormattedText() {
        ObjectNode paragraph = JsonNodeFactory.instance.objectNode();
        var runs = paragraph.putArray("runs");
        runs.addObject().put("text", "Đậm tô nền").put("bold", true).put("highlight", "#FFF2CC");
        runs.addObject().put("text", " thường");
        DocumentModel model = new DocumentModel(List.of(AssignmentFixtures.heading("H1", "Phân tích"),
                new DocumentBlock(UUID.randomUUID(), BlockOrigin.STUDENT, BlockType.PARAGRAPH, paragraph)));
        List<DocumentBlock> exported = importer.preview(teacher, exporter.export("Đề 1", "", model, linker), BlockOrigin.STUDENT,
                true, ref -> Optional.empty()).document().blocks();
        var back = exported.stream().filter(b -> b.type() == BlockType.PARAGRAPH
                && b.content().path("runs").get(0).path("text").asText().equals("Đậm tô nền")).findFirst().orElseThrow();
        assertThat(back.content().path("runs").get(0).path("bold").asBoolean()).isTrue();
        assertThat(back.content().path("runs").get(0).path("highlight").asText()).isEqualTo("#FFF2CC");
        assertThat(back.content().path("runs").get(1).has("highlight")).isFalse();

        // Tệp viết trong Word: bút highlight và kiểu ký tự Strong, Emphasis.
        byte[] word = docx(doc -> {
            XWPFParagraph p = doc.createParagraph();
            var marked = p.createRun();
            marked.setText("Vàng");
            marked.getCTR().addNewRPr().addNewHighlight().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STHighlightColor.YELLOW);
            var strong = p.createRun();
            strong.setText(" mạnh");
            strong.setStyle("Strong");
            var emphasis = p.createRun();
            emphasis.setText(" nghiêng");
            emphasis.setStyle("Emphasis");
        });
        var fromWord = importer.preview(teacher, word, BlockOrigin.STUDENT, true, ref -> Optional.empty()).document().blocks()
                .get(0).content().path("runs");
        assertThat(fromWord.get(0).path("highlight").asText()).isEqualTo("#FFFF00");
        assertThat(fromWord.get(1).path("bold").asBoolean()).isTrue();
        assertThat(fromWord.get(2).path("italic").asBoolean()).isTrue();
    }

    @Test
    void exportedPngCarriesMxfileChunk() {
        byte[] png = PngChunks.drawDiagramPlaceholder(DocumentModelServiceTest.DIAGRAM);
        assertThat(PngChunks.isPng(png)).isTrue();
        assertThat(PngChunks.readMxfile(png)).contains(DocumentModelServiceTest.DIAGRAM);
        assertThat(PngChunks.readSize(png)).hasValueSatisfying(size -> assertThat(size).containsExactly(480, 270));
    }

    private static byte[] docx(java.util.function.Consumer<XWPFDocument> build) {
        try (XWPFDocument doc = new XWPFDocument(); java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            build.accept(doc);
            doc.write(out);
            return out.toByteArray();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void para(XWPFDocument doc, String style, String text, boolean bold) {
        XWPFParagraph p = doc.createParagraph();
        if (style != null) {
            p.setStyle(style);
        }
        var run = p.createRun();
        run.setText(text);
        run.setBold(bold);
    }

    @Test
    void numberedHeadingsBecomeTheSectionTreeWithoutTheirOriginalNumbers() {
        byte[] file = docx(doc -> {
            para(doc, "Heading1", "ĐỀ THI CUỐI KỲ", false);
            para(doc, "Heading1", "Bối cảnh", false);
            para(doc, "Heading1", "I. Phân tích", false);
            para(doc, "Heading2", "1. Use case", false);
            para(doc, "Heading2", "2) Class diagram", false);
            para(doc, null, "II. THIẾT KẾ", true);
            para(doc, null, "1.1 Kiến trúc", true);
            para(doc, null, "1. Đây là một ý thường, không in đậm", false);
        });
        List<DocumentBlock> blocks = importer.preview(teacher, file, BlockOrigin.TEACHER, true, ref -> Optional.empty())
                .document().blocks();
        assertThat(blocks).extracting(b -> b.type() + ":" + (b.type() == BlockType.HEADING
                        ? b.content().path("style").asText() + " " + b.content().path("text").asText()
                        : b.content().path("runs").get(0).path("text").asText()))
                .containsExactly("PARAGRAPH:ĐỀ THI CUỐI KỲ", "PARAGRAPH:Bối cảnh", "HEADING:H1 Phân tích",
                        "HEADING:H2 Use case", "HEADING:H2 Class diagram", "HEADING:H1 THIẾT KẾ", "HEADING:H3 Kiến trúc",
                        "PARAGRAPH:1. Đây là một ý thường, không in đậm");
        // Tiêu đề không đánh số thành văn bản thường in đậm.
        assertThat(blocks.get(0).content().path("runs").get(0).path("bold").asBoolean()).isTrue();
    }

    @Test
    void exportedSectionNumbersRoundTripIntoTheSameTree() {
        DocumentModel model = new DocumentModel(List.of(AssignmentFixtures.heading("H1", "Phân tích"),
                AssignmentFixtures.heading("H2", "Use case"), AssignmentFixtures.heading("H3", "Actor"),
                AssignmentFixtures.heading("H4", "Khách"), AssignmentFixtures.heading("H2", "Class"),
                AssignmentFixtures.heading("H1", "Thiết kế")));
        byte[] file = exporter.export("Đề 1", "", model, linker);
        List<DocumentBlock> blocks = importer.preview(teacher, file, BlockOrigin.TEACHER, true, ref -> Optional.empty())
                .document().blocks().stream().filter(b -> b.type() == BlockType.HEADING).toList();
        assertThat(blocks).extracting(b -> b.content().path("style").asText() + " " + b.content().path("text").asText())
                .containsExactly("H1 Phân tích", "H2 Use case", "H3 Actor", "H4 Khách", "H2 Class", "H1 Thiết kế");
    }

    @Test
    void notADocxIsRejected() {
        assertThatThrownBy(() -> importer.preview(teacher, "không phải zip".getBytes(), BlockOrigin.TEACHER, true, ref -> Optional.empty()))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("DOCX_INVALID");
    }
}
