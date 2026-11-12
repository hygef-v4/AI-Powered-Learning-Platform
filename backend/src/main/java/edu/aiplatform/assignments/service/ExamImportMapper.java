package edu.aiplatform.assignments.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.assignments.contract.BlockOrigin;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentFiles;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.model.ImportPage;
import edu.aiplatform.assignments.service.FigureCropper.Box;
import edu.aiplatform.assignments.service.FigureCropper.CroppedFigure;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

/**
 * Chuyển JSON AI chép đề thành khung đề (BR-U07-95…97). Tiêu đề trong đề thành đoạn in đậm, không thành mục câu hỏi,
 * vì nhập không tự tách câu hỏi (FR-IMP-03); hình được cắt qua {@code cropper}.
 */
final class ExamImportMapper {

    static final String KIND = "IMPORT";
    static final int MAX_CAPTION = 300;
    static final int MAX_MESSAGE = 300;
    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;
    private static final Pattern HEX = Pattern.compile("^#[0-9a-fA-F]{6}$");
    private static final Set<String> ALIGNS = Set.of("LEFT", "CENTER", "RIGHT", "JUSTIFY");
    private static final Set<String> WARNING_KINDS = Set.of("ILLEGIBLE", "HEADER_FOOTER_REMOVED", "ROTATED_PAGE",
            "OTHER");

    private ExamImportMapper() {
    }

    /** Kết quả ánh xạ: đề xuất lưu vào bản nháp và khung đề để kiểm. */
    record MappedImport(ObjectNode proposal, DocumentModel document) {
    }

    /**
     * @param pageWarnings cảnh báo từ bước chuẩn hóa trang (độ phân giải thấp), đặt trước cảnh báo của AI
     * @param cropper      cắt và lưu một hình; rỗng khi lỗi
     */
    static MappedImport mapResult(JsonNode output, Map<Integer, ImportPage> pages, JsonNode pageWarnings,
                                  BiFunction<ImportPage, Box, Optional<CroppedFigure>> cropper) {
        List<DocumentBlock> blocks = new ArrayList<>();
        ArrayNode warnings = NODES.arrayNode();
        if (pageWarnings != null && pageWarnings.isArray()) {
            pageWarnings.forEach(warnings::add);
        }
        int headings = 0;
        int tables = 0;
        int images = 0;
        int figureIndex = 0;
        for (JsonNode block : output.path("blocks")) {
            String type = block.path("type").asText("");
            switch (type) {
                case "HEADING" -> {
                    ObjectNode content = paragraph(block, true);
                    if (content != null) {
                        if (block.path("level").asInt(2) == 1 && !content.has("align")) {
                            content.put("align", "CENTER");
                        }
                        blocks.add(block(BlockType.PARAGRAPH, content));
                        headings++;
                    }
                }
                case "PARAGRAPH" -> Optional.ofNullable(paragraph(block, false))
                        .ifPresent(content -> blocks.add(block(BlockType.PARAGRAPH, content)));
                case "LIST" -> Optional.ofNullable(list(block))
                        .ifPresent(content -> blocks.add(block(BlockType.LIST, content)));
                case "TABLE" -> {
                    ObjectNode content = table(block);
                    if (content != null) {
                        blocks.add(block(BlockType.TABLE, content));
                        tables++;
                    }
                }
                case "FIGURE" -> {
                    // Khối FIGURE thứ n lấy vị trí ở figures[n]; đầu ra kiểu cũ có page/box ngay trong khối.
                    JsonNode figureInfo = block.has("box") ? block : output.path("figures").path(figureIndex);
                    figureIndex++;
                    String caption = limit(figureInfo.path("caption").asText("").strip(), MAX_CAPTION);
                    int pageNo = figureInfo.path("page").asInt(0);
                    ImportPage page = pages.get(pageNo);
                    JsonNode box = figureInfo.path("box");
                    Optional<CroppedFigure> figure = page == null || !box.isObject() ? Optional.empty()
                            : cropper.apply(page, new Box(box.path("x").asDouble(0), box.path("y").asDouble(0),
                                    box.path("w").asDouble(0), box.path("h").asDouble(0)));
                    if (figure.isPresent()) {
                        ObjectNode content = NODES.objectNode();
                        content.put(DocumentFiles.FILE_REF, figure.get().fileRef());
                        content.put("alt", caption);
                        content.put("width", figure.get().width());
                        blocks.add(block(BlockType.IMAGE, content));
                        images++;
                    } else {
                        ObjectNode content = NODES.objectNode();
                        content.set("runs", NODES.arrayNode().add(NODES.objectNode()
                                .put("text", "[Hình: " + (caption.isEmpty() ? "không có mô tả" : caption) + "]")
                                .put("italic", true)));
                        blocks.add(block(BlockType.PARAGRAPH, content));
                        warnings.add(warning("FIGURE_CROP_FAILED", pageNo,
                                "Không cắt được hình" + (pageNo > 0 ? " ở trang " + pageNo : "")
                                        + "; hãy chèn lại ảnh"));
                    }
                }
                default -> {
                    // Loại khối lạ: schema đã chặn, bỏ qua cho chắc.
                }
            }
        }
        for (JsonNode warning : output.path("warnings")) {
            String kind = warning.path("kind").asText("OTHER");
            warnings.add(warning(WARNING_KINDS.contains(kind) ? kind : "OTHER", warning.path("page").asInt(0),
                    limit(warning.path("message").asText(""), MAX_MESSAGE)));
        }
        DocumentModel document = new DocumentModel(List.copyOf(blocks));
        ObjectNode proposal = NODES.objectNode();
        proposal.put("kind", KIND);
        proposal.put("title", limit(output.path("title").asText("").strip(), edu.aiplatform.assignments.model.Assignment.TITLE_MAX));
        proposal.set("warnings", warnings);
        ObjectNode counts = proposal.putObject("counts");
        counts.put("headings", headings);
        counts.put("tables", tables);
        counts.put("images", images);
        counts.put("illegible", countIllegible(output));
        return new MappedImport(proposal, document);
    }

