package edu.aiplatform.groupdocs.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.groupdocs.domain.GroupDocument.SectionAuthor;
import edu.aiplatform.groupdocs.domain.GroupSection;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ghép tài liệu nhóm (BR-U12-41): phần chung của khung, nội dung đã đăng của từng mục đặt sau các block khung của
 * phần đó (trước heading kế tiếp). Mục chưa có nội dung đăng không vào bản ghép.
 */
public final class GroupSnapshot {

    private GroupSnapshot() {
    }

    public record Result(DocumentModel document, List<SectionAuthor> authors, List<String> undone) {
    }

    public static Result build(DocumentModel shared, List<GroupSection> sections) {
        List<DocumentBlock> skeleton = shared == null || shared.blocks() == null ? List.of() : shared.blocks();
        List<GroupSection> ordered = sections.stream().sorted(Comparator.comparingInt(GroupSection::orderNo)).toList();
        Map<Integer, List<DocumentBlock>> inserts = new HashMap<>();
        List<DocumentBlock> tail = new ArrayList<>();
        List<SectionAuthor> authors = new ArrayList<>();
        List<String> undone = new ArrayList<>();
        for (GroupSection s : ordered) {
            boolean done = s.status() == GroupSection.Status.DONE && s.hasPublished();
            authors.add(new SectionAuthor(s.partId(), s.title(), done ? s.lastAuthorId() : null, done));
            if (!done) {
                undone.add(s.title());
                continue;
            }
            int at = insertionPoint(skeleton, s.headingBlockId());
            (at < 0 ? tail : inserts.computeIfAbsent(at, k -> new ArrayList<>())).addAll(s.publishedBlocks());
        }
        List<DocumentBlock> out = new ArrayList<>();
        for (int i = 0; i < skeleton.size(); i++) {
            out.addAll(inserts.getOrDefault(i, List.of()));
            out.add(skeleton.get(i));
        }
        out.addAll(inserts.getOrDefault(skeleton.size(), List.of()));
        out.addAll(tail);
        return new Result(new DocumentModel(out), authors, undone);
    }

    /**
     * Khung kèm nội dung của một mục đặt đúng chỗ của phần đó, để kiểm nháp như một bài hoàn chỉnh: bộ kiểm của U07
     * so block của người soạn trên cả tài liệu, nên không kiểm riêng nội dung mục được.
     */
    public static DocumentModel withSection(DocumentModel shared, UUID headingBlockId, List<DocumentBlock> content) {
        List<DocumentBlock> skeleton = shared == null || shared.blocks() == null ? List.of() : shared.blocks();
        List<DocumentBlock> out = new ArrayList<>(skeleton);
        int at = insertionPoint(skeleton, headingBlockId);
        out.addAll(at < 0 ? out.size() : at, content);
        return new DocumentModel(out);
    }

    /** Vị trí ngay trước heading kế tiếp sau heading của phần; không có heading của phần thì −1 (cuối tài liệu). */
    static int insertionPoint(List<DocumentBlock> skeleton, UUID headingId) {
        if (headingId == null) {
            return -1;
        }
        for (int i = 0; i < skeleton.size(); i++) {
            if (headingId.equals(skeleton.get(i).id())) {
                for (int j = i + 1; j < skeleton.size(); j++) {
                    if (skeleton.get(j).type() == BlockType.HEADING) {
                        return j;
                    }
                }
                return skeleton.size();
            }
        }
        return -1;
    }

    /** Mã biên nhận: SHA-256 của tài liệu, tác giả, mã tài liệu và thời điểm nộp (BR-U12-41). */
    public static String receiptHash(ObjectMapper json, UUID documentId, Result result, Instant at) {
        Map<String, Object> material = new LinkedHashMap<>();
        material.put("documentId", documentId.toString());
        material.put("submittedAt", at.toString());
        material.put("document", result.document());
        material.put("authors", result.authors());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(json.writeValueAsString(material)
                    .getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(digest);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Không tính được mã biên nhận", e);
        }
    }
}
