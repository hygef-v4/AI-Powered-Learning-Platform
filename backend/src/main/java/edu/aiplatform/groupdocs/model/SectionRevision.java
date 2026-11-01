package edu.aiplatform.groupdocs.model;

import edu.aiplatform.assignments.contract.DocumentBlock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Bản lịch sử bất biến của một mục (BR-U12-35): mỗi lần Xong, bản nháp của người trước khi bị tiếp quản và mỗi lần
 * trưởng nhóm nhập file.
 */
public record SectionRevision(UUID id, UUID sectionId, UUID authorId, List<DocumentBlock> blocks, Instant createdAt,
                              Kind kind) {

    public enum Kind {
        DONE, TAKEOVER, IMPORT
    }

    public SectionRevision(UUID id, UUID sectionId, UUID authorId, List<DocumentBlock> blocks, Instant createdAt) {
        this(id, sectionId, authorId, blocks, createdAt, Kind.DONE);
    }
}
