package edu.aiplatform.assignments.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Ảnh trong tài liệu (block {@code IMAGE}): bảng của unit sở hữu lưu {@code fileId} của U03, frontend không thấy
 * {@code fileId} mà mở ảnh theo id block; ảnh mới tải lên mang {@code fileRef} và được gắn khi lưu. Dùng bởi U07,
 * U08, U12.
 */
public final class DocumentFiles {

    public static final String FILE_ID = "fileId";
    public static final String FILE_REF = "fileRef";
    public static final String HAS_FILE = "hasFile";

    private DocumentFiles() {
    }

    /** Bản trả cho frontend: bỏ {@code fileId}, đánh dấu {@code hasFile}. */
    public static DocumentModel hideFiles(DocumentModel document) {
        if (document == null) {
            return new DocumentModel(List.of());
        }
        List<DocumentBlock> blocks = new ArrayList<>(document.blocks().size());
        for (DocumentBlock block : document.blocks()) {
            if (block.type() == BlockType.IMAGE && block.content() instanceof ObjectNode content) {
                ObjectNode copy = content.deepCopy();
                boolean hasFile = copy.hasNonNull(FILE_ID);
                copy.remove(FILE_ID);
                copy.remove(FILE_REF);
                copy.put(HAS_FILE, hasFile);
                blocks.add(new DocumentBlock(block.id(), block.origin(), block.type(), copy));
            } else {
                blocks.add(block);
            }
        }
        return new DocumentModel(List.copyOf(blocks));
    }

    public static Optional<String> fileId(DocumentModel document, UUID blockId) {
        if (document == null || blockId == null) {
            return Optional.empty();
        }
        return document.blocks().stream()
                .filter(b -> blockId.equals(b.id()) && b.type() == BlockType.IMAGE && b.content() != null)
                .map(b -> b.content().path(FILE_ID))
                .filter(JsonNode::isTextual)
                .map(JsonNode::asText)
                .findFirst();
    }

    public static Set<String> fileIds(DocumentModel document) {
        Set<String> ids = new LinkedHashSet<>();
        if (document != null) {
            for (DocumentBlock block : document.blocks()) {
                if (block.type() == BlockType.IMAGE && block.content() != null
                        && block.content().path(FILE_ID).isTextual()) {
                    ids.add(block.content().path(FILE_ID).asText());
                }
            }
        }
        return ids;
    }

    /**
     * Dựng tài liệu để lưu: ảnh có {@code fileRef} được gắn qua {@code attach} (trả {@code fileId}); ảnh không có
     * {@code fileRef} lấy lại {@code fileId} của block cùng id trong bản đã lưu {@code previous}. {@code fileId}
     * do client gửi luôn bị bỏ. Ảnh không gắn được và không có bản cũ giữ nguyên để bước kiểm báo lỗi.
     * {@code attach} rỗng thì giữ {@code fileRef} để kiểm trước, gắn sau.
     */
    public static DocumentModel resolveFiles(DocumentModel previous, DocumentModel incoming,
                                             Function<String, String> attach) {
        Map<UUID, String> known = new HashMap<>();
        if (previous != null) {
            for (DocumentBlock block : previous.blocks()) {
                fileId(previous, block.id()).ifPresent(id -> known.put(block.id(), id));
            }
        }
        List<DocumentBlock> blocks = new ArrayList<>(incoming.blocks().size());
        for (DocumentBlock block : incoming.blocks()) {
            if (block.type() == BlockType.IMAGE && block.content() instanceof ObjectNode content) {
                ObjectNode copy = content.deepCopy();
                copy.remove(FILE_ID);
                copy.remove(HAS_FILE);
                JsonNode ref = attach == null ? copy.get(FILE_REF) : copy.remove(FILE_REF);
                if (ref != null && ref.isTextual() && !ref.asText().isBlank()) {
                    if (attach != null) {
                        copy.put(FILE_ID, attach.apply(ref.asText()));
                    }
                } else if (known.containsKey(block.id())) {
                    copy.put(FILE_ID, known.get(block.id()));
                }
                blocks.add(new DocumentBlock(block.id(), block.origin(), block.type(), copy));
            } else {
                blocks.add(block);
            }
        }
        return new DocumentModel(List.copyOf(blocks));
    }
}
