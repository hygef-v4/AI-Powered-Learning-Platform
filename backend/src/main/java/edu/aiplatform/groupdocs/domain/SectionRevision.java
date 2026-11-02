package edu.aiplatform.groupdocs.domain;

import edu.aiplatform.assignments.contract.DocumentBlock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Bản lịch sử bất biến của một mục mỗi lần Xong (BR-U12-35). */
public record SectionRevision(UUID id, UUID sectionId, UUID authorId, List<DocumentBlock> blocks, Instant createdAt) {
}
