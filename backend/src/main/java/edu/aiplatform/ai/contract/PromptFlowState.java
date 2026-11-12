package edu.aiplatform.ai.contract;

/**
 * Trạng thái nghiệp vụ của đối tượng mà prompt tác động, do service gọi xác định từ dữ liệu phía server (không lấy từ
 * request). Guardrail dùng máy trạng thái để chỉ cho phép thao tác đúng quy trình.
 */
public enum PromptFlowState {
    /** Đề/quiz/ngân hàng câu hỏi đang soạn, còn sửa được. */
    AUTHORING_DRAFT,
    /** Đề đã phát hành, không sửa bằng AI. */
    AUTHORING_PUBLISHED,
    /** Barem còn chỉnh được (chưa khóa để chấm). */
    RUBRIC_EDITABLE,
    /** Barem đã khóa vì đã có bài chấm hoặc đã công bố điểm. */
    RUBRIC_LOCKED,
    /** Sinh viên đang làm bài: chưa được hỏi AI về bài này. */
    ATTEMPT_IN_PROGRESS,
    /** Bài đã nộp: được nhận góp ý. */
    ATTEMPT_SUBMITTED,
    /** Tác vụ không gắn với đối tượng có quy trình riêng. */
    OPEN
}
