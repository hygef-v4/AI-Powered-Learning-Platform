package edu.aiplatform.assignments.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.assignments.contract.BlockOrigin;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.DiagramType;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentFiles;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.DocumentPart;
import edu.aiplatform.assignments.contract.IDocumentModel;
import edu.aiplatform.assignments.contract.ValidationIssue;
import edu.aiplatform.assignments.contract.ValidationResult;
import edu.aiplatform.assignments.domain.PartCalculator;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Cài {@link IDocumentModel} (BR-U07-10…15): kiểm cấu trúc, trần kích thước, XML sơ đồ an toàn; bài làm không đụng
 * block của người soạn; đủ sơ đồ khi nộp; chữ và XML rút gọn cho AI. Dùng bởi U07 (khung) và U08, U09, U12.
 */
@Service
public class DocumentModelService implements IDocumentModel {

    static final int MAX_BLOCKS = 2_000;
    static final int MAX_DIAGRAMS = 100;
    static final long MAX_DOCUMENT_BYTES = 20L * 1024 * 1024;
    static final int MAX_TEXT = 20_000;
    static final int MAX_HEADING = 500;
    static final int MAX_RUNS = 500;
    static final int MAX_ITEMS = 500;
    static final int MAX_LIST_LEVEL = 4;
    static final int MAX_ROWS = 200;
    static final int MAX_CELLS = 30;
    static final int MAX_ALT = 500;
    static final int MAX_ISSUES = 100;
    static final int COMPACT_MAX_CHARS = 60_000;
    static final Set<String> HEADING_STYLES = Set.of("TITLE", "SUBTITLE", "H1", "H2", "H3", "H4", "H5", "H6");
    /** Danh sách (không phải Set.of, thứ tự lặp của Set.of đổi theo mỗi lần chạy JVM): khóa của run luôn ra cùng thứ tự. */
    private static final List<String> RUN_FLAGS = List.of("bold", "italic", "underline", "strike", "code");
    /** Màu chữ, màu tô nền của đoạn chữ: mã hex 6 chữ số. */
    private static final List<String> RUN_COLORS = List.of("color", "highlight");
    private static final java.util.regex.Pattern HEX = java.util.regex.Pattern.compile("^#[0-9a-fA-F]{6}$");
    /** Căn lề của tiêu đề, đoạn, danh sách. */
    static final Set<String> ALIGNS = Set.of("LEFT", "CENTER", "RIGHT", "JUSTIFY");

    private final ObjectMapper json;

    public DocumentModelService(ObjectMapper json) {
        this.json = json;
    }

    @Override
    public ValidationResult validateSkeleton(DocumentModel skeleton) {
        List<ValidationIssue> errors = structure(skeleton);
        List<ValidationIssue> warnings = new ArrayList<>();
        if (skeleton != null && skeleton.blocks() != null) {
            for (DocumentBlock block : skeleton.blocks()) {
                if (block != null && block.origin() == BlockOrigin.STUDENT) {
                    add(errors, "STUDENT_BLOCK_IN_SKELETON", block.id(), "Khung chỉ chứa block của người soạn");
                }
                if (block != null && PartCalculator.level(block) > 0
                        && block.content().path("text").asText("").isBlank()) {
                    add(warnings, "HEADING_EMPTY", block.id(), "Tiêu đề mục đang trống");
                }
            }
        }
        return new ValidationResult(List.copyOf(errors), List.copyOf(warnings));
    }

    /**
     * Bài làm (BR-U07-15, yêu cầu 2026-10-21, 2026-10-22): block của người soạn giữ nguyên, trừ sơ đồ và bảng nằm
     * trong phần (người làm vẽ, điền thẳng vào đó); người làm không thêm tiêu đề và chỉ viết trong các phần (tiêu đề
     * lá của cây).
     */
    @Override
    public ValidationResult validateForSave(DocumentModel skeleton, DocumentModel document) {
        List<ValidationIssue> errors = structure(document);
        if (errors.isEmpty()) {
            Set<UUID> inParts = blocksInParts(document);
            teacherBlocks(skeleton, document, inParts, errors);
            for (DocumentBlock block : document.blocks()) {
                if (block.origin() != BlockOrigin.STUDENT) {
                    continue;
                }
                if (block.type() == BlockType.HEADING) {
                    add(errors, "STUDENT_HEADING_NOT_ALLOWED", block.id(), "Người làm bài không thêm tiêu đề mục");
                } else if (!inParts.contains(block.id())) {
                    add(errors, "STUDENT_BLOCK_OUTSIDE_PART", block.id(), "Chỉ viết trong các phần của đề");
                }
            }
        }
        return new ValidationResult(List.copyOf(errors), List.of());
    }

