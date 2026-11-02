package edu.aiplatform.quizzes.model;

import java.util.List;

/** Một câu đề xuất kèm lỗi theo BR-U10-02, 03; câu có lỗi vẫn giữ để người soạn sửa trước khi chấp nhận (H5). */
public record DraftQuestion(QuestionContent content, List<Violation> violations) {

    public DraftQuestion {
        violations = violations == null ? List.of() : List.copyOf(violations);
    }

    public static DraftQuestion of(QuestionContent content) {
        return new DraftQuestion(content, QuestionRules.validate(content));
    }
}
