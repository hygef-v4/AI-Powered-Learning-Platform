package edu.aiplatform.assignments.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.assignments.contract.BlockOrigin;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.DiagramType;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.ValidationIssue;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.Deflater;
import org.junit.jupiter.api.Test;

/** BR-U07-12, 15; NFR-U07-30: kiểm khung, bài làm không đụng khung, đủ sơ đồ khi nộp, XML an toàn, SVG sạch. */
class DocumentModelServiceTest {

    static final String DIAGRAM = "<mxfile><diagram id=\"p\" name=\"Trang 1\"><mxGraphModel><root><mxCell id=\"0\"/>"
            + "<mxCell id=\"1\" parent=\"0\"/><mxCell id=\"2\" value=\"User\" style=\"shape=umlActor\" vertex=\"1\" parent=\"1\"/>"
            + "</root></mxGraphModel></diagram></mxfile>";
    static final String EMPTY = "<mxfile><diagram id=\"p\"><mxGraphModel><root><mxCell id=\"0\"/></root></mxGraphModel>"
            + "</diagram></mxfile>";

    private final DocumentModelService service = new DocumentModelService(new ObjectMapper());

    static DocumentBlock paragraph(BlockOrigin origin, String text) {
        ObjectNode content = JsonNodeFactory.instance.objectNode();
        content.putArray("runs").addObject().put("text", text);
        return new DocumentBlock(UUID.randomUUID(), origin, BlockType.PARAGRAPH, content);
    }

    static DocumentBlock diagram(BlockOrigin origin, String xml, String svg) {
        ObjectNode content = JsonNodeFactory.instance.objectNode().put("diagramType", "CLASS").put("xml", xml);
        if (svg != null) {
            content.put("svgPreview", svg);
        }
        return new DocumentBlock(UUID.randomUUID(), origin, BlockType.DIAGRAM, content);
    }

    static DocumentBlock heading(BlockOrigin origin) {
        return heading(origin, "H1", "Mục");
    }

    static DocumentBlock heading(BlockOrigin origin, String style, String text) {
        return new DocumentBlock(UUID.randomUUID(), origin, BlockType.HEADING,
                JsonNodeFactory.instance.objectNode().put("style", style).put("text", text));
    }

    private static DocumentBlock redrawn(DocumentBlock diagram, String xml) {
        ObjectNode content = ((ObjectNode) diagram.content()).deepCopy().put("xml", xml);
        return new DocumentBlock(diagram.id(), diagram.origin(), diagram.type(), content);
    }

    private static List<String> codes(List<ValidationIssue> issues) {
        return issues.stream().map(ValidationIssue::code).toList();
    }

    @Test
    void skeletonRejectsStudentBlocksAndUnsafeXml() {
        String doctype = "<!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]><mxfile>&e;</mxfile>";
        DocumentModel skeleton = new DocumentModel(List.of(paragraph(BlockOrigin.STUDENT, "a"),
                diagram(BlockOrigin.TEACHER, doctype, null), diagram(BlockOrigin.TEACHER, "<svg/>", null)));
        assertThat(codes(service.validateSkeleton(skeleton).errors()))
                .contains("STUDENT_BLOCK_IN_SKELETON", "DIAGRAM_XML_INVALID");
    }

    @Test
    void studentCannotChangeMoveRemoveOrAddTeacherBlocks() {
        DocumentBlock first = paragraph(BlockOrigin.TEACHER, "Đề 1");
        DocumentBlock second = paragraph(BlockOrigin.TEACHER, "Đề 2");
        DocumentModel skeleton = new DocumentModel(List.of(first, second));
        DocumentBlock changed = new DocumentBlock(first.id(), BlockOrigin.TEACHER, BlockType.PARAGRAPH,
                paragraph(BlockOrigin.TEACHER, "sửa").content());
        assertThat(codes(service.validateForSave(skeleton, new DocumentModel(List.of(changed, second))).errors()))
                .contains("TEACHER_BLOCK_CHANGED");
        assertThat(codes(service.validateForSave(skeleton, new DocumentModel(List.of(second, first))).errors()))
                .contains("TEACHER_BLOCK_MOVED");
        assertThat(codes(service.validateForSave(skeleton, new DocumentModel(List.of(first))).errors()))
                .contains("TEACHER_BLOCK_MISSING");
        assertThat(codes(service.validateForSave(skeleton,
                new DocumentModel(List.of(first, paragraph(BlockOrigin.TEACHER, "lạ"), second))).errors()))
                .contains("TEACHER_BLOCK_ADDED");
        assertThat(codes(service.validateForSave(skeleton,
                new DocumentModel(List.of(first, heading(BlockOrigin.STUDENT), second))).errors()))
                .contains("STUDENT_HEADING_NOT_ALLOWED");
        assertThat(service.validateForSave(skeleton,
                new DocumentModel(List.of(paragraph(BlockOrigin.STUDENT, "bài"), first, second))).ok()).isTrue();
    }

