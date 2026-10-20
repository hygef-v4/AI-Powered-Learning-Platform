package edu.aiplatform.quizzes.application;

import edu.aiplatform.quizzes.domain.Violation;
import edu.aiplatform.shared.error.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;

/** Nội dung câu sai quy tắc (BR-U10-02, 03): {@code 400 QUESTION_INVALID} kèm danh sách lỗi theo trường. */
public class QuestionInvalidException extends ApiException {

    private final List<Violation> violations;

    public QuestionInvalidException(List<Violation> violations) {
        super(HttpStatus.BAD_REQUEST, "QUESTION_INVALID", "Nội dung câu hỏi chưa hợp lệ");
        this.violations = List.copyOf(violations);
    }

    public List<Violation> violations() {
        return violations;
    }
}
