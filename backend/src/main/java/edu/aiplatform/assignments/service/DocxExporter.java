package edu.aiplatform.assignments.service;

import com.fasterxml.jackson.databind.JsonNode;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentFiles;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.IDiagramLinker;
import edu.aiplatform.assignments.contract.RubricCriterion;
import edu.aiplatform.assignments.contract.RubricItem;
import edu.aiplatform.files.contract.IArtifact;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlException;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualDrawingProps;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTInline;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDrawing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.NumberingDocument;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.StylesDocument;

/**
 * Xuất tài liệu ra DOCX (BR-U07-60): heading theo style Word (Title, Subtitle, Heading 1–6), đoạn, danh sách, bảng,
 * ảnh. Mỗi sơ đồ thành ảnh PNG kèm alt text "mô tả [drawio:mã]", hyperlink tới trang xem sơ đồ và chunk
 * {@code mxfile}. Ảnh PNG là hình đại diện dựng sẵn, không vẽ lại sơ đồ (image chạy không có thư viện vẽ SVG).
 */
final class DocxExporter {

    static final String W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final int MAX_IMAGE_WIDTH_PX = 600;
    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;

    record RubricSection(String partTitle, List<RubricCriterion> criteria, BigDecimal total) {
    }

    private final IArtifact artifacts;

    DocxExporter(IArtifact artifacts) {
        this.artifacts = artifacts;
    }

