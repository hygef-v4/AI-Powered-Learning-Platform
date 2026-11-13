package edu.aiplatform.quizzes.contract;

import java.util.UUID;

/**
 * U10 khai báo, unit khác cài; chạy trong cùng transaction của U10, lỗi thì rollback cả. U11: tự nộp lượt đang làm khi
 * ngưng Quiz hoặc phát hành version mới. U13: gỡ Quiz khỏi module trước khi Quiz nháp bị xóa.
 */
public interface IQuizLifecycleListener {

    void onRetired(UUID quizId);

    /** Ngay trước khi xóa vĩnh viễn Quiz nháp chưa phát hành: dọn dữ liệu đang tham chiếu Quiz. */
    default void onDeleting(UUID quizId) {
    }
}
