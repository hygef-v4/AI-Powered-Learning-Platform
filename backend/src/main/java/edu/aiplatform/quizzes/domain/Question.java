package edu.aiplatform.quizzes.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Một phiên bản câu (BR-U10-04). Bản {@code ACTIVE} không sửa được: sửa là mở bản {@code DRAFT} mới cùng
 * {@code lineageId} ({@link #nextDraft}); Quiz ghim id phiên bản nên bản cũ giữ nguyên (P1).
 */
public record Question(UUID id, UUID lineageId, int versionNo, QuestionScope scope, UUID subjectId, UUID quizId,
                       QuestionContent content, QuestionStatus status, Instant activatedAt, UUID createdBy,
                       Instant createdAt, Instant updatedAt, long version) {

    /** Câu ngân hàng mới: lineage mới, phiên bản 1, {@code DRAFT}. */
    public static Question newBankDraft(UUID subjectId, QuestionContent content, UUID createdBy, Instant now) {
        UUID id = UUID.randomUUID();
        return new Question(id, id, 1, QuestionScope.BANK, subjectId, null, content, QuestionStatus.DRAFT, null,
                createdBy, now, now, 0);
    }

    public boolean isDraft() {
        return status == QuestionStatus.DRAFT;
    }

    /** Xóa hẳn chỉ với bản nháp chưa từng kích hoạt (BR-U10-05); bản đã kích hoạt chỉ ngưng dùng. */
    public boolean isDeletable() {
        return status == QuestionStatus.DRAFT && activatedAt == null;
    }

    public Question withContent(QuestionContent newContent, Instant now) {
        requireStatus(QuestionStatus.DRAFT);
        return new Question(id, lineageId, versionNo, scope, subjectId, quizId, newContent, status, activatedAt,
                createdBy, createdAt, now, version);
    }

    public Question activate(Instant now) {
        requireStatus(QuestionStatus.DRAFT);
        return new Question(id, lineageId, versionNo, scope, subjectId, quizId, content, QuestionStatus.ACTIVE, now,
                createdBy, createdAt, now, version);
    }

    public Question retire(Instant now) {
        requireStatus(QuestionStatus.ACTIVE);
        return new Question(id, lineageId, versionNo, scope, subjectId, quizId, content, QuestionStatus.RETIRED,
                activatedAt, createdBy, createdAt, now, version);
    }

    /** Bản nháp kế tiếp cùng lineage, chép nội dung của bản này; {@code nextVersionNo} lấy từ phiên bản lớn nhất. */
    public Question nextDraft(int nextVersionNo, UUID actorId, Instant now) {
        if (status == QuestionStatus.DRAFT) {
            throw new IllegalStateException("Bản nháp không mở bản nháp mới");
        }
        return new Question(UUID.randomUUID(), lineageId, nextVersionNo, scope, subjectId, quizId, content,
                QuestionStatus.DRAFT, null, actorId, now, now, 0);
    }

    private void requireStatus(QuestionStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Câu đang " + status + ", cần " + expected);
        }
    }
}
