package edu.aiplatform.assignments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void tableCellsKeepAlignAndRejectBadValues() {
        ObjectNode content = JsonNodeFactory.instance.objectNode();
        ObjectNode cell = content.putArray("rows").addArray().addObject().put("align", "CENTER");
        cell.putArray("runs").addObject().put("text", "Ô");
        DocumentBlock table = new DocumentBlock(UUID.randomUUID(), BlockOrigin.TEACHER, BlockType.TABLE, content);
        DocumentModel skeleton = new DocumentModel(List.of(table));
        assertThat(service.sanitize(skeleton).blocks().get(0).content().path("rows").get(0).get(0).path("align").asText())
                .isEqualTo("CENTER");
        cell.put("align", "MIDDLE");
        assertThat(codes(service.validateSkeleton(skeleton).errors())).contains("TABLE_INVALID");
    }

    @Test
    void paragraphAfterSubPartsKeepsUnderAndLeavesTheLastPart() {
        DocumentBlock problem = heading(BlockOrigin.TEACHER, "H1", "Đề 1");
        DocumentBlock question = heading(BlockOrigin.TEACHER, "H2", "Câu 1");
        DocumentBlock note = paragraph(BlockOrigin.TEACHER, "Ghi chú cuối đề");
        ((ObjectNode) note.content()).put("under", 1);
        DocumentModel skeleton = new DocumentModel(List.of(problem, question, note));
        assertThat(service.sanitize(skeleton).blocks().get(2).content().path("under").asInt()).isEqualTo(1);
        assertThat(DocumentModelService.collectBlocksInParts(skeleton)).contains(question.id()).doesNotContain(note.id());
        ((ObjectNode) note.content()).put("under", 9);
        assertThat(codes(service.validateSkeleton(skeleton).errors())).contains("BLOCK_INVALID");
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
    void diagramPagesShareOneExpansionBudget() {
        String model = "<mxGraphModel><root><!--" + "a".repeat(500) + "--></root></mxGraphModel>";
        String page = "<diagram>" + deflate(model) + "</diagram>";
        String single = "<mxfile>" + page + "</mxfile>";
        String multiple = "<mxfile>" + page.repeat(2) + "</mxfile>";
        assertThat(DiagramXml.listGraphModels(single, 1000)).hasSize(1);
        assertThatThrownBy(() -> DiagramXml.listGraphModels(multiple, 1000))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void diagramPageLimitAppliesToEveryReadPath() {
        assertThat(DiagramXml.countCells(null)).isZero();
        String xml = "<mxfile>" + "<diagram><mxGraphModel/></diagram>".repeat(101) + "</mxfile>";
        assertThat(DiagramXml.validate(xml)).contains("DIAGRAM_XML_INVALID");
        assertThat(DiagramXml.detectType(xml)).isEqualTo(DiagramType.OTHER);
        assertThat(DiagramXml.countCells(xml)).isZero();
        assertThat(DiagramXml.compactXml(xml, 100)).isEqualTo("(sơ đồ không đọc được)");
    }

    @Test
    void compactDiagramXmlKeepsStereotypeLabelsAndMarksDashedEdges() {
        String model = "<mxGraphModel><root><mxCell id=\"0\"/><mxCell id=\"1\" parent=\"0\"/>"
                + "<mxCell id=\"a\" value=\"Book\" vertex=\"1\" parent=\"1\"/>"
                + "<mxCell id=\"b\" value=\"Pay\" vertex=\"1\" parent=\"1\"/>"
                + "<mxCell id=\"c\" value=\"Cancel\" vertex=\"1\" parent=\"1\"/>"
                + "<mxCell id=\"e1\" value=\"&lt;&lt;include&gt;&gt;\" style=\"endArrow=open;dashed=1;\" edge=\"1\""
                + " source=\"a\" target=\"b\" parent=\"1\"/>"
                + "<mxCell id=\"e2\" value=\"&lt;div&gt;&amp;lt;&amp;lt;extend&amp;gt;&amp;gt;&lt;/div&gt;\""
                + " style=\"endArrow=open;html=1;\" edge=\"1\" source=\"c\" target=\"b\" parent=\"1\"/>"
                + "</root></mxGraphModel>";
        String compact = service.compactDiagramXml(model);
        assertThat(compact).contains("nối \"Book\" [a] -> \"Pay\" [b] (nét đứt): <<include>>",
                "nối \"Cancel\" [c] -> \"Pay\" [b] (nét liền): <<extend>>");
        assertThat(DiagramXml.clean("<b>Đặt</b> sân<br/>&lt;i&gt;")).isEqualTo("Đặt sân <i>");
    }

    @Test
    void plainTextMarksTeacherBlocksWhenMixed() {
        DocumentModel document = new DocumentModel(new ArrayList<>(List.of(paragraph(BlockOrigin.TEACHER, "Đề"),
                paragraph(BlockOrigin.STUDENT, "Bài làm"))));
        assertThat(service.toPlainText(document)).isEqualTo("> Đề\n\nBài làm");
    }

    @Test
    void plainTextWithBlockIdsLabelsEveryBlockAndKeepsTeacherMarks() {
        DocumentBlock teacher = paragraph(BlockOrigin.TEACHER, "Đề");
        DocumentBlock student = paragraph(BlockOrigin.STUDENT, "Bài làm");
        DocumentModel document = new DocumentModel(new ArrayList<>(List.of(teacher, student)));
        assertThat(service.toPlainTextWithBlockIds(document)).isEqualTo(
                "[block " + teacher.id() + "]\n> Đề\n\n[block " + student.id() + "]\nBài làm");
        assertThat(service.toPlainText(document)).doesNotContain("[block ");
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