    /**
     * Id các block nằm trong một phần: từ tiêu đề lá tới trước tiêu đề Tiêu đề 1–6 kế tiếp; phần chung (trước tiêu đề
     * đầu tiên) và đoạn dưới tiêu đề cha không thuộc phần nào. Khung không có Tiêu đề 1–6 thì cả bài là một phần.
     */
    static Set<UUID> blocksInParts(DocumentModel document) {
        Set<UUID> result = new HashSet<>();
        if (document == null || document.blocks() == null) {
            return result;
        }
        List<DocumentPart> parts = PartCalculator.compute(document);
        boolean whole = parts.size() == 1 && parts.get(0).headingBlockId() == null;
        Set<UUID> leaves = new HashSet<>();
        parts.forEach(part -> leaves.add(part.headingBlockId()));
        boolean inside = whole;
        for (DocumentBlock block : document.blocks()) {
            if (!whole && PartCalculator.level(block) > 0) {
                inside = leaves.contains(block.id());
            }
            if (inside && block.id() != null) {
                result.add(block.id());
            }
        }
        return result;
    }

    @Override
    public ValidationResult validateForSubmit(DocumentModel skeleton, DocumentModel document,
                                              Map<DiagramType, Integer> requiredDiagrams) {
        ValidationResult saved = validateForSave(skeleton, document);
        if (!saved.ok()) {
            return saved;
        }
        List<ValidationIssue> errors = new ArrayList<>();
        Map<DiagramType, Integer> counts = new EnumMap<>(DiagramType.class);
        Map<UUID, String> skeletonXml = new HashMap<>();
        Map<UUID, JsonNode> skeletonRows = new HashMap<>();
        if (skeleton != null && skeleton.blocks() != null) {
            skeleton.blocks().stream().filter(b -> b.type() == BlockType.DIAGRAM)
                    .forEach(b -> skeletonXml.put(b.id(), b.content().path("xml").asText("")));
            skeleton.blocks().stream().filter(b -> b.type() == BlockType.TABLE)
                    .forEach(b -> skeletonRows.put(b.id(), b.content().path("rows")));
        }
        Set<UUID> inParts = blocksInParts(document);
        boolean hasContent = false;
        for (DocumentBlock block : document.blocks()) {
            if (block.origin() == BlockOrigin.TEACHER && block.type() == BlockType.DIAGRAM
                    && inParts.contains(block.id())) {
                // Sơ đồ của người soạn trong phần: ô trống thì phải vẽ; vẽ khác bản của đề thì tính là bài làm.
                String xml = block.content().path("xml").asText("");
                String original = skeletonXml.getOrDefault(block.id(), "");
                boolean empty = DiagramXml.cellCount(xml) == 0;
                if (empty && DiagramXml.cellCount(original) == 0) {
                    add(errors, "DIAGRAM_EMPTY", block.id(), "Sơ đồ này cần được vẽ");
                } else if (!empty && !xml.equals(original)) {
                    hasContent = true;
                    counts.merge(DiagramType.valueOf(block.content().path("diagramType").asText()), 1, Integer::sum);
                }
                continue;
            }
            if (block.origin() == BlockOrigin.TEACHER && block.type() == BlockType.TABLE && inParts.contains(block.id())) {
                // Bảng của đề trong phần: điền khác đề thì tính là bài làm.
                hasContent |= !block.content().path("rows").equals(skeletonRows.get(block.id()));
                continue;
            }
            if (block.origin() != BlockOrigin.STUDENT) {
                continue;
            }
            if (block.type() == BlockType.DIAGRAM) {
                if (DiagramXml.cellCount(block.content().path("xml").asText("")) == 0) {
                    add(errors, "DIAGRAM_EMPTY", block.id(), "Sơ đồ đang trống");
                } else {
                    hasContent = true;
                    counts.merge(DiagramType.valueOf(block.content().path("diagramType").asText()), 1, Integer::sum);
                }
            } else if (block.type() == BlockType.IMAGE || hasText(block)) {
                hasContent = true;
            }
        }
        if (requiredDiagrams != null) {
            // Loại sơ đồ suy từ hình vẽ (yêu cầu 2026-10-22): sơ đồ không nhận ra loại (OTHER), sau khi đủ yêu cầu của
            // chính OTHER, được tính bù cho loại còn thiếu.
            Integer otherMinimum = requiredDiagrams.get(DiagramType.OTHER);
            int spare = Math.max(0, counts.getOrDefault(DiagramType.OTHER, 0) - (otherMinimum == null ? 0 : otherMinimum));
            for (DiagramType type : DiagramType.values()) {
                Integer minimum = requiredDiagrams.get(type);
                if (minimum == null) {
                    continue;
                }
                int actual = counts.getOrDefault(type, 0);
                if (type != DiagramType.OTHER && actual < minimum) {
                    int fill = Math.min(spare, minimum - actual);
                    spare -= fill;
                    actual += fill;
                }
                if (actual < minimum) {
                    add(errors, "REQUIRED_DIAGRAMS_MISSING", null,
                            "Cần ít nhất " + minimum + " sơ đồ " + type.name() + ", bài mới có " + actual);
                }
            }
        }
        if (!hasContent) {
            add(errors, "SUBMISSION_EMPTY", null, "Bài làm chưa có nội dung của bạn");
        }
        return new ValidationResult(List.copyOf(errors), List.of());
    }

