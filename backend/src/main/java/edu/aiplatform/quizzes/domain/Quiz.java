package edu.aiplatform.quizzes.domain;

import edu.aiplatform.quizzes.contract.QuizScope;
import edu.aiplatform.quizzes.contract.QuizSettings;
import edu.aiplatform.quizzes.contract.QuizStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Một version của Quiz (UC 48). Trạng thái {@code DRAFT → REVIEWED → OPEN → RETIRED} (BR-U10-20); chỉ sửa khi
 * {@code DRAFT}, sửa Quiz {@code REVIEWED} đưa về {@code DRAFT}. Quiz chỉ có chế độ Practice (BR-U10-12).
 */
public record Quiz(UUID id, QuizScope scope, UUID classId, UUID subjectId, UUID sourceQuizId, QuizOrigin origin,
                   int versionNo, String title, String instructions, QuizSettings settings,
                   boolean activityMonitoringEnabled, String activityNotice, List<UUID> referenceMaterialIds,
                   QuizStatus status, Instant publishedAt, Instant retiredAt, String retiredReason, UUID createdBy,
                   Instant createdAt, Instant updatedAt, long version) {

    /** Mặc định F6: không trộn, không giới hạn giờ, 1 lượt, hiện điểm, không hiện đáp án; theo dõi tắt. */
    public static final QuizSettings DEFAULT_SETTINGS = new QuizSettings(false, false, null, 1, true, false);

    public Quiz {
        referenceMaterialIds = referenceMaterialIds == null ? List.of() : List.copyOf(referenceMaterialIds);
    }

    public static Quiz create(QuizScope scope, UUID classId, UUID subjectId, String title, UUID createdBy,
                              Instant now) {
        return new Quiz(UUID.randomUUID(), scope, classId, subjectId, null, QuizOrigin.MANUAL, 1, title, null,
                DEFAULT_SETTINGS, false, null, List.of(), QuizStatus.DRAFT, null, null, null, createdBy, now, now, 0);
    }

    public boolean isEditable() {
        return status == QuizStatus.DRAFT || status == QuizStatus.REVIEWED;
    }

    /** Quiz chưa từng phát hành thì xóa hẳn được (BR-U10-24). */
    public boolean isDeletable() {
        return isEditable() && publishedAt == null;
    }

    /** Mọi thay đổi nội dung: Quiz đã duyệt quay về nháp (BR-U10-20). */
    public Quiz edited(Instant now) {
        requireEditable();
        return with(QuizStatus.DRAFT, publishedAt, retiredAt, retiredReason, now);
    }

    public Quiz withDetails(String newTitle, String newInstructions, QuizSettings newSettings, boolean monitoring,
                            String notice, List<UUID> materials, Instant now) {
        requireEditable();
        return new Quiz(id, scope, classId, subjectId, sourceQuizId, origin, versionNo, newTitle, newInstructions,
                newSettings, monitoring, notice, materials, QuizStatus.DRAFT, publishedAt, retiredAt, retiredReason,
                createdBy, createdAt, now, version);
    }

    public Quiz reviewed(Instant now) {
        require(QuizStatus.DRAFT);
        return with(QuizStatus.REVIEWED, publishedAt, retiredAt, retiredReason, now);
    }

    public Quiz published(Instant now) {
        require(QuizStatus.REVIEWED);
        return with(QuizStatus.OPEN, now, retiredAt, retiredReason, now);
    }

    public Quiz retired(String reason, Instant now) {
        require(QuizStatus.OPEN);
        return with(QuizStatus.RETIRED, publishedAt, now, reason, now);
    }

    /**
     * Bản sao ở trạng thái nháp (BR-U10-31): giữ tiêu đề, hướng dẫn, cài đặt, theo dõi, tài liệu; không giữ lịch sử
     * phát hành. {@code targetClassId} khác lớp gốc khi copy giữa lớp.
     */
    public Quiz copyAs(QuizOrigin newOrigin, int newVersionNo, UUID targetClassId, UUID actorId, Instant now) {
        return new Quiz(UUID.randomUUID(), scope, targetClassId, subjectId, id, newOrigin, newVersionNo, title,
                instructions, settings, activityMonitoringEnabled, activityNotice, referenceMaterialIds,
                QuizStatus.DRAFT, null, null, null, actorId, now, now, 0);
    }

    private Quiz with(QuizStatus newStatus, Instant newPublishedAt, Instant newRetiredAt, String reason, Instant now) {
        return new Quiz(id, scope, classId, subjectId, sourceQuizId, origin, versionNo, title, instructions, settings,
                activityMonitoringEnabled, activityNotice, referenceMaterialIds, newStatus, newPublishedAt,
                newRetiredAt, reason, createdBy, createdAt, now, version);
    }

    private void requireEditable() {
        if (!isEditable()) {
            throw new IllegalStateException("Quiz đang " + status + " không sửa được");
        }
    }

    private void require(QuizStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Quiz đang " + status + ", cần " + expected);
        }
    }
}
