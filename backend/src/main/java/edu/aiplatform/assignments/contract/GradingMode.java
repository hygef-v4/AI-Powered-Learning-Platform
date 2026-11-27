package edu.aiplatform.assignments.contract;

/**
 * Chế độ chấm của bài. {@code TEMPLATE}: đề mẫu của môn do Chủ nhiệm môn soạn (yêu cầu 2026-11-26), không có loại,
 * không giao cho sinh viên; giảng viên chọn loại khi tạo bài ở lớp từ đề mẫu.
 */
public enum GradingMode {
    GRADED, PRACTICE, TEMPLATE;

    /** Bài có điểm và rubric: bài chấm điểm và đề mẫu (đề mẫu giữ thang điểm để giảng viên dùng lại). */
    public boolean hasPoints() {
        return this != PRACTICE;
    }
}