    @Override
    public List<DocumentPart> computeParts(DocumentModel skeleton) {
        return PartCalculator.compute(skeleton);
    }

    @Override
    public String toPlainText(DocumentModel document) {
        if (document == null || document.blocks() == null) {
            return "";
        }
        boolean mixed = document.blocks().stream().anyMatch(b -> b.origin() == BlockOrigin.STUDENT);
        StringBuilder out = new StringBuilder();
        for (DocumentBlock block : document.blocks()) {
            String text = blockText(block);
            if (text.isBlank()) {
                continue;
            }
            if (mixed && block.origin() == BlockOrigin.TEACHER && block.type() != BlockType.HEADING) {
                text = "> " + text.replace("\n", "\n> ");
            }
            out.append(text).append("\n\n");
        }
        return out.toString().strip();
    }

    @Override
    public String compactDiagramXml(String xml) {
        return DiagramXml.compact(xml, COMPACT_MAX_CHARS);
    }

    @Override
    public DocumentModel sanitize(DocumentModel document) {
        if (document == null || document.blocks() == null) {
            return document;
        }
        List<DocumentBlock> blocks = new ArrayList<>(document.blocks().size());
        for (DocumentBlock block : document.blocks()) {
            if (block == null || block.type() == null || !(block.content() instanceof ObjectNode content)) {
                blocks.add(block);
                continue;
            }
            blocks.add(new DocumentBlock(block.id(), block.origin(), block.type(), clean(block.type(), content)));
        }
        return new DocumentModel(blocks);
    }

    /** Chữ của một block, dùng cho prompt AI, kiểm bài trống và DOCX. */
    String blockText(DocumentBlock block) {
        if (block == null || block.type() == null || block.content() == null) {
            return "";
        }
        JsonNode c = block.content();
        return switch (block.type()) {
            case HEADING -> {
                String style = c.path("style").asText("");
                String text = c.path("text").asText("");
                yield switch (style) {
                    case "TITLE" -> "Tiêu đề: " + text;
                    case "SUBTITLE" -> "Phụ đề: " + text;
                    default -> "#".repeat(Math.max(1, PartCalculator.level(block))) + " " + text;
                };
            }
            case PARAGRAPH -> runs(c.path("runs"));
            case LIST -> {
                StringBuilder list = new StringBuilder();
                int number = 1;
                for (JsonNode item : c.path("items")) {
                    list.append("  ".repeat(Math.max(0, item.path("level").asInt(0))))
                            .append(c.path("ordered").asBoolean(false) ? (number++) + ". " : "- ")
                            .append(runs(item.path("runs"))).append('\n');
                }
                yield list.toString().stripTrailing();
            }
            case TABLE -> {
                StringBuilder table = new StringBuilder();
                for (JsonNode row : c.path("rows")) {
                    List<String> cells = new ArrayList<>();
                    row.forEach(cell -> cells.add(runs(cell.path("runs"))));
                    table.append("| ").append(String.join(" | ", cells)).append(" |\n");
                }
                yield table.toString().stripTrailing();
            }
            case IMAGE -> "[Ảnh" + suffix(c.path("alt").asText("")) + "]";
            case DIAGRAM -> "[Sơ đồ " + c.path("diagramType").asText("OTHER") + suffix(c.path("alt").asText(""))
                    + "]\n" + compactDiagramXml(c.path("xml").asText(""));
        };
    }

