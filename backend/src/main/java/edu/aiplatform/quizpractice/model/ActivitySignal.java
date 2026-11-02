package edu.aiplatform.quizpractice.model;

/**
 * Chất lượng tín hiệu rời trang của một lượt (BR-U11-32). {@code NO_SIGNAL}: bật theo dõi mà không nhận sự kiện nào;
 * {@code SIGNAL_LOST}: có khoảng thiếu sự kiện hoặc vẫn lưu câu trả lời sau khi sự kiện đã ngừng; {@code UNAVAILABLE}:
 * không đọc được dữ liệu lúc nộp. Chỉ là cờ để giảng viên xem, không phải bằng chứng và không tự trừ điểm.
 */
public enum ActivitySignal {
    DISABLED, OK, NO_SIGNAL, SIGNAL_LOST, UNAVAILABLE
}
