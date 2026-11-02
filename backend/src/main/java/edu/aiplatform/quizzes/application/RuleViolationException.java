package edu.aiplatform.quizzes.application;

import edu.aiplatform.quizzes.domain.Violation;
import edu.aiplatform.shared.error.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;

/** Lỗi theo quy tắc nghiệp vụ kèm danh sách lỗi theo trường; API trả thêm thuộc tính {@code violations}. */
public class RuleViolationException extends ApiException {

    private final List<Violation> violations;

    public RuleViolationException(HttpStatus status, String code, String message, List<Violation> violations) {
        super(status, code, message);
        this.violations = List.copyOf(violations);
    }

    public List<Violation> violations() {
        return violations;
    }
}
