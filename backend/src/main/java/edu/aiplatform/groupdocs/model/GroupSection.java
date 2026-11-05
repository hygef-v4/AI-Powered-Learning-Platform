package edu.aiplatform.groupdocs.model;

import edu.aiplatform.assignments.contract.DocumentBlock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Mục của tài liệu nhóm, khóa theo {@code partId} (BR-U12-30). Chỉ người giữ thấy {@code draftBlocks}; nội dung đã
 * đăng ({@code publishedBlocks}) là bản mọi người thấy và vào bản nộp. {@code assigneeId}: người phụ trách mục, nhận
 * hoặc được giao trong giai đoạn phân công (FB-U12-01); sau khi chốt phân công chỉ người này soạn mục.
 */
public record GroupSection(UUID id, UUID documentId, String partId, UUID headingBlockId, List<UUID> ancestorHeadingIds,
                           int orderNo, String title, Status status, UUID claimedBy, Instant claimedAt, UUID assignedBy,
                           List<DocumentBlock> publishedBlocks, List<DocumentBlock> draftBlocks, Instant draftSavedAt,
                           UUID lastAuthorId, long version, UUID assigneeId) {

    public enum Status {
        OPEN, CLAIMED, DONE
    }

    public GroupSection {
        ancestorHeadingIds = ancestorHeadingIds == null ? List.of() : List.copyOf(ancestorHeadingIds);
    }

    /** Mục chưa có người phụ trách. */
    public GroupSection(UUID id, UUID documentId, String partId, UUID headingBlockId, List<UUID> ancestorHeadingIds,
                        int orderNo, String title, Status status, UUID claimedBy, Instant claimedAt, UUID assignedBy,
                        List<DocumentBlock> publishedBlocks, List<DocumentBlock> draftBlocks, Instant draftSavedAt,
                        UUID lastAuthorId, long version) {
        this(id, documentId, partId, headingBlockId, ancestorHeadingIds, orderNo, title, status, claimedBy, claimedAt,
                assignedBy, publishedBlocks, draftBlocks, draftSavedAt, lastAuthorId, version, null);
    }

    public boolean assignedTo(UUID accountId) {
        return accountId != null && accountId.equals(assigneeId);
    }

    public boolean hasPublished() {
        return publishedBlocks != null && !publishedBlocks.isEmpty();
    }

    public boolean hasContent() {
        return hasPublished() || (draftBlocks != null && !draftBlocks.isEmpty());
    }

    /** Bắt đầu soạn: {@code CLAIMED} cho người giữ; bản nháp bắt đầu từ nội dung đã đăng. */
    public GroupSection claimedBy(UUID holder, UUID by, Instant now) {
        return new GroupSection(id, documentId, partId, headingBlockId, ancestorHeadingIds, orderNo, title, Status.CLAIMED,
                holder, now, by, publishedBlocks, publishedBlocks, null, lastAuthorId, version, assigneeId);
    }

    /** Nhả: bỏ bản nháp; về {@code DONE} nếu đã có nội dung đăng, không thì {@code OPEN} (BR-U12-36). */
    public GroupSection released() {
        return new GroupSection(id, documentId, partId, headingBlockId, ancestorHeadingIds, orderNo, title,
                hasPublished() ? Status.DONE : Status.OPEN, null, null, null, publishedBlocks, null, null, lastAuthorId,
                version, assigneeId);
    }

    public GroupSection withDraft(List<DocumentBlock> blocks, Instant now) {
        return new GroupSection(id, documentId, partId, headingBlockId, ancestorHeadingIds, orderNo, title, status,
                claimedBy, claimedAt, assignedBy, publishedBlocks, blocks, now, lastAuthorId, version, assigneeId);
    }

    /** Xong: bản nháp thành nội dung đã đăng, bỏ khóa (BR-U12-35). */
    public GroupSection done(Instant now) {
        List<DocumentBlock> content = draftBlocks == null ? publishedBlocks : draftBlocks;
        return new GroupSection(id, documentId, partId, headingBlockId, ancestorHeadingIds, orderNo, title, Status.DONE,
                null, null, null, content, null, now, claimedBy, version, assigneeId);
    }

    /** Nhập file của trưởng nhóm (FB-U12-03): nội dung thành bản đã đăng, mục {@code DONE}. */
    public GroupSection published(List<DocumentBlock> blocks, UUID author, Instant now) {
        return new GroupSection(id, documentId, partId, headingBlockId, ancestorHeadingIds, orderNo, title, Status.DONE,
                null, null, null, blocks, null, now, author, version, assigneeId);
    }

    /** Đặt hoặc bỏ người phụ trách trong giai đoạn phân công (FB-U12-01). */
    public GroupSection withAssignee(UUID assignee) {
        return new GroupSection(id, documentId, partId, headingBlockId, ancestorHeadingIds, orderNo, title, status,
                claimedBy, claimedAt, assignedBy, publishedBlocks, draftBlocks, draftSavedAt, lastAuthorId, version, assignee);
    }
}
