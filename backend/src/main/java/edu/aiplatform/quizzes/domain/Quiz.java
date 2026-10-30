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
    /** Quiz mới: không giới hạn thời gian và số lượt (người soạn tự đặt), hiện điểm sau khi nộp. */
    public static final QuizSettings DEFAULT_SETTINGS = new QuizSettings(false, false, null, null, true, false);

    public Quiz {
        referenceMaterialIds = referenceMaterialIds == null ? List.of() : List.copyOf(referenceMaterialIds);
    }

    public static Quiz create(QuizScope scope, UUID classId, UUID subjectId, String title, UUID createdBy,
                              Instant now) {
        return new Quiz(UUID.randomUUID(), scope, classId, subjectId, null, QuizOrigin.MANUAL, 1, title, null,
                DEFAULT_SETTINGS, false, null, List.of(), QuizStatus.DRAFT, null, null, null, createdBy, now, now, 0);
    }

    /** Quiz chưa lưu trữ sửa được tại chỗ, kể cả đang mở: không tạo phiên bản mới (yêu cầu 2026-10-29). */
    public boolean isEditable() {
        return status == QuizStatus.DRAFT || status == QuizStatus.REVIEWED || status == QuizStatus.OPEN;
    }

    /** Quiz chưa từng phát hành thì xóa hẳn được (BR-U10-24). */
    public boolean isDeletable() {
        return (status == QuizStatus.DRAFT || status == QuizStatus.REVIEWED) && publishedAt == null;
    }

    /** Mọi thay đổi nội dung: Quiz đã duyệt quay về nháp (BR-U10-20); Quiz đang mở vẫn mở. */
    public Quiz edited(Instant now) {
        requireEditable();
        return with(afterEdit(), publishedAt, retiredAt, retiredReason, now);
    }

    private QuizStatus afterEdit() {
        return status == QuizStatus.OPEN ? QuizStatus.OPEN : QuizStatus.DRAFT;
    }

    public Quiz withDetails(String newTitle, String newInstructions, QuizSettings newSettings, boolean monitoring,
                            String notice, List<UUID> materials, Instant now) {
        requireEditable();
        return new Quiz(id, scope, classId, subjectId, sourceQuizId, origin, versionNo, newTitle, newInstructions,
                newSettings, monitoring, notice, materials, afterEdit(), publishedAt, retiredAt, retiredReason,
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

    /** Lưu trữ Quiz chưa lưu trữ, giữ nguyên lịch sử phát hành (null nếu chưa từng phát hành). */
    public Quiz retired(String reason, Instant now) {
        if (status == QuizStatus.RETIRED) {
            throw new IllegalStateException("Quiz đã lưu trữ");
        }
        return with(QuizStatus.RETIRED, publishedAt, now, reason, now);
    }

    /**
     * Mở lại Quiz đã lưu trữ: Quiz có lịch sử phát hành mở lại cho sinh viên làm; Quiz chưa từng phát hành về nháp để
     * soạn tiếp. Hai sự kiện phát hành/lưu trữ có thể xảy ra cùng thời điểm.
     */
    public Quiz reopened(Instant now) {
        require(QuizStatus.RETIRED);
        boolean neverPublished = publishedAt == null;
        return with(neverPublished ? QuizStatus.DRAFT : QuizStatus.OPEN, publishedAt, null, null, now);
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