    @Test
    void studentWritesOnlyInsideLeafParts() {
        DocumentBlock intro = paragraph(BlockOrigin.TEACHER, "Phần chung");
        DocumentBlock analysis = heading(BlockOrigin.TEACHER, "H1", "Phân tích");
        DocumentBlock analysisIntro = paragraph(BlockOrigin.TEACHER, "Dẫn nhập của mục cha");
        DocumentBlock useCase = heading(BlockOrigin.TEACHER, "H2", "Use case");
        DocumentBlock design = heading(BlockOrigin.TEACHER, "H1", "Thiết kế");
        DocumentModel skeleton = new DocumentModel(List.of(intro, analysis, analysisIntro, useCase, design));
        DocumentBlock common = paragraph(BlockOrigin.STUDENT, "ở phần chung");
        DocumentBlock parent = paragraph(BlockOrigin.STUDENT, "dưới mục cha");
        DocumentBlock leaf = paragraph(BlockOrigin.STUDENT, "trong mục lá");
        DocumentBlock last = paragraph(BlockOrigin.STUDENT, "cuối bài");
        List<ValidationIssue> errors = service.validateForSave(skeleton, new DocumentModel(List.of(intro, common,
                analysis, analysisIntro, parent, useCase, leaf, design, last))).errors();
        assertThat(errors).filteredOn(issue -> issue.code().equals("STUDENT_BLOCK_OUTSIDE_PART"))
                .extracting(ValidationIssue::blockId).containsExactly(common.id(), parent.id());
        assertThat(service.validateForSave(skeleton, new DocumentModel(List.of(intro, analysis, analysisIntro, useCase,
                leaf, design, last))).ok()).isTrue();
    }

    @Test
    void studentDrawsIntoTeacherDiagramsInsideParts() {
        DocumentBlock outside = diagram(BlockOrigin.TEACHER, DIAGRAM, null);
        DocumentBlock leaf = heading(BlockOrigin.TEACHER, "H1", "Use case");
        DocumentBlock slot = diagram(BlockOrigin.TEACHER, EMPTY, null);
        DocumentModel skeleton = new DocumentModel(List.of(outside, leaf, slot));
        assertThat(codes(service.validateForSubmit(skeleton, skeleton, Map.of()).errors())).contains("DIAGRAM_EMPTY");
        DocumentModel drawn = new DocumentModel(List.of(outside, leaf, redrawn(slot, DIAGRAM)));
        assertThat(service.validateForSave(skeleton, drawn).ok()).isTrue();
        assertThat(service.validateForSubmit(skeleton, drawn, Map.of(DiagramType.CLASS, 1)).ok())
                .as("ô sơ đồ đã vẽ tính là bài làm và đủ sơ đồ bắt buộc").isTrue();
        assertThat(codes(service.validateForSave(skeleton, new DocumentModel(List.of(redrawn(outside, EMPTY), leaf, slot)))
                .errors())).as("sơ đồ ở phần chung vẫn khóa").contains("TEACHER_BLOCK_CHANGED");
        DocumentBlock retyped = new DocumentBlock(slot.id(), BlockOrigin.TEACHER, BlockType.DIAGRAM,
                ((ObjectNode) slot.content()).deepCopy().put("diagramType", "ER").put("xml", DIAGRAM));
        assertThat(codes(service.validateForSave(skeleton, new DocumentModel(List.of(outside, leaf, retyped))).errors()))
                .contains("TEACHER_BLOCK_CHANGED");
    }

    @Test
    void submitChecksRequiredDiagramsEmptyDiagramsAndStudentContent() {
        DocumentBlock teacher = paragraph(BlockOrigin.TEACHER, "Đề");
        DocumentModel skeleton = new DocumentModel(List.of(teacher));
        assertThat(codes(service.validateForSubmit(skeleton, skeleton, Map.of()).errors())).contains("SUBMISSION_EMPTY");
        DocumentModel withEmpty = new DocumentModel(List.of(teacher, diagram(BlockOrigin.STUDENT, EMPTY, null)));
        assertThat(codes(service.validateForSubmit(skeleton, withEmpty, Map.of(DiagramType.CLASS, 1)).errors()))
                .contains("DIAGRAM_EMPTY", "REQUIRED_DIAGRAMS_MISSING");
        DocumentModel done = new DocumentModel(List.of(teacher, diagram(BlockOrigin.STUDENT, DIAGRAM, null)));
        assertThat(service.validateForSubmit(skeleton, done, Map.of(DiagramType.CLASS, 1)).ok()).isTrue();
    }

