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
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.NumberingDocument;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.StylesDocument;

/**
 * Xuất tài liệu ra DOCX (BR-U07-60): heading theo style Word (Title, Subtitle, Heading 1–6), đoạn, danh sách, bảng,
 * ảnh. Mỗi sơ đồ thành ảnh PNG kèm alt text "mô tả [drawio:mã]", hyperlink tới trang xem sơ đồ và chunk
 * {@code mxfile}. Ảnh PNG là ảnh Draw.io xuất khi lưu sơ đồ ({@code pngPreview}); sơ đồ chưa vẽ là ảnh trắng; sơ đồ lưu
 * trước 2026-11-27 chưa có ảnh thì dùng hình đại diện dựng sẵn (image chạy không có thư viện vẽ SVG).
 */
final class DocxExporter {

    static final String W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final int MAX_IMAGE_WIDTH_PX = 600;
    /** Bảng rộng gần hết trang: Word cộng thêm lề trong của ô vào bề ngang, để 100% thì bảng lấn ra ngoài lề phải. */
    private static final String TABLE_WIDTH = "97.5%";
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
                doc.createNumbering().setNumbering(NumberingDocument.Factory.parse(buildNumbering(lists)).getNumbering());
            }
            JsonNode formatted = document == null ? null : document.title();
            if (formatted != null && !joinRunText(formatted.path("runs")).isBlank()) {
                // Tên bài có định dạng trên trang soạn (đậm, nghiêng, màu, căn lề): xuất đúng như thế.
                XWPFParagraph p = doc.createParagraph();
                p.setStyle("Title");
                p.setAlignment(ParagraphAlignment.CENTER);
                writeRuns(p, formatted.path("runs"), false);
            } else if (title != null && !title.isBlank()) {
                // Tên bài luôn đứng giữa trang (yêu cầu 2026-11-12), ảnh và sơ đồ cũng vậy.
                addStyledParagraph(doc, "Title", title).setAlignment(ParagraphAlignment.CENTER);
            }
            if (instructions != null && !instructions.isBlank()) {
                for (String line : instructions.split("\\r?\\n")) {
                    if (!line.isBlank()) {
                        doc.createParagraph().createRun().setText(clean(line));
                    }
                }
            }
            int listNumber = 0;
            SectionNumbers numbers = new SectionNumbers();
            for (DocumentBlock block : blocks) {
                if (block.type() == BlockType.LIST) {
                    listNumber++;
                }
                writeBlock(doc, block, linker, listNumber, numbers);
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
                addStyledParagraph(doc, "Title", title).setAlignment(ParagraphAlignment.CENTER);
            }
            if (rubrics.isEmpty()) {
                doc.createParagraph().createRun().setText("Chưa có mục chấm.");
            } else {
                writeRubric(doc, rubrics);
            }
            doc.write(out);
            return out.toByteArray();
        } catch (IOException | XmlException e) {
            throw new IllegalStateException("Không dựng được DOCX", e);
        }
    }

    /**
     * Số câu hỏi như trình soạn hiển thị (I., I.1., I.1.a., I.1.a.aa.; yêu cầu 2026-11-10), ghi vào đầu tiêu đề để mở lại
     * tệp vẫn dựng đúng cây câu hỏi (yêu cầu 2026-11-05: nhập DOCX lấy cây theo số đầu mục).
     */
    static final class SectionNumbers {
        private final java.util.ArrayDeque<Integer> open = new java.util.ArrayDeque<>();
        private final int[] counters = new int[DocxImporter.MAX_LEVEL];

        String nextNumber(int level) {
            while (!open.isEmpty() && open.peekLast() >= level) {
                open.pollLast();
            }
            open.addLast(level);
            int depth = Math.min(open.size(), counters.length);
            counters[depth - 1]++;
            for (int i = depth; i < counters.length; i++) {
                counters[i] = 0;
            }
            java.util.List<Integer> used = new java.util.ArrayList<>();
            for (int i = 0; i < depth; i++) {
                used.add(counters[i]);
            }
            return edu.aiplatform.assignments.model.SectionNumbering.formatNumber(used);
        }
    }

    private void writeBlock(XWPFDocument doc, DocumentBlock block, IDiagramLinker linker, int listNumber,
                       SectionNumbers numbers) {
        JsonNode c = block.content();
        switch (block.type()) {
            case HEADING -> {
                String style = switch (c.path("style").asText("")) {
                    case "TITLE" -> "Title";
                    case "SUBTITLE" -> "Subtitle";
                    default -> "Heading" + parseHeadingLevel(c.path("style").asText(""));
                };
                String number = style.startsWith("Heading")
                        ? numbers.nextNumber(Integer.parseInt(style.substring("Heading".length()))) : "";
                JsonNode formatted = c.path("runs");
                if (formatted.isArray() && !joinRunText(formatted).isBlank()) {
                    // Tiêu đề có định dạng (yêu cầu 2026-11-04): số của câu rồi chữ đúng định dạng trên trang.
                    XWPFParagraph p = doc.createParagraph();
                    p.setStyle(style);
                    if (!number.isEmpty()) {
                        p.createRun().setText(number + " ");
                    }
                    writeRuns(p, formatted, false);
                    align(p, c);
                } else {
                    align(addStyledParagraph(doc, style, (number + " " + c.path("text").asText("")).strip()), c);
                }
            }
            case PARAGRAPH -> {
                XWPFParagraph p = doc.createParagraph();
                align(p, c);
                writeRuns(p, c.path("runs"), false);
            }
            case LIST -> {
                for (JsonNode item : c.path("items")) {
                    XWPFParagraph p = doc.createParagraph();
                    p.setStyle("ListParagraph");
                    p.setNumID(BigInteger.valueOf(listNumber));
                    p.setNumILvl(BigInteger.valueOf(Math.max(0, Math.min(4, item.path("level").asInt(0)))));
                    align(p, c);
                    writeRuns(p, item.path("runs"), false);
                }
            }
            case TABLE -> writeTable(doc, c);
            case IMAGE -> writeImage(doc, c);
            case DIAGRAM -> writeDiagram(doc, block, linker);
        }
    }

    /** Cấp tiêu đề 1–6 của block; kiểu lạ hay thiếu thì coi là cấp 1 để tệp vẫn xuất được. */
    private static int parseHeadingLevel(String style) {
        String digits = style.replaceAll("\\D", "");
        if (digits.isEmpty() || digits.length() > 1) {
            return 1;
        }
        return Math.max(1, Math.min(6, Integer.parseInt(digits)));
    }

    private static XWPFParagraph addStyledParagraph(XWPFDocument doc, String style, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setStyle(style);
        p.createRun().setText(clean(text));
        return p;
    }

    /**
     * Bỏ ký tự XML không cho phép (ký tự điều khiển dán từ nơi khác vào): còn chúng thì Word báo tệp hỏng, không mở được.
     * Tab giữ nguyên; xuống dòng do nơi gọi tách thành ngắt dòng.
     */
    static String clean(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length());
        text.codePoints().filter(cp -> cp == '\t' || cp == '\n' || (cp >= 0x20 && cp <= 0xD7FF)
                || (cp >= 0xE000 && cp <= 0xFFFD) || cp >= 0x10000).forEach(out::appendCodePoint);
        return out.toString();
    }

    /** Chữ thuần của một dãy run. */
    private static String joinRunText(JsonNode runs) {
        StringBuilder out = new StringBuilder();
        for (JsonNode run : runs) {
            out.append(run.path("text").asText(""));
        }
        return out.toString();
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

    private static void writeRuns(XWPFParagraph p, JsonNode runs, boolean bold) {
        for (JsonNode run : runs) {
            String[] lines = clean(run.path("text").asText("")).split("\n", -1);
            XWPFRun r = p.createRun();
            for (int i = 0; i < lines.length; i++) {
                if (i > 0) {
                    r.addBreak();
                }
                r.setText(lines[i], i);
            }
            // Chỉ ghi đậm, nghiêng khi bật: ghi "không đậm" tường minh sẽ đè mất chữ đậm của kiểu Tiêu đề, tên bài.
            if (bold || run.path("bold").asBoolean(false)) {
                r.setBold(true);
            }
            if (run.path("italic").asBoolean(false)) {
                r.setItalic(true);
            }
            if (run.path("underline").asBoolean(false)) {
                r.setUnderline(UnderlinePatterns.SINGLE);
            }
            if (run.path("code").asBoolean(false)) {
                r.setFontFamily("Consolas");
            }
            if (run.path("strike").asBoolean(false)) {
                r.setStrikeThrough(true);
            }
            String color = run.path("color").asText("");
            if (color.matches("#[0-9a-fA-F]{6}")) {
                r.setColor(color.substring(1));
            }
            // Màu tô nền của chữ: tô đúng mã màu (shading), không ép về bảng màu highlight có sẵn của Word.
            String highlight = run.path("highlight").asText("");
            if (highlight.matches("#[0-9a-fA-F]{6}")) {
                CTR ctr = r.getCTR();
                CTShd shade = (ctr.isSetRPr() ? ctr.getRPr() : ctr.addNewRPr()).addNewShd();
                shade.setVal(STShd.CLEAR);
                shade.setColor("auto");
                shade.setFill(highlight.substring(1));
            }
        }
    }

    private static void writeTable(XWPFDocument doc, JsonNode c) {
        JsonNode rows = c.path("rows");
        int columns = 1;
        for (JsonNode row : rows) {
            columns = Math.max(columns, row.size());
        }
        XWPFTable table = doc.createTable(Math.max(1, rows.size()), columns);
        table.setStyleID("TableGrid");
        // Bảng rộng hết trang, các cột bằng nhau như trên trang soạn: bảng ô trống không bị co thành một vạch (lỗi 2026-11-12).
        table.setWidth(TABLE_WIDTH);
        // Cột theo độ rộng người soạn đã kéo (widths, %); không có hoặc lệch số cột thì chia đều.
        JsonNode widths = c.path("widths");
        boolean custom = widths.isArray() && widths.size() == columns;
        for (XWPFTableRow row : table.getRows()) {
            List<XWPFTableCell> cells = row.getTableCells();
            for (int j = 0; j < cells.size(); j++) {
                double percent = custom ? widths.get(j).asDouble(0) : 0;
                cells.get(j).setWidth(String.format(java.util.Locale.ROOT, "%.2f%%",
                        percent > 0 && percent <= 100 ? percent : 100.0 / columns));
            }
        }
        boolean header = c.path("headerRow").asBoolean(false);
        for (int i = 0; i < rows.size(); i++) {
            XWPFTableRow row = table.getRow(i);
            if (header && i == 0) {
                row.setRepeatHeader(true);
            }
            for (int j = 0; j < columns; j++) {
                XWPFTableCell cell = row.getCell(j);
                JsonNode source = rows.get(i).get(j);
                // Chữ trong ô cách đều trên dưới, không dùng khoảng cách sau đoạn của văn bản thường.
                cell.getParagraphs().get(0).setSpacingBefore(60);
                cell.getParagraphs().get(0).setSpacingAfter(60);
                if (source != null) {
                    align(cell.getParagraphs().get(0), source);
                    writeRuns(cell.getParagraphs().get(0), source.path("runs"), header && i == 0);
                }
            }
        }
        // Đoạn trống sau bảng: hai bảng liền nhau không bị Word gộp thành một, bảng không dính sát khối đứng sau.
        doc.createParagraph().setSpacingAfter(0);
    }

    private void writeImage(XWPFDocument doc, JsonNode c) {
        String alt = c.path("alt").asText("");
        byte[] data = readFile(c.path(DocumentFiles.FILE_ID).asText(null));
        int type = detectPictureType(data);
        if (type < 0) {
            doc.createParagraph().createRun().setText(clean("[Ảnh" + (alt.isBlank() ? "" : ": " + alt) + "]"));
            return;
        }
        int[] size = PngChunks.readSize(data).orElse(new int[]{400, 300});
        int requested = c.path("width").asInt(size[0]);
        // Ảnh đứng giữa trang như trên trang soạn.
        XWPFParagraph holder = doc.createParagraph();
        holder.setAlignment(ParagraphAlignment.CENTER);
        addPicture(holder.createRun(), data, type, "image", Math.max(1, Math.min(requested, MAX_IMAGE_WIDTH_PX)),
                size, clean(alt));
    }

    private static void writeDiagram(XWPFDocument doc, DocumentBlock block, IDiagramLinker linker) {
        JsonNode c = block.content();
        String alt = c.path("alt").asText("").strip();
        String code = linker == null ? null : linker.linkFor(block.id());
        String description = (alt.isEmpty() ? "Sơ đồ " + c.path("diagramType").asText("") : alt)
                + (code == null ? "" : " [drawio:" + code + "]");
        // Sơ đồ chưa vẽ (trang soạn hiện "Chưa có sơ đồ"): khung trống, không dùng hình đại diện của sơ đồ đã vẽ.
        String xml = c.path("xml").asText(null);
        boolean drawn = DiagramXml.countCells(xml == null ? "" : xml) > 0;
        byte[] saved = drawn ? PngChunks.readPngDataUri(c.path("pngPreview").asText(null)).orElse(null) : null;
        byte[] png = !drawn ? PngChunks.drawBlankDiagram(xml)
                : saved == null ? PngChunks.drawDiagramPlaceholder(xml)
                : PngChunks.readMxfile(saved).isPresent() || xml == null ? saved
                : PngChunks.addTextChunk(saved, PngChunks.MXFILE, PngChunks.encodeUri(xml));
        int[] size = saved == null ? new int[]{480, 270} : PngChunks.readSize(saved).orElse(new int[]{480, 270});
        int width = Math.max(1, Math.min(size[0], MAX_IMAGE_WIDTH_PX));
        String url = linker == null ? null : linker.viewUrlFor(block.id());
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = url == null ? p.createRun() : p.createHyperlinkRun(url);
        addPicture(run, png, XWPFDocument.PICTURE_TYPE_PNG, "diagram-" + block.id() + ".png", width, size, description);
        XWPFRun caption = p.createRun();
        caption.addBreak();
        caption.setItalic(true);
        caption.setText(clean(drawn ? description.replaceAll("\\s*\\[drawio:[^\\]]*\\]", "")
                + (url == null ? "" : " (bấm vào ảnh để mở sơ đồ)") : "Chưa có sơ đồ"));
    }

    private static void addPicture(XWPFRun run, byte[] data, int type, String name, int width, int[] size,
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

    private static void writeRubric(XWPFDocument doc, List<RubricSection> sections) {
        // Tên bài và chữ Rubric đứng giữa trang, tổng điểm ngay dưới chữ Rubric, căn phải (yêu cầu 2026-11-12).
        addStyledParagraph(doc, "Heading1", "Rubric").setAlignment(ParagraphAlignment.CENTER);
        BigDecimal sum = sections.stream().map(RubricSection::total).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        XWPFParagraph totalLine = doc.createParagraph();
        totalLine.setAlignment(ParagraphAlignment.RIGHT);
        XWPFRun total = totalLine.createRun();
        total.setBold(true);
        total.setText("Tổng điểm: " + formatPoints(sum));
        for (RubricSection section : sections) {
            String partTitle = section.partTitle() == null ? "" : section.partTitle().strip();
            addStyledParagraph(doc, "Heading2", (partTitle + (section.total() == null ? "" : " (" + formatPoints(section.total())
                    + " điểm)")).strip());
            // Rubric atomic: mỗi dòng một mục chấm có điểm; tiêu chí nhiều mục (bản cũ, đề xuất AI) ghi kèm tên tiêu chí.
            List<String[]> rows = new ArrayList<>();
            for (RubricCriterion criterion : section.criteria()) {
                String name = criterion.title() == null ? "" : criterion.title().strip();
                List<RubricItem> items = criterion.items() == null ? List.of() : criterion.items();
                for (RubricItem item : items) {
                    String text = item.text() == null ? "" : item.text().strip();
                    boolean atomic = items.size() == 1 && text.startsWith(name);
                    rows.add(new String[]{atomic || name.isEmpty() ? text : text.isEmpty() ? name : name + ": " + text,
                            item.points() == null ? "" : formatPoints(item.points())});
                }
            }
            XWPFTable table = doc.createTable(rows.size() + 1, 2);
            table.setStyleID("TableGrid");
            // Bảng rộng hết trang, cột Điểm hẹp: mục chấm dài không bị bóp thành cột nhỏ.
            table.setWidth(TABLE_WIDTH);
            for (XWPFTableRow row : table.getRows()) {
                row.getCell(0).setWidth("85%");
                row.getCell(1).setWidth("15%");
            }
            String[] header = {"Mục chấm", "Điểm"};
            for (int j = 0; j < 2; j++) {
                XWPFParagraph head = table.getRow(0).getCell(j).getParagraphs().get(0);
                head.setSpacingBefore(60);
                head.setSpacingAfter(60);
                if (j == 1) {
                    head.setAlignment(ParagraphAlignment.CENTER);
                }
                XWPFRun r = head.createRun();
                r.setBold(true);
                r.setText(header[j]);
            }
            for (int i = 0; i < rows.size(); i++) {
                for (int j = 0; j < 2; j++) {
                    XWPFParagraph cell = table.getRow(i + 1).getCell(j).getParagraphs().get(0);
                    cell.setSpacingBefore(60);
                    cell.setSpacingAfter(60);
                    if (j == 1) {
                        cell.setAlignment(ParagraphAlignment.CENTER);
                    }
                    cell.createRun().setText(clean(rows.get(i)[j]));
                }
            }
        }
    }

    /** Điểm ghi gọn: 2 thay cho 2.00, 1.5 giữ nguyên. */
    private static String formatPoints(BigDecimal value) {
        return value.signum() == 0 ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private byte[] readFile(String fileId) {
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

    private static int detectPictureType(byte[] data) {
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
    static String buildNumbering(List<Boolean> ordered) {
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
            + buildStyle("Title", "Title", -1, "<w:b/><w:sz w:val=\"48\"/><w:szCs w:val=\"48\"/>")
            + buildStyle("Subtitle", "Subtitle", -1, "<w:i/><w:color w:val=\"595959\"/><w:sz w:val=\"30\"/><w:szCs w:val=\"30\"/>")
            + buildStyle("Heading1", "heading 1", 0, "<w:b/><w:sz w:val=\"36\"/><w:szCs w:val=\"36\"/>")
            + buildStyle("Heading2", "heading 2", 1, "<w:b/><w:sz w:val=\"30\"/><w:szCs w:val=\"30\"/>")
            + buildStyle("Heading3", "heading 3", 2, "<w:b/><w:sz w:val=\"26\"/><w:szCs w:val=\"26\"/>")
            + buildStyle("Heading4", "heading 4", 3, "<w:b/><w:i/><w:sz w:val=\"24\"/><w:szCs w:val=\"24\"/>")
            + buildStyle("Heading5", "heading 5", 4, "<w:b/><w:sz w:val=\"22\"/><w:szCs w:val=\"22\"/>")
            + buildStyle("Heading6", "heading 6", 5, "<w:i/><w:sz w:val=\"22\"/><w:szCs w:val=\"22\"/>")
            + "<w:style w:type=\"paragraph\" w:styleId=\"ListParagraph\"><w:name w:val=\"List Paragraph\"/>"
            + "<w:basedOn w:val=\"Normal\"/><w:qFormat/><w:pPr><w:ind w:left=\"720\"/><w:contextualSpacing/></w:pPr></w:style>"
            + "<w:style w:type=\"table\" w:styleId=\"TableGrid\"><w:name w:val=\"Table Grid\"/><w:tblPr><w:tblBorders>"
            + "<w:top w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"auto\"/>"
            + "<w:left w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"auto\"/>"
            + "<w:bottom w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"auto\"/>"
            + "<w:right w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"auto\"/>"
            + "<w:insideH w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"auto\"/>"
            + "<w:insideV w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"auto\"/>"
            + "</w:tblBorders><w:tblCellMar><w:left w:w=\"144\" w:type=\"dxa\"/><w:right w:w=\"108\" w:type=\"dxa\"/>"
            + "</w:tblCellMar></w:tblPr></w:style>"
            + "</w:styles>";

    private static String buildStyle(String id, String name, int outline, String runProperties) {
        return "<w:style w:type=\"paragraph\" w:styleId=\"" + id + "\"><w:name w:val=\"" + name + "\"/>"
                + "<w:basedOn w:val=\"Normal\"/><w:next w:val=\"Normal\"/><w:qFormat/><w:pPr><w:keepNext/>"
                + "<w:spacing w:before=\"240\" w:after=\"120\"/>"
                + (outline >= 0 ? "<w:outlineLvl w:val=\"" + outline + "\"/>" : "")
                + "</w:pPr><w:rPr>" + runProperties + "</w:rPr></w:style>";
    }
}