    private static DocumentBlock block(BlockType type, ObjectNode content) {
        return new DocumentBlock(UUID.randomUUID(), BlockOrigin.TEACHER, type, content);
    }

    private static ObjectNode paragraph(JsonNode block, boolean bold) {
        ArrayNode runs = runs(block.path("runs"), bold);
        if (runs.isEmpty()) {
            return null;
        }
        ObjectNode content = NODES.objectNode();
        String align = block.path("align").asText("");
        if (ALIGNS.contains(align)) {
            content.put("align", align);
        }
        content.set("runs", runs);
        return content;
    }

    private static ObjectNode list(JsonNode block) {
        ArrayNode items = NODES.arrayNode();
        for (JsonNode item : block.path("items")) {
            ArrayNode runs = runs(item.path("runs"), false);
            if (!runs.isEmpty()) {
                items.addObject().put("level", Math.max(0, Math.min(4, item.path("level").asInt(0))))
                        .set("runs", runs);
            }
        }
        if (items.isEmpty()) {
            return null;
        }
        ObjectNode content = NODES.objectNode();
        content.put("ordered", block.path("ordered").asBoolean(false));
        content.set("items", items);
        return content;
    }

    /** Bảng đều cột theo hàng dài nhất; bảng không có chữ nào thì bỏ. */
    private static ObjectNode table(JsonNode block) {
        int columns = 0;
        for (JsonNode row : block.path("rows")) {
            columns = Math.max(columns, Math.min(DocumentModelService.MAX_CELLS, row.size()));
        }
        if (columns == 0) {
            return null;
        }
        ArrayNode rows = NODES.arrayNode();
        boolean hasText = false;
        for (JsonNode row : block.path("rows")) {
            if (rows.size() >= DocumentModelService.MAX_ROWS) {
                break;
            }
            ArrayNode cells = rows.addArray();
            for (int i = 0; i < columns; i++) {
                ArrayNode runs = runs(row.path(i).path("runs"), false);
                hasText |= !runs.isEmpty();
                cells.addObject().set("runs", runs);
            }
        }
        if (!hasText) {
            return null;
        }
        ObjectNode content = NODES.objectNode();
        content.put("headerRow", block.path("headerRow").asBoolean(false));
        content.set("rows", rows);
        return content;
    }

    private static ArrayNode runs(JsonNode source, boolean forceBold) {
        ArrayNode runs = NODES.arrayNode();
        for (JsonNode run : source) {
            String text = run.path("text").asText("");
            if (text.isBlank() || runs.size() >= DocumentModelService.MAX_RUNS) {
                continue;
            }
            ObjectNode out = runs.addObject();
            out.put("text", limit(text, DocumentModelService.MAX_TEXT));
            if (forceBold || run.path("bold").asBoolean(false)) {
                out.put("bold", true);
            }
            if (run.path("italic").asBoolean(false)) {
                out.put("italic", true);
            }
            if (run.path("underline").asBoolean(false)) {
                out.put("underline", true);
            }
            String color = run.path("color").asText("");
            if (isClearColor(color)) {
                out.put("color", color.toUpperCase(java.util.Locale.ROOT));
            }
        }
        return runs;
    }

    /** Màu rõ ràng: đúng {@code #RRGGBB} và không phải đen/xám đậm (ba kênh chênh nhau ≤ 40, kênh lớn nhất ≤ 0x80). */
    static boolean isClearColor(String color) {
        if (color == null || !HEX.matcher(color).matches()) {
            return false;
        }
        int r = Integer.parseInt(color.substring(1, 3), 16);
        int g = Integer.parseInt(color.substring(3, 5), 16);
        int b = Integer.parseInt(color.substring(5, 7), 16);
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        return !(max - min <= 40 && max <= 0x80);
    }

    private static int countIllegible(JsonNode output) {
        String text = output.path("blocks").toString();
        int count = 0;
        for (int at = text.indexOf(ExamImportPrompt.ILLEGIBLE); at >= 0;
             at = text.indexOf(ExamImportPrompt.ILLEGIBLE, at + 1)) {
            count++;
        }
        return count;
    }

    private static ObjectNode warning(String kind, int page, String message) {
        ObjectNode warning = NODES.objectNode();
        warning.put("kind", kind);
        if (page > 0) {
            warning.put("page", page);
        }
        warning.put("message", message);
        return warning;
    }

    private static String limit(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }
}
