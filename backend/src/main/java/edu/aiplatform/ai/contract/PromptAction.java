package edu.aiplatform.ai.contract;

/** Thao tác AI mà người dùng yêu cầu; mỗi thao tác chỉ thuộc một {@link PromptContext}. */
public enum PromptAction {
    CREATE_DRAFT(PromptContext.EXAM_EDITING, false),
    REVISE_DRAFT(PromptContext.EXAM_EDITING, true),
    GENERATE_QUESTIONS(PromptContext.EXAM_EDITING, true),
    ADJUST_RUBRIC(PromptContext.GRADING_RUBRIC, true),
    EXPLAIN_FEEDBACK(PromptContext.STUDENT_FEEDBACK, true),
    ASK(PromptContext.GENERAL, true);

    private final PromptContext context;
    private final boolean textRequired;

    PromptAction(PromptContext context, boolean textRequired) {
        this.context = context;
        this.textRequired = textRequired;
    }

    public PromptContext context() {
        return context;
    }

    /** {@code false} khi để trống thì AI tự quyết (ví dụ soạn đề mới không kèm yêu cầu). */
    public boolean isTextRequired() {
        return textRequired;
    }
}