    /** Block có chữ người dùng nhập (không tính nhãn loại block). */
    static boolean hasText(DocumentBlock block) {
        JsonNode c = block.content();
        return switch (block.type()) {
            case HEADING -> !c.path("text").asText("").isBlank();
            case PARAGRAPH -> !runs(c.path("runs")).isBlank();
            case LIST -> {
                for (JsonNode item : c.path("items")) {
                    if (!runs(item.path("runs")).isBlank()) {
                        yield true;
                    }
                }
                yield false;
            }
            case TABLE -> {
                for (JsonNode row : c.path("rows")) {
                    for (JsonNode cell : row) {
                        if (!runs(cell.path("runs")).isBlank()) {
                            yield true;
                        }
                    }
                }
                yield false;
            }
            case IMAGE, DIAGRAM -> false;
        };
    }

    private List<ValidationIssue> structure(DocumentModel document) {
        List<ValidationIssue> errors = new ArrayList<>();
        if (document == null || document.blocks() == null) {
            add(errors, "DOCUMENT_INVALID", null, "Tài liệu không hợp lệ");
            return errors;
        }
        if (document.blocks().size() > MAX_BLOCKS) {
            add(errors, "TOO_MANY_BLOCKS", null, "Tài liệu tối đa " + MAX_BLOCKS + " block");
            return errors;
        }
        Set<UUID> ids = new HashSet<>();
        int diagrams = 0;
        for (DocumentBlock block : document.blocks()) {
            if (block == null || block.id() == null || block.origin() == null || block.type() == null
                    || !(block.content() instanceof ObjectNode)) {
                add(errors, "BLOCK_INVALID", block == null ? null : block.id(), "Block không hợp lệ");
                continue;
            }
            if (!ids.add(block.id())) {
                add(errors, "BLOCK_ID_DUPLICATE", block.id(), "Hai block trùng id");
            }
            if (block.type() == BlockType.DIAGRAM) {
                diagrams++;
            }
            content(block, errors);
        }
        if (diagrams > MAX_DIAGRAMS) {
            add(errors, "TOO_MANY_DIAGRAMS", null, "Tài liệu tối đa " + MAX_DIAGRAMS + " sơ đồ");
        }
        if (errors.isEmpty() && size(document) > MAX_DOCUMENT_BYTES) {
            add(errors, "DOCUMENT_TOO_LARGE", null, "Tài liệu vượt 20 MB");
        }
        return errors;
    }