    byte[] export(String title, String instructions, DocumentModel document, IDiagramLinker linker) {
        List<DocumentBlock> blocks = document == null || document.blocks() == null ? List.of() : document.blocks();
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            doc.createStyles().setStyles(StylesDocument.Factory.parse(STYLES).getStyles());
            List<Boolean> lists = blocks.stream().filter(b -> b.type() == BlockType.LIST)
                    .map(b -> b.content().path("ordered").asBoolean(false)).toList();
            if (!lists.isEmpty()) {
                doc.createNumbering().setNumbering(NumberingDocument.Factory.parse(numbering(lists)).getNumbering());
            }
            if (title != null && !title.isBlank()) {
                styled(doc, "Title", title);
            }
            if (instructions != null && !instructions.isBlank()) {
                for (String line : instructions.split("\\r?\\n")) {
                    if (!line.isBlank()) {
                        doc.createParagraph().createRun().setText(line);
                    }
                }
            }
            int listNumber = 0;
            for (DocumentBlock block : blocks) {
                if (block.type() == BlockType.LIST) {
                    listNumber++;
                }
                write(doc, block, linker, listNumber);
            }
            doc.write(out);
            return out.toByteArray();
        } catch (IOException | XmlException e) {
            throw new IllegalStateException("Không dựng được DOCX", e);
        }
    }

    /** Rubric xuất thành tệp riêng (yêu cầu 2026-10-22): tên bài, rồi mỗi phần một bảng "Mục chấm | Điểm". */
    byte[] exportRubric(String title, List<RubricSection> rubrics) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            doc.createStyles().setStyles(StylesDocument.Factory.parse(STYLES).getStyles());
            if (title != null && !title.isBlank()) {
                styled(doc, "Title", title);
            }
            if (rubrics.isEmpty()) {
                doc.createParagraph().createRun().setText("Chưa có mục chấm.");
            } else {
                rubric(doc, rubrics);
            }
            doc.write(out);
            return out.toByteArray();
        } catch (IOException | XmlException e) {
            throw new IllegalStateException("Không dựng được DOCX", e);
        }
    }

    private void write(XWPFDocument doc, DocumentBlock block, IDiagramLinker linker, int listNumber) {
        JsonNode c = block.content();
        switch (block.type()) {
            case HEADING -> {
                String style = switch (c.path("style").asText("")) {
                    case "TITLE" -> "Title";
                    case "SUBTITLE" -> "Subtitle";
                    default -> "Heading" + c.path("style").asText("H1").substring(1);
                };
                align(styled(doc, style, c.path("text").asText("")), c);
            }
            case PARAGRAPH -> {
                XWPFParagraph p = doc.createParagraph();
                align(p, c);
                runs(p, c.path("runs"), false);
            }
            case LIST -> {
                for (JsonNode item : c.path("items")) {
                    XWPFParagraph p = doc.createParagraph();
                    p.setStyle("ListParagraph");
                    p.setNumID(BigInteger.valueOf(listNumber));
                    p.setNumILvl(BigInteger.valueOf(Math.max(0, Math.min(4, item.path("level").asInt(0)))));
                    align(p, c);
                    runs(p, item.path("runs"), false);
                }
            }
            case TABLE -> table(doc, c);
            case IMAGE -> image(doc, c);
            case DIAGRAM -> diagram(doc, block, linker);
        }
    }

    private static XWPFParagraph styled(XWPFDocument doc, String style, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setStyle(style);
        p.createRun().setText(text);
        return p;
    }

    /** Căn lề của block (tiêu đề, đoạn, danh sách). */
    private static void align(XWPFParagraph p, JsonNode c) {
        switch (c.path("align").asText("")) {
            case "CENTER" -> p.setAlignment(ParagraphAlignment.CENTER);
            case "RIGHT" -> p.setAlignment(ParagraphAlignment.RIGHT);
            case "JUSTIFY" -> p.setAlignment(ParagraphAlignment.BOTH);
            default -> { }
        }
    }

    private static void runs(XWPFParagraph p, JsonNode runs, boolean bold) {
        for (JsonNode run : runs) {
            String[] lines = run.path("text").asText("").split("\n", -1);
            XWPFRun r = p.createRun();
            for (int i = 0; i < lines.length; i++) {
                if (i > 0) {
                    r.addBreak();
                }
                r.setText(lines[i], i);
            }
            r.setBold(bold || run.path("bold").asBoolean(false));
            r.setItalic(run.path("italic").asBoolean(false));
            if (run.path("underline").asBoolean(false)) {
                r.setUnderline(UnderlinePatterns.SINGLE);
            }
            if (run.path("code").asBoolean(false)) {
                r.setFontFamily("Consolas");
            }
            if (run.path("strike").asBoolean(false)) {
                r.setStrikeThrough(true);
            }
            if (run.path("color").isTextual()) {
                r.setColor(run.path("color").asText().substring(1));
            }
        }
    }

    private static void table(XWPFDocument doc, JsonNode c) {
        JsonNode rows = c.path("rows");
        int columns = 1;
        for (JsonNode row : rows) {
            columns = Math.max(columns, row.size());
        }
        XWPFTable table = doc.createTable(Math.max(1, rows.size()), columns);
        table.setStyleID("TableGrid");
        boolean header = c.path("headerRow").asBoolean(false);
        for (int i = 0; i < rows.size(); i++) {
            XWPFTableRow row = table.getRow(i);
            if (header && i == 0) {
                row.setRepeatHeader(true);
            }
            for (int j = 0; j < columns; j++) {
                XWPFTableCell cell = row.getCell(j);
                JsonNode source = rows.get(i).get(j);
                if (source != null) {
                    runs(cell.getParagraphs().get(0), source.path("runs"), header && i == 0);
                }
            }
        }
    }

    private void image(XWPFDocument doc, JsonNode c) {
        String alt = c.path("alt").asText("");
        byte[] data = read(c.path(DocumentFiles.FILE_ID).asText(null));
        int type = pictureType(data);
        if (type < 0) {
            doc.createParagraph().createRun().setText("[Ảnh" + (alt.isBlank() ? "" : ": " + alt) + "]");
            return;
        }
        int[] size = PngChunks.size(data).orElse(new int[]{400, 300});
        int requested = c.path("width").asInt(size[0]);
        picture(doc.createParagraph().createRun(), data, type, "image", Math.min(requested, MAX_IMAGE_WIDTH_PX),
                size, alt);
    }

    private static void diagram(XWPFDocument doc, DocumentBlock block, IDiagramLinker linker) {
        JsonNode c = block.content();
        String alt = c.path("alt").asText("").strip();
        String code = linker == null ? null : linker.linkFor(block.id());
        String description = (alt.isEmpty() ? "Sơ đồ " + c.path("diagramType").asText("") : alt)
                + (code == null ? "" : " [drawio:" + code + "]");
        byte[] png = PngChunks.diagramPlaceholder(c.path("xml").asText(null));
        String url = linker == null ? null : linker.viewUrlFor(block.id());
        XWPFParagraph p = doc.createParagraph();
        XWPFRun run = url == null ? p.createRun() : p.createHyperlinkRun(url);
        picture(run, png, XWPFDocument.PICTURE_TYPE_PNG, "diagram-" + block.id() + ".png", 480, new int[]{480, 270},
                description);
        XWPFRun caption = p.createRun();
        caption.addBreak();
        caption.setItalic(true);
        caption.setText(description.replaceAll("\\s*\\[drawio:[^\\]]*\\]", "")
                + (url == null ? "" : " (bấm vào ảnh để mở sơ đồ)"));
    }

    private static void picture(XWPFRun run, byte[] data, int type, String name, int width, int[] size,
                                String description) {
        int height = size[0] <= 0 ? width * 3 / 4 : (int) Math.round((double) width * size[1] / size[0]);
        try (InputStream in = new ByteArrayInputStream(data)) {
            run.addPicture(in, type, name, Units.pixelToEMU(width), Units.pixelToEMU(Math.max(1, height)));
        } catch (Exception e) {
            run.setText("[Ảnh không chèn được]");
            return;
        }
        CTR ctr = run.getCTR();
        if (ctr.sizeOfDrawingArray() > 0) {
            CTDrawing drawing = ctr.getDrawingArray(ctr.sizeOfDrawingArray() - 1);
            if (drawing.sizeOfInlineArray() > 0) {
                CTInline inline = drawing.getInlineArray(0);
                inline.getDocPr().setDescr(description);
            }
        }
        if (!run.getEmbeddedPictures().isEmpty()) {
            CTNonVisualDrawingProps props = run.getEmbeddedPictures().get(run.getEmbeddedPictures().size() - 1)
                    .getCTPicture().getNvPicPr().getCNvPr();
            props.setDescr(description);
        }
    }

    private static void rubric(XWPFDocument doc, List<RubricSection> sections) {
        styled(doc, "Heading1", "Rubric");
        for (RubricSection section : sections) {
            styled(doc, "Heading2", section.partTitle() + " (" + section.total().stripTrailingZeros().toPlainString()
                    + " điểm)");
            // Rubric atomic: mỗi dòng một mục chấm có điểm; tiêu chí nhiều mục (bản cũ, đề xuất AI) ghi kèm tên tiêu chí.
            List<String[]> rows = new ArrayList<>();
            for (RubricCriterion criterion : section.criteria()) {
                for (RubricItem item : criterion.items()) {
                    String text = item.text() == null ? "" : item.text();
                    boolean atomic = criterion.items().size() == 1 && text.startsWith(criterion.title());
                    rows.add(new String[]{atomic || criterion.title().isBlank() ? text : criterion.title() + ": " + text,
                            item.points() == null ? "" : item.points().stripTrailingZeros().toPlainString()});
                }
            }
            XWPFTable table = doc.createTable(rows.size() + 1, 2);
            table.setStyleID("TableGrid");
            String[] header = {"Mục chấm", "Điểm"};
            for (int j = 0; j < 2; j++) {
                XWPFRun r = table.getRow(0).getCell(j).getParagraphs().get(0).createRun();
                r.setBold(true);
                r.setText(header[j]);
            }
            for (int i = 0; i < rows.size(); i++) {
                for (int j = 0; j < 2; j++) {
                    table.getRow(i + 1).getCell(j).getParagraphs().get(0).createRun().setText(rows.get(i)[j]);
                }
            }
        }
    }

    private byte[] read(String fileId) {
        if (fileId == null) {
            return null;
        }
        try (InputStream in = artifacts.open(fileId)) {
            byte[] data = in.readNBytes(MAX_IMAGE_BYTES + 1);
            return data.length > MAX_IMAGE_BYTES ? null : data;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static int pictureType(byte[] data) {
        if (data == null || data.length < 4) {
            return -1;
        }
        if (PngChunks.isPng(data)) {
            return XWPFDocument.PICTURE_TYPE_PNG;
        }
        if ((data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8) {
            return XWPFDocument.PICTURE_TYPE_JPEG;
        }
        if (data[0] == 'G' && data[1] == 'I' && data[2] == 'F') {
            return XWPFDocument.PICTURE_TYPE_GIF;
        }
        return -1;
    }

    /** Mỗi danh sách một {@code num}: 0 là gạch đầu dòng, 1 là đánh số (đánh lại từ 1). */
    static String numbering(List<Boolean> ordered) {
        StringBuilder xml = new StringBuilder("<w:numbering xmlns:w=\"" + W + "\">");
        for (int abstractId = 0; abstractId < 2; abstractId++) {
            xml.append("<w:abstractNum w:abstractNumId=\"").append(abstractId).append("\">")
                    .append("<w:multiLevelType w:val=\"hybridMultilevel\"/>");
            for (int level = 0; level < 5; level++) {
                xml.append("<w:lvl w:ilvl=\"").append(level).append("\"><w:start w:val=\"1\"/>")
                        .append(abstractId == 0 ? "<w:numFmt w:val=\"bullet\"/><w:lvlText w:val=\"•\"/>"
                                : "<w:numFmt w:val=\"decimal\"/><w:lvlText w:val=\"%" + (level + 1) + ".\"/>")
                        .append("<w:lvlJc w:val=\"left\"/><w:pPr><w:ind w:left=\"").append(720 * (level + 1))
                        .append("\" w:hanging=\"360\"/></w:pPr></w:lvl>");
            }
            xml.append("</w:abstractNum>");
        }
        for (int i = 0; i < ordered.size(); i++) {
            xml.append("<w:num w:numId=\"").append(i + 1).append("\"><w:abstractNumId w:val=\"")
                    .append(ordered.get(i) ? 1 : 0).append("\"/>");
            if (ordered.get(i)) {
                xml.append("<w:lvlOverride w:ilvl=\"0\"><w:startOverride w:val=\"1\"/></w:lvlOverride>");
            }
            xml.append("</w:num>");
        }
        return xml.append("</w:numbering>").toString();
    }

    private static final String STYLES = "<w:styles xmlns:w=\"" + W + "\">"
            + "<w:docDefaults><w:rPrDefault><w:rPr><w:rFonts w:ascii=\"Calibri\" w:hAnsi=\"Calibri\" w:eastAsia=\"Calibri\""
            + " w:cs=\"Calibri\"/><w:sz w:val=\"22\"/><w:szCs w:val=\"22\"/><w:lang w:val=\"vi-VN\"/></w:rPr></w:rPrDefault>"
            + "<w:pPrDefault><w:pPr><w:spacing w:after=\"120\" w:line=\"276\" w:lineRule=\"auto\"/></w:pPr></w:pPrDefault>"
            + "</w:docDefaults>"
            + "<w:style w:type=\"paragraph\" w:default=\"1\" w:styleId=\"Normal\"><w:name w:val=\"Normal\"/><w:qFormat/></w:style>"
            + style("Title", "Title", -1, "<w:b/><w:sz w:val=\"48\"/><w:szCs w:val=\"48\"/>")
            + style("Subtitle", "Subtitle", -1, "<w:i/><w:color w:val=\"595959\"/><w:sz w:val=\"30\"/><w:szCs w:val=\"30\"/>")
            + style("Heading1", "heading 1", 0, "<w:b/><w:sz w:val=\"36\"/><w:szCs w:val=\"36\"/>")
            + style("Heading2", "heading 2", 1, "<w:b/><w:sz w:val=\"30\"/><w:szCs w:val=\"30\"/>")
            + style("Heading3", "heading 3", 2, "<w:b/><w:sz w:val=\"26\"/><w:szCs w:val=\"26\"/>")
            + style("Heading4", "heading 4", 3, "<w:b/><w:i/><w:sz w:val=\"24\"/><w:szCs w:val=\"24\"/>")
            + style("Heading5", "heading 5", 4, "<w:b/><w:sz w:val=\"22\"/><w:szCs w:val=\"22\"/>")
            + style("Heading6", "heading 6", 5, "<w:i/><w:sz w:val=\"22\"/><w:szCs w:val=\"22\"/>")
            + "<w:style w:type=\"paragraph\" w:styleId=\"ListParagraph\"><w:name w:val=\"List Paragraph\"/>"
            + "<w:basedOn w:val=\"Normal\"/><w:qFormat/><w:pPr><w:ind w:left=\"720\"/><w:contextualSpacing/></w:pPr></w:style>"
            + "<w:style w:type=\"table\" w:styleId=\"TableGrid\"><w:name w:val=\"Table Grid\"/><w:tblPr><w:tblBorders>"
            + "<w:top w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"auto\"/>"
            + "<w:left w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"auto\"/>"
            + "<w:bottom w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"auto\"/>"
            + "<w:right w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"auto\"/>"
            + "<w:insideH w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"auto\"/>"
            + "<w:insideV w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"auto\"/>"
            + "</w:tblBorders></w:tblPr></w:style>"
            + "</w:styles>";

    private static String style(String id, String name, int outline, String runProperties) {
        return "<w:style w:type=\"paragraph\" w:styleId=\"" + id + "\"><w:name w:val=\"" + name + "\"/>"
                + "<w:basedOn w:val=\"Normal\"/><w:next w:val=\"Normal\"/><w:qFormat/><w:pPr><w:keepNext/>"
                + "<w:spacing w:before=\"240\" w:after=\"120\"/>"
                + (outline >= 0 ? "<w:outlineLvl w:val=\"" + outline + "\"/>" : "")
                + "</w:pPr><w:rPr>" + runProperties + "</w:rPr></w:style>";
    }
}
