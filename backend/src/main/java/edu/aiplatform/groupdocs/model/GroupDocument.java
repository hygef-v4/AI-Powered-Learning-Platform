package edu.aiplatform.groupdocs.model;

import edu.aiplatform.assignments.contract.DocumentModel;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Tài liệu nhóm của một nhóm cho một bài nhóm (domain-entities U12 mục 3). Bản nộp cuối là ảnh chụp bất biến:
 * phần chung và nội dung đã đăng của các mục kèm tác giả (BR-U12-41).
 */
public record GroupDocument(UUID id, UUID groupId, UUID assignmentId, UUID classId, Status status,
                            DocumentModel sharedBlocks, boolean activityMonitoring, SubmitMode autoSubmitReason,
                            DocumentModel submittedDocument, List<SectionAuthor> sectionAuthors, UUID submittedBy,
                            SubmitMode submitMode, Instant submittedAt, boolean late, String receiptHash,
                            List<String> warnings, Instant lastChangedAt, long version, Instant createdAt) {

    public enum Status {
        IN_PROGRESS, CLOSED
    }

    public enum SubmitMode {
        MANUAL, AUTO_DEADLINE, AUTO_RETIRED
    }

    /** Tác giả và trạng thái một mục lúc nộp. */
    public record SectionAuthor(String partId, String title, UUID accountId, boolean done) {
    }

    public GroupDocument {
        sectionAuthors = sectionAuthors == null ? List.of() : List.copyOf(sectionAuthors);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public static GroupDocument create(UUID id, UUID groupId, UUID assignmentId, UUID classId, DocumentModel shared,
                                       boolean activityMonitoring, Instant now) {
        return new GroupDocument(id, groupId, assignmentId, classId, Status.IN_PROGRESS, shared, activityMonitoring, null,
                null, List.of(), null, null, null, false, null, List.of(), now, 0, now);
    }

    public boolean submitted() {
        return submittedAt != null;
    }

    /** Có sửa sau lần nộp cuối (hoặc chưa nộp lần nào) thì tự nộp mới chụp lại (P5). */
    public boolean changedSinceSubmit() {
        return submittedAt == null || lastChangedAt.isAfter(submittedAt);
    }

    public GroupDocument withSubmission(DocumentModel snapshot, List<SectionAuthor> authors, UUID by, SubmitMode mode,
                                        Instant at, boolean isLate, String hash, List<String> notes, Status next) {
        return new GroupDocument(id, groupId, assignmentId, classId, next, sharedBlocks, activityMonitoring, autoSubmitReason,
                snapshot, authors, by, mode, at, isLate, hash, notes, lastChangedAt, version, createdAt);
    }

    public GroupDocument withAutoSubmitReason(SubmitMode reason) {
        return new GroupDocument(id, groupId, assignmentId, classId, status, sharedBlocks, activityMonitoring, reason,
                submittedDocument, sectionAuthors, submittedBy, submitMode, submittedAt, late, receiptHash, warnings,
                lastChangedAt, version, createdAt);
    }

    public GroupDocument closed() {
        return new GroupDocument(id, groupId, assignmentId, classId, Status.CLOSED, sharedBlocks, activityMonitoring,
                autoSubmitReason, submittedDocument, sectionAuthors, submittedBy, submitMode, submittedAt, late,
                receiptHash, warnings, lastChangedAt, version, createdAt);
    }
}
