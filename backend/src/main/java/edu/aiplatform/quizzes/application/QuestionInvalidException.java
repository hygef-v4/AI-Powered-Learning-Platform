package edu.aiplatform.quizzes.application;

import edu.aiplatform.quizzes.domain.Violation;
import java.util.List;
import org.springframework.http.HttpStatus;

/** Nội dung câu sai quy tắc (BR-U10-02, 03): {@code 400 QUESTION_INVALID} kèm danh sách lỗi theo trường. */
public class QuestionInvalidException extends RuleViolationException {

    public QuestionInvalidException(List<Violation> violations) {
        super(HttpStatus.BAD_REQUEST, "QUESTION_INVALID", "Nội dung câu hỏi chưa hợp lệ", violations);
    }
}
