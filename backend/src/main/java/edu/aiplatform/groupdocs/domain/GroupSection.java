package edu.aiplatform.groupdocs.domain;

import edu.aiplatform.assignments.contract.DocumentBlock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Mục của tài liệu nhóm, khóa theo {@code partId} (BR-U12-30). Chỉ người giữ thấy {@code draftBlocks}; nội dung đã
 * đăng ({@code publishedBlocks}) là bản mọi người thấy và vào bản nộp.
 */
public record GroupSection(UUID id, UUID documentId, String partId, UUID headingBlockId, List<UUID> ancestorHeadingIds,
                           int orderNo, String title, Status status, UUID claimedBy, Instant claimedAt, UUID assignedBy,
                           List<DocumentBlock> publishedBlocks, List<DocumentBlock> draftBlocks, Instant draftSavedAt,
                           UUID lastAuthorId, long version) {

    public enum Status {
        OPEN, CLAIMED, DONE
    }

    public GroupSection {
        ancestorHeadingIds = ancestorHeadingIds == null ? List.of() : List.copyOf(ancestorHeadingIds);
    }

    public boolean hasPublished() {
        return publishedBlocks != null && !publishedBlocks.isEmpty();
    }

    public boolean hasContent() {
        return hasPublished() || (draftBlocks != null && !draftBlocks.isEmpty());
    }

    /** Nhận hoặc được giao: {@code CLAIMED} cho người giữ; bản nháp bắt đầu từ nội dung đã đăng. */
    public GroupSection claimedBy(UUID holder, UUID by, Instant now) {
        return new GroupSection(id, documentId, partId, headingBlockId, ancestorHeadingIds, orderNo, title, Status.CLAIMED,
                holder, now, by, publishedBlocks, publishedBlocks, null, lastAuthorId, version);
    }

    /** Nhả: bỏ bản nháp; về {@code DONE} nếu đã có nội dung đăng, không thì {@code OPEN} (BR-U12-36). */
    public GroupSection released() {
        return new GroupSection(id, documentId, partId, headingBlockId, ancestorHeadingIds, orderNo, title,
                hasPublished() ? Status.DONE : Status.OPEN, null, null, null, publishedBlocks, null, null, lastAuthorId,
                version);
    }

    public GroupSection withDraft(List<DocumentBlock> blocks, Instant now) {
        return new GroupSection(id, documentId, partId, headingBlockId, ancestorHeadingIds, orderNo, title, status,
                claimedBy, claimedAt, assignedBy, publishedBlocks, blocks, now, lastAuthorId, version);
    }

    /** Xong: bản nháp thành nội dung đã đăng, bỏ khóa (BR-U12-35). */
    public GroupSection done(Instant now) {
        List<DocumentBlock> content = draftBlocks == null ? publishedBlocks : draftBlocks;
        return new GroupSection(id, documentId, partId, headingBlockId, ancestorHeadingIds, orderNo, title, Status.DONE,
                null, null, null, content, null, now, claimedBy, version);
    }
}