    private void content(DocumentBlock block, List<ValidationIssue> errors) {
        JsonNode c = block.content();
        UUID id = block.id();
        JsonNode align = c.get("align");
        if (align != null && !align.isNull() && !ALIGNS.contains(align.asText(""))) {
            add(errors, "BLOCK_INVALID", id, "Căn lề không hợp lệ");
        }
        switch (block.type()) {
            case HEADING -> {
                if (!HEADING_STYLES.contains(c.path("style").asText(""))) {
                    add(errors, "HEADING_STYLE_INVALID", id, "Kiểu tiêu đề không hợp lệ");
                }
                text(c.get("text"), MAX_HEADING, id, errors);
            }
            case PARAGRAPH -> runs(c.get("runs"), id, errors);
            case LIST -> {
                flag(c.get("ordered"), id, errors);
                JsonNode items = c.get("items");
                if (items == null || !items.isArray() || items.size() > MAX_ITEMS) {
                    add(errors, "LIST_INVALID", id, "Danh sách không hợp lệ");
                    return;
                }
                for (JsonNode item : items) {
                    JsonNode level = item.get("level");
                    if (!item.isObject() || (level != null && (!level.canConvertToInt() || level.asInt() < 0
                            || level.asInt() > MAX_LIST_LEVEL))) {
                        add(errors, "LIST_INVALID", id, "Mục danh sách không hợp lệ");
                        return;
                    }
                    runs(item.get("runs"), id, errors);
                }
            }
            case TABLE -> {
                flag(c.get("headerRow"), id, errors);
                JsonNode rows = c.get("rows");
                if (rows == null || !rows.isArray() || rows.isEmpty() || rows.size() > MAX_ROWS) {
                    add(errors, "TABLE_INVALID", id, "Bảng không hợp lệ");
                    return;
                }
                for (JsonNode row : rows) {
                    if (!row.isArray() || row.isEmpty() || row.size() > MAX_CELLS) {
                        add(errors, "TABLE_INVALID", id, "Hàng của bảng không hợp lệ");
                        return;
                    }
                    for (JsonNode cell : row) {
                        if (!cell.isObject()) {
                            add(errors, "TABLE_INVALID", id, "Ô của bảng không hợp lệ");
                            return;
                        }
                        runs(cell.get("runs"), id, errors);
                        flag(cell.get("added"), id, errors);
                    }
                }
            }
            case IMAGE -> {
                boolean file = c.path(DocumentFiles.FILE_ID).isTextual() && !c.path(DocumentFiles.FILE_ID).asText().isBlank();
                boolean ref = c.path(DocumentFiles.FILE_REF).isTextual() && !c.path(DocumentFiles.FILE_REF).asText().isBlank();
                if (!file && !ref) {
                    add(errors, "IMAGE_FILE_MISSING", id, "Ảnh chưa được tải lên");
                }
                text(c.get("alt"), MAX_ALT, id, errors);
                JsonNode width = c.get("width");
                if (width != null && !width.isNull() && (!width.canConvertToInt() || width.asInt() < 16
                        || width.asInt() > 2000)) {
                    add(errors, "IMAGE_INVALID", id, "Độ rộng ảnh không hợp lệ");
                }
            }
            case DIAGRAM -> {
                try {
                    DiagramType.valueOf(c.path("diagramType").asText(""));
                } catch (IllegalArgumentException e) {
                    add(errors, "DIAGRAM_TYPE_INVALID", id, "Loại sơ đồ không hợp lệ");
                }
                DiagramXml.validate(c.path("xml").isTextual() ? c.path("xml").asText() : null)
                        .ifPresent(code -> add(errors, code, id, code.equals("DIAGRAM_TOO_LARGE")
                                ? "XML sơ đồ vượt 2 MB" : "XML sơ đồ không hợp lệ"));
                JsonNode svg = c.get("svgPreview");
                if (svg != null && !svg.isNull() && !svg.isTextual()) {
                    add(errors, "DIAGRAM_PREVIEW_INVALID", id, "Ảnh xem trước của sơ đồ không hợp lệ");
                }
                text(c.get("alt"), MAX_ALT, id, errors);
            }
        }
    }

    /** Block của người soạn có đủ, không đổi, đúng thứ tự; không có block người soạn lạ (BR-U07-15). */
    private void teacherBlocks(DocumentModel skeleton, DocumentModel document, Set<UUID> inParts,
                               List<ValidationIssue> errors) {
        List<DocumentBlock> expected = skeleton == null || skeleton.blocks() == null ? List.of()
                : skeleton.blocks().stream().filter(b -> b.origin() == BlockOrigin.TEACHER).toList();
        Map<UUID, DocumentBlock> byId = new HashMap<>();
        expected.forEach(b -> byId.put(b.id(), b));
        List<UUID> seen = new ArrayList<>();
        for (DocumentBlock block : document.blocks()) {
            if (block.origin() != BlockOrigin.TEACHER) {
                continue;
            }
            DocumentBlock original = byId.get(block.id());
            if (original == null) {
                add(errors, "TEACHER_BLOCK_ADDED", block.id(), "Không thêm block của người soạn");
            } else if (original.type() != block.type() || !sameContent(original, block, inParts.contains(block.id()))) {
                add(errors, "TEACHER_BLOCK_CHANGED", block.id(), "Không sửa block của người soạn");
            } else {
                seen.add(block.id());
            }
        }
        List<UUID> order = expected.stream().map(DocumentBlock::id).filter(seen::contains).toList();
        for (DocumentBlock block : expected) {
            if (!seen.contains(block.id())) {
                add(errors, "TEACHER_BLOCK_MISSING", block.id(), "Không xóa block của người soạn");
            }
        }
        if (!order.equals(seen)) {
            add(errors, "TEACHER_BLOCK_MOVED", null, "Không đổi thứ tự block của người soạn");
        }
    }

