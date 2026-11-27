package edu.aiplatform.assignments.contract;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;

/**
 * Ảnh PNG của sơ đồ do trình duyệt vẽ lúc bấm tải DOCX (yêu cầu 2026-11-27): sơ đồ lưu trước khi có {@code pngPreview}
 * chỉ có XML nên máy chủ không vẽ được; trình duyệt dựng PNG bằng Draw.io rồi gửi kèm yêu cầu tải. Chỉ điền vào bản sao
 * dùng để xuất, không lưu; ảnh không phải PNG hợp lệ bị bộ xuất bỏ qua.
 */
public final class DiagramPngs {

    private static final String PNG_DATA_URI = "data:image/png;base64,";
    /** Mỗi ảnh tối đa 2 MB (base64 khoảng 2,7 MB); tối đa 50 sơ đồ một lần tải. */
    private static final int MAX_DATA_URI_CHARS = 2_800_000;
    private static final int MAX_DIAGRAMS = 50;

    private DiagramPngs() {
    }

    /** Thân yêu cầu tải DOCX kèm ảnh sơ đồ: blockId → data URI PNG. */
    public record Body(Map<String, String> diagramPngs) {

        /** Thân trống (yêu cầu GET cũ hay POST không có ảnh) thì không có ảnh nào. */
        public static Map<String, String> read(Body body) {
            return body == null || body.diagramPngs() == null ? Map.of() : body.diagramPngs();
        }
    }

    /** Bản sao của {@code document} với PNG điền vào các sơ đồ chưa có ảnh; không có ảnh gửi kèm thì trả nguyên tài liệu. */
    public static DocumentModel fillMissing(DocumentModel document, Map<String, String> pngs) {
        if (document == null || document.blocks() == null || pngs == null || pngs.isEmpty() || pngs.size() > MAX_DIAGRAMS) {
            return document;
        }
        List<DocumentBlock> blocks = document.blocks().stream().map(block -> {
            if (block == null || block.type() != BlockType.DIAGRAM || !(block.content() instanceof ObjectNode content)
                    || content.hasNonNull("pngPreview")) {
                return block;
            }
            String png = pngs.get(String.valueOf(block.id()));
            if (png == null || !png.startsWith(PNG_DATA_URI) || png.length() > MAX_DATA_URI_CHARS) {
                return block;
            }
            ObjectNode filled = content.deepCopy();
            filled.put("pngPreview", png);
            return new DocumentBlock(block.id(), block.origin(), block.type(), filled);
        }).toList();
        return new DocumentModel(blocks, document.title());
    }
}