    @Test
    void sanitizeRemovesScriptsEventHandlersAndExternalLinks() {
        String svg = "<?xml version=\"1.0\"?><!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" "
                + "\"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\"><svg xmlns=\"http://www.w3.org/2000/svg\" "
                + "xmlns:xlink=\"http://www.w3.org/1999/xlink\" content=\"x\" onload=\"alert(1)\"><script>alert(2)</script>"
                + "<a xlink:href=\"https://evil.example\"><rect width=\"1\" height=\"1\" onclick=\"x()\"/></a></svg>";
        DocumentModel clean = service.sanitize(new DocumentModel(List.of(diagram(BlockOrigin.TEACHER, DIAGRAM, svg))));
        String result = clean.blocks().get(0).content().path("svgPreview").asText();
        assertThat(result).contains("<rect").doesNotContain("script", "onload", "onclick", "evil.example", "content=");
    }

    @Test
    void formattingKeepsStrikeColorsAlignAndRejectsBadValues() {
        ObjectNode content = JsonNodeFactory.instance.objectNode().put("align", "CENTER").put("extra", "x");
        content.putArray("runs").addObject().put("text", "Chữ").put("strike", true).put("color", "#C5221F")
                .put("highlight", "#FFF475").put("font", "Comic");
        DocumentBlock block = new DocumentBlock(UUID.randomUUID(), BlockOrigin.TEACHER, BlockType.PARAGRAPH, content);
        DocumentBlock clean = service.sanitize(new DocumentModel(List.of(block))).blocks().get(0);
        assertThat(clean.content().toString()).isEqualTo(
                "{\"runs\":[{\"text\":\"Chữ\",\"strike\":true,\"color\":\"#C5221F\",\"highlight\":\"#FFF475\"}],\"align\":\"CENTER\"}");
        ObjectNode bad = JsonNodeFactory.instance.objectNode().put("align", "MIDDLE");
        bad.putArray("runs").addObject().put("text", "x").put("color", "red");
        assertThat(codes(service.validateSkeleton(new DocumentModel(List.of(new DocumentBlock(UUID.randomUUID(),
                BlockOrigin.TEACHER, BlockType.PARAGRAPH, bad)))).errors())).contains("BLOCK_INVALID", "TEXT_INVALID");
    }

    @Test
    void compactDiagramXmlReadsCompressedPagesAndLabels() {
        String model = "<mxGraphModel><root><mxCell id=\"0\"/><mxCell id=\"1\" parent=\"0\"/>"
                + "<mxCell id=\"a\" value=\"&lt;b&gt;Order&lt;/b&gt;\" vertex=\"1\" parent=\"1\"/>"
                + "<mxCell id=\"b\" value=\"Item\" vertex=\"1\" parent=\"1\"/>"
                + "<mxCell id=\"e\" value=\"has\" edge=\"1\" source=\"a\" target=\"b\" parent=\"1\"/></root></mxGraphModel>";
        String packed = "<mxfile><diagram id=\"p\" name=\"Lớp\">" + deflate(model) + "</diagram></mxfile>";
        String compact = service.compactDiagramXml(packed);
        assertThat(compact).contains("Trang 1 \"Lớp\"", "hình a: Order", "nối a -> b: has");
        assertThat(DiagramXml.cellCount(packed)).isEqualTo(3);
    }

    @Test
    void plainTextMarksTeacherBlocksWhenMixed() {
        DocumentModel document = new DocumentModel(new ArrayList<>(List.of(paragraph(BlockOrigin.TEACHER, "Đề"),
                paragraph(BlockOrigin.STUDENT, "Bài làm"))));
        assertThat(service.toPlainText(document)).isEqualTo("> Đề\n\nBài làm");
    }

    private static String deflate(String text) {
        byte[] input = URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20").getBytes(StandardCharsets.UTF_8);
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
        deflater.setInput(input);
        deflater.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            out.write(buffer, 0, deflater.deflate(buffer));
        }
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }
}