    /**
     * So nội dung; ảnh xem trước của sơ đồ được làm sạch lại mỗi lần lưu nên không đưa vào so sánh. Block nằm trong
     * phần ({@code inPart}) thì người làm được vẽ lại sơ đồ (XML, loại suy từ hình vẽ; mô tả giữ nguyên) và điền bảng
     * ({@link #filledTable}) — yêu cầu 2026-10-21, 2026-10-22.
     */
    private static boolean sameContent(DocumentBlock original, DocumentBlock block, boolean inPart) {
        if (original.type() == BlockType.DIAGRAM && original.content() instanceof ObjectNode a
                && block.content() instanceof ObjectNode b) {
            ObjectNode left = a.deepCopy();
            ObjectNode right = b.deepCopy();
            left.remove("svgPreview");
            right.remove("svgPreview");
            if (inPart) {
                left.remove(List.of("xml", "diagramType"));
                right.remove(List.of("xml", "diagramType"));
            }
            return left.equals(right);
        }
        if (original.type() == BlockType.TABLE && inPart) {
            return filledTable(original.content(), block.content());
        }
        return original.content().equals(block.content());
    }

    /**
     * Bảng của đề được điền: sửa ô, thêm hàng, cột ở bất kỳ đâu; ô người học thêm mang cờ {@code added} (yêu cầu
     * 2026-10-22) và chỉ ô đó được xóa. Giữ hàng tiêu đề; các hàng còn ô của đề (ô không cờ) không ít hơn đề, mỗi hàng
     * đó còn đủ số ô của đề.
     */
    private static boolean filledTable(JsonNode original, JsonNode table) {
        if (original.path("headerRow").asBoolean(false) != table.path("headerRow").asBoolean(false)) {
            return false;
        }
        List<Integer> teacherCells = new ArrayList<>();
        for (JsonNode row : table.path("rows")) {
            int count = 0;
            for (JsonNode cell : row) {
                if (!cell.path("added").asBoolean(false)) {
                    count++;
                }
            }
            if (count > 0) {
                teacherCells.add(count);
            }
        }
        JsonNode before = original.path("rows");
        if (teacherCells.size() < before.size()) {
            return false;
        }
        for (int i = 0; i < before.size(); i++) {
            if (teacherCells.get(i) < before.get(i).size()) {
                return false;
            }
        }
        return true;
    }

    private ObjectNode clean(BlockType type, ObjectNode c) {
        ObjectNode out = JsonNodeFactory.instance.objectNode();
        switch (type) {
            case HEADING -> {
                copy(c, out, "style");
                copy(c, out, "text");
                copy(c, out, "align");
            }
            case PARAGRAPH -> {
                out.set("runs", cleanRuns(c.get("runs")));
                copy(c, out, "align");
            }
            case LIST -> {
                copy(c, out, "ordered");
                copy(c, out, "align");
                if (c.get("items") instanceof ArrayNode items) {
                    ArrayNode cleaned = out.putArray("items");
                    for (JsonNode item : items) {
                        ObjectNode next = cleaned.addObject();
                        copy(item, next, "level");
                        next.set("runs", cleanRuns(item.get("runs")));
                    }
                } else {
                    copy(c, out, "items");
                }
            }
            case TABLE -> {
                copy(c, out, "headerRow");
                if (c.get("rows") instanceof ArrayNode rows) {
                    ArrayNode cleaned = out.putArray("rows");
                    for (JsonNode row : rows) {
                        if (row instanceof ArrayNode cells) {
                            ArrayNode next = cleaned.addArray();
                            for (JsonNode cell : cells) {
                                ObjectNode cleanCell = next.addObject();
                                cleanCell.set("runs", cleanRuns(cell.get("runs")));
                                if (cell.path("added").asBoolean(false)) {
                                    cleanCell.put("added", true);
                                }
                            }
                        } else {
                            cleaned.add(row);
                        }
                    }
                } else {
                    copy(c, out, "rows");
                }
            }
            case IMAGE -> {
                copy(c, out, DocumentFiles.FILE_ID);
                copy(c, out, DocumentFiles.FILE_REF);
                copy(c, out, DocumentFiles.HAS_FILE);
                copy(c, out, "alt");
                copy(c, out, "width");
            }
            case DIAGRAM -> {
                // Loại sơ đồ suy từ hình vẽ (yêu cầu 2026-10-22, chèn sơ đồ không chọn loại); chưa vẽ hoặc không nhận ra
                // thì giữ loại gửi lên.
                DiagramType detected = c.path("xml").isTextual() ? DiagramXml.detectType(c.path("xml").asText())
                        : DiagramType.OTHER;
                if (detected == DiagramType.OTHER) {
                    copy(c, out, "diagramType");
                } else {
                    out.put("diagramType", detected.name());
                }
                copy(c, out, "xml");
                copy(c, out, "alt");
                JsonNode svg = c.get("svgPreview");
                if (svg != null && svg.isTextual()) {
                    SvgSanitizer.sanitize(svg.asText()).ifPresent(clean -> out.put("svgPreview", clean));
                }
            }
        }
        return out;
    }

