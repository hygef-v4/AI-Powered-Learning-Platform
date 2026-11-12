package edu.aiplatform.ai.contract;

/**
 * Phạm vi nghiệp vụ của một điểm nhận prompt tự do. Mỗi phạm vi có giới hạn độ dài, vai trò được dùng và nhóm ý định
 * riêng do guardrail quyết định; bên gọi chỉ chọn phạm vi, không tự đặt chính sách.
 */
public enum PromptContext {
    /** Soạn/sửa đề và câu hỏi: dịch đề, đổi ngữ cảnh, thêm/bớt yêu cầu, đổi độ khó. */
    EXAM_EDITING,
    /** Chỉnh barem: phân phối điểm, tiêu chí đánh giá bài làm. */
    GRADING_RUBRIC,
    /** Góp ý cho sinh viên: giải thích lỗi sai, gợi ý hướng tiếp cận; không giải hộ toàn bộ bài. */
    STUDENT_FEEDBACK,
    /** Tác vụ khác chưa có phạm vi riêng. */
    GENERAL
}
