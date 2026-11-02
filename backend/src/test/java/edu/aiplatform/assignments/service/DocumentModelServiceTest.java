package edu.aiplatform.assignments.service;

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
                ((ObjectNode) slot.content()).deepCopy().put("diagramType", "USE_CASE").put("xml", DIAGRAM));
        assertThat(service.validateForSave(skeleton, new DocumentModel(List.of(outside, leaf, retyped))).ok())
                .as("loại sơ đồ đi theo hình vẽ").isTrue();
        DocumentBlock recaptioned = new DocumentBlock(slot.id(), BlockOrigin.TEACHER, BlockType.DIAGRAM,
                ((ObjectNode) slot.content()).deepCopy().put("alt", "mô tả khác").put("xml", DIAGRAM));
        assertThat(codes(service.validateForSave(skeleton, new DocumentModel(List.of(outside, leaf, recaptioned)))
                .errors())).as("mô tả của đề giữ nguyên").contains("TEACHER_BLOCK_CHANGED");
    }

    private static String drawing(String... styles) {
        StringBuilder cells = new StringBuilder();
        for (int i = 0; i < styles.length; i++) {
            boolean edge = styles[i].startsWith("edge:");
            String style = edge ? styles[i].substring(5) : styles[i];
            cells.append("<mxCell id=\"c").append(i).append("\" style=\"").append(style).append(edge ? "\" edge=\"1\"" : "\" vertex=\"1\"")
                    .append(" parent=\"1\"/>");
        }
        return "<mxfile><diagram id=\"p\"><mxGraphModel><root><mxCell id=\"0\"/><mxCell id=\"1\" parent=\"0\"/>" + cells
                + "</root></mxGraphModel></diagram></mxfile>";
    }

    @Test
    void diagramTypeIsDetectedFromTheDrawing() {
        assertThat(DiagramXml.detectType(drawing("shape=umlActor;html=1;", "ellipse;whiteSpace=wrap;")))
                .isEqualTo(DiagramType.USE_CASE);
        assertThat(DiagramXml.detectType(drawing("shape=umlLifeline;participant=umlActor;", "shape=umlLifeline;")))
                .isEqualTo(DiagramType.SEQUENCE);
        assertThat(DiagramXml.detectType(drawing("swimlane;fontStyle=1;childLayout=stackLayout;", "text;html=1;")))
                .isEqualTo(DiagramType.CLASS);
        assertThat(DiagramXml.detectType(drawing("shape=table;childLayout=tableLayout;",
                "edge:edgeStyle=entityRelationEdgeStyle;endArrow=ERmany;"))).isEqualTo(DiagramType.ER);
        assertThat(DiagramXml.detectType(drawing("rounded=0;", "rhombus;", "ellipse;", "edge:endArrow=none;")))
                .as("ER ký pháp Chen").isEqualTo(DiagramType.ER);
        assertThat(DiagramXml.detectType(drawing("ellipse;shape=startState;", "rhombus;", "ellipse;shape=endState;",
                "edge:html=1;"))).isEqualTo(DiagramType.ACTIVITY);
        assertThat(DiagramXml.detectType(drawing("ellipse;shape=startState;", "rounded=1;arcSize=40;")))
                .isEqualTo(DiagramType.STATE);
        assertThat(DiagramXml.detectType(drawing("shape=component;align=left;"))).isEqualTo(DiagramType.COMPONENT);
        assertThat(DiagramXml.detectType(drawing("rounded=0;whiteSpace=wrap;"))).isEqualTo(DiagramType.OTHER);
        assertThat(DiagramXml.detectType("không phải xml")).isEqualTo(DiagramType.OTHER);
        DocumentBlock sent = new DocumentBlock(UUID.randomUUID(), BlockOrigin.STUDENT, BlockType.DIAGRAM,
                JsonNodeFactory.instance.objectNode().put("diagramType", "OTHER").put("xml", DIAGRAM));
        assertThat(service.sanitize(new DocumentModel(List.of(sent))).blocks().get(0).content().path("diagramType").asText())
                .as("lưu là lấy loại theo hình vẽ").isEqualTo("USE_CASE");
        DocumentBlock empty = new DocumentBlock(UUID.randomUUID(), BlockOrigin.TEACHER, BlockType.DIAGRAM,
                JsonNodeFactory.instance.objectNode().put("diagramType", "CLASS").put("xml", EMPTY));
        assertThat(service.sanitize(new DocumentModel(List.of(empty))).blocks().get(0).content().path("diagramType").asText())
                .as("chưa vẽ thì giữ loại gửi lên").isEqualTo("CLASS");
    }

    @Test
    void unknownDiagramsFillMissingRequiredTypes() {
        DocumentBlock teacher = paragraph(BlockOrigin.TEACHER, "Đề");
        DocumentModel skeleton = new DocumentModel(List.of(teacher));
        DocumentBlock other = new DocumentBlock(UUID.randomUUID(), BlockOrigin.STUDENT, BlockType.DIAGRAM,
                JsonNodeFactory.instance.objectNode().put("diagramType", "OTHER").put("xml", DIAGRAM));
        DocumentModel work = new DocumentModel(List.of(teacher, other));
        assertThat(service.validateForSubmit(skeleton, work, Map.of(DiagramType.USE_CASE, 1)).ok()).isTrue();
        assertThat(codes(service.validateForSubmit(skeleton, work, Map.of(DiagramType.USE_CASE, 1, DiagramType.OTHER, 1))
                .errors())).as("sơ đồ OTHER đủ yêu cầu của chính OTHER trước").contains("REQUIRED_DIAGRAMS_MISSING");
        DocumentModel classOnly = new DocumentModel(List.of(teacher, diagram(BlockOrigin.STUDENT, DIAGRAM, null)));
        assertThat(codes(service.validateForSubmit(skeleton, classOnly, Map.of(DiagramType.USE_CASE, 1)).errors()))
                .as("sơ đồ đã nhận ra loại không tính cho loại khác").contains("REQUIRED_DIAGRAMS_MISSING");
    }

    /** Bảng thử: ô bắt đầu bằng "+" là ô người học thêm (cờ {@code added}). */
    private static DocumentBlock table(UUID id, boolean headerRow, String[]... rows) {
        ObjectNode content = JsonNodeFactory.instance.objectNode().put("headerRow", headerRow);
        var array = content.putArray("rows");
        for (String[] row : rows) {
            var cells = array.addArray();
            for (String cell : row) {
                ObjectNode next = cells.addObject();
                next.putArray("runs").addObject().put("text", cell.startsWith("+") ? cell.substring(1) : cell);
                if (cell.startsWith("+")) {
                    next.put("added", true);
                }
            }
        }
        return new DocumentBlock(id, BlockOrigin.TEACHER, BlockType.TABLE, content);
    }

    @Test
    void studentFillsTeacherTablesInsideParts() {
        UUID commonId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        DocumentBlock common = table(commonId, true, new String[]{"A"}, new String[]{""});
        DocumentBlock leaf = heading(BlockOrigin.TEACHER, "H1", "Kiểm thử");
        DocumentBlock slot = table(slotId, true, new String[]{"Ca", "Kết quả"}, new String[]{"", ""});
        DocumentModel skeleton = new DocumentModel(List.of(common, leaf, slot));
        DocumentBlock filled = table(slotId, true, new String[]{"Ca", "Kết quả"}, new String[]{"Đăng nhập", "Đạt"},
                new String[]{"Thêm", "", ""});
        DocumentModel work = new DocumentModel(List.of(common, leaf, filled));
        assertThat(service.validateForSave(skeleton, work).ok()).as("sửa ô, thêm hàng, thêm cột").isTrue();
        assertThat(service.validateForSubmit(skeleton, work, Map.of()).ok()).as("bảng đã điền là bài làm").isTrue();
        assertThat(codes(service.validateForSubmit(skeleton, skeleton, Map.of()).errors())).contains("SUBMISSION_EMPTY");
        assertThat(codes(service.validateForSave(skeleton, new DocumentModel(List.of(common, leaf,
                table(slotId, true, new String[]{"Ca", "Kết quả"})))).errors())).as("không bớt hàng của đề")
                .contains("TEACHER_BLOCK_CHANGED");
        assertThat(codes(service.validateForSave(skeleton, new DocumentModel(List.of(common, leaf,
                table(slotId, false, new String[]{"Ca", "Kết quả"}, new String[]{"", ""})))).errors()))
                .as("không đổi hàng tiêu đề").contains("TEACHER_BLOCK_CHANGED");
        assertThat(codes(service.validateForSave(skeleton, new DocumentModel(List.of(
                table(commonId, true, new String[]{"A"}, new String[]{"x"}), leaf, slot))).errors()))
                .as("bảng ở phần chung vẫn khóa").contains("TEACHER_BLOCK_CHANGED");
        DocumentModel inserted = new DocumentModel(List.of(common, leaf, table(slotId, true,
                new String[]{"Ca", "+Ghi chú", "Kết quả"}, new String[]{"+Mới", "+", "+"}, new String[]{"", "+", ""})));
        assertThat(service.validateForSave(skeleton, inserted).ok()).as("chèn hàng, cột ở giữa bảng").isTrue();
        DocumentModel relabelled = new DocumentModel(List.of(common, leaf, table(slotId, true,
                new String[]{"Ca", "Kết quả"}, new String[]{"+", "+"})));
        assertThat(codes(service.validateForSave(skeleton, relabelled).errors()))
                .as("đánh dấu ô của đề là ô thêm để xóa thì không được").contains("TEACHER_BLOCK_CHANGED");
        DocumentBlock sanitized = service.sanitize(inserted).blocks().get(2);
        assertThat(sanitized.content().path("rows").get(1).get(0).path("added").asBoolean()).as("lưu giữ cờ ô thêm").isTrue();
        assertThat(sanitized.content().path("rows").get(0).get(0).has("added")).isFalse();
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