    private static JsonNode cleanRuns(JsonNode runs) {
        if (!(runs instanceof ArrayNode array)) {
            return runs == null ? JsonNodeFactory.instance.arrayNode() : runs;
        }
        ArrayNode cleaned = JsonNodeFactory.instance.arrayNode();
        for (JsonNode run : array) {
            if (!run.isObject()) {
                cleaned.add(run);
                continue;
            }
            ObjectNode next = cleaned.addObject();
            copy(run, next, "text");
            for (String flag : RUN_FLAGS) {
                if (run.path(flag).asBoolean(false)) {
                    next.put(flag, true);
                }
            }
            for (String color : RUN_COLORS) {
                if (HEX.matcher(run.path(color).asText("")).matches()) {
                    next.put(color, run.path(color).asText());
                }
            }
        }
        return cleaned;
    }

    private static void copy(JsonNode from, ObjectNode to, String field) {
        JsonNode value = from.get(field);
        if (value != null && !value.isNull()) {
            to.set(field, value);
        }
    }

    private void runs(JsonNode runs, UUID id, List<ValidationIssue> errors) {
        if (runs == null || !runs.isArray() || runs.size() > MAX_RUNS) {
            add(errors, "TEXT_INVALID", id, "Đoạn chữ không hợp lệ");
            return;
        }
        for (JsonNode run : runs) {
            if (!run.isObject()) {
                add(errors, "TEXT_INVALID", id, "Đoạn chữ không hợp lệ");
                return;
            }
            text(run.get("text"), MAX_TEXT, id, errors);
            for (String flag : RUN_FLAGS) {
                flag(run.get(flag), id, errors);
            }
            for (String color : RUN_COLORS) {
                JsonNode value = run.get(color);
                if (value != null && !value.isNull() && !HEX.matcher(value.asText("")).matches()) {
                    add(errors, "TEXT_INVALID", id, "Màu chữ không hợp lệ");
                }
            }
        }
    }

    private static void text(JsonNode value, int max, UUID id, List<ValidationIssue> errors) {
        if (value == null || value.isNull()) {
            return;
        }
        if (!value.isTextual()) {
            add(errors, "TEXT_INVALID", id, "Nội dung chữ không hợp lệ");
        } else if (value.asText().length() > max) {
            add(errors, "TEXT_TOO_LONG", id, "Nội dung vượt " + max + " ký tự");
        }
    }

    private static void flag(JsonNode value, UUID id, List<ValidationIssue> errors) {
        if (value != null && !value.isNull() && !value.isBoolean()) {
            add(errors, "BLOCK_INVALID", id, "Giá trị bật tắt không hợp lệ");
        }
    }

    private static String runs(JsonNode runs) {
        StringBuilder text = new StringBuilder();
        runs.forEach(run -> text.append(run.path("text").asText("")));
        return text.toString();
    }

    private static String suffix(String alt) {
        return alt == null || alt.isBlank() ? "" : ": " + alt.strip();
    }

    private long size(DocumentModel document) {
        try {
            return json.writeValueAsBytes(document).length;
        } catch (JsonProcessingException e) {
            return Long.MAX_VALUE;
        }
    }

    private static void add(List<ValidationIssue> issues, String code, UUID blockId, String message) {
        if (issues.size() < MAX_ISSUES) {
            issues.add(new ValidationIssue(code, blockId, message));
        }
    }
}
