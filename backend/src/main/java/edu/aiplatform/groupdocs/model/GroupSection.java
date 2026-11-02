package edu.aiplatform.groupdocs.model;

import edu.aiplatform.assignments.contract.DocumentBlock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Mục của tài liệu nhóm, khóa theo {@code partId} (BR-U12-30). Chỉ người giữ thấy {@code draftBlocks}; nội dung đã
 * đăng ({@code publishedBlocks}) là bản mọi người thấy và vào bản nộp. {@code lockedBy}: trưởng nhóm đã chốt mục
 * (BR-U12-39), chỉ khi mục {@code DONE}.
 */
public record GroupSection(UUID id, UUID documentId, String partId, UUID headingBlockId, List<UUID> ancestorHeadingIds,
                           int orderNo, String title, Status status, UUID claimedBy, Instant claimedAt, UUID assignedBy,
                           List<DocumentBlock> publishedBlocks, List<DocumentBlock> draftBlocks, Instant draftSavedAt,
                           UUID lastAuthorId, long version, UUID lockedBy, Instant lockedAt) {

    public enum Status {
        OPEN, CLAIMED, DONE
    }

    public GroupSection {
        ancestorHeadingIds = ancestorHeadingIds == null ? List.of() : List.copyOf(ancestorHeadingIds);
    }

    /** Mục chưa chốt. */
    public GroupSection(UUID id, UUID documentId, String partId, UUID headingBlockId, List<UUID> ancestorHeadingIds,
                        int orderNo, String title, Status status, UUID claimedBy, Instant claimedAt, UUID assignedBy,
                        List<DocumentBlock> publishedBlocks, List<DocumentBlock> draftBlocks, Instant draftSavedAt,
                        UUID lastAuthorId, long version) {
        this(id, documentId, partId, headingBlockId, ancestorHeadingIds, orderNo, title, status, claimedBy, claimedAt,
                assignedBy, publishedBlocks, draftBlocks, draftSavedAt, lastAuthorId, version, null, null);
    }

    public boolean locked() {
        return lockedBy != null;
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
                holder, now, by, publishedBlocks, publishedBlocks, null, lastAuthorId, version, lockedBy, lockedAt);
    }

    /** Nhả: bỏ bản nháp; về {@code DONE} nếu đã có nội dung đăng, không thì {@code OPEN} (BR-U12-36). */
    public GroupSection released() {
        return new GroupSection(id, documentId, partId, headingBlockId, ancestorHeadingIds, orderNo, title,
                hasPublished() ? Status.DONE : Status.OPEN, null, null, null, publishedBlocks, null, null, lastAuthorId,
                version, lockedBy, lockedAt);
    }

    public GroupSection withDraft(List<DocumentBlock> blocks, Instant now) {
        return new GroupSection(id, documentId, partId, headingBlockId, ancestorHeadingIds, orderNo, title, status,
                claimedBy, claimedAt, assignedBy, publishedBlocks, blocks, now, lastAuthorId, version, lockedBy, lockedAt);
    }

    /** Xong: bản nháp thành nội dung đã đăng, bỏ khóa (BR-U12-35). */
    public GroupSection done(Instant now) {
        List<DocumentBlock> content = draftBlocks == null ? publishedBlocks : draftBlocks;
        return new GroupSection(id, documentId, partId, headingBlockId, ancestorHeadingIds, orderNo, title, Status.DONE,
                null, null, null, content, null, now, claimedBy, version, lockedBy, lockedAt);
    }

    /**
     * Tiếp quản (BR-U12-33): người mới giữ mục, bản nháp đã lưu của người trước được giữ nguyên để làm tiếp; chưa có
     * bản nháp thì bắt đầu từ nội dung đã đăng.
     */
    public GroupSection takenOverBy(UUID holder, Instant now) {
        List<DocumentBlock> draft = draftBlocks != null ? draftBlocks : publishedBlocks;
        return new GroupSection(id, documentId, partId, headingBlockId, ancestorHeadingIds, orderNo, title, Status.CLAIMED,
                holder, now, holder, publishedBlocks, draft, draftSavedAt, lastAuthorId, version, lockedBy, lockedAt);
    }

    /** Nhập file của trưởng nhóm (BR-U12-46): nội dung thành bản đã đăng, mục {@code DONE}. */
    public GroupSection published(List<DocumentBlock> blocks, UUID author, Instant now) {
        return new GroupSection(id, documentId, partId, headingBlockId, ancestorHeadingIds, orderNo, title, Status.DONE,
                null, null, null, blocks, null, now, author, version, lockedBy, lockedAt);
    }

    /** Trưởng nhóm chốt mục đã Xong (BR-U12-39). */
    public GroupSection lockedBy(UUID leader, Instant now) {
        return new GroupSection(id, documentId, partId, headingBlockId, ancestorHeadingIds, orderNo, title, status,
                claimedBy, claimedAt, assignedBy, publishedBlocks, draftBlocks, draftSavedAt, lastAuthorId, version, leader,
                now);
    }

    public GroupSection unlocked() {
        return new GroupSection(id, documentId, partId, headingBlockId, ancestorHeadingIds, orderNo, title, status,
                claimedBy, claimedAt, assignedBy, publishedBlocks, draftBlocks, draftSavedAt, lastAuthorId, version, null,
                null);
    }
}
