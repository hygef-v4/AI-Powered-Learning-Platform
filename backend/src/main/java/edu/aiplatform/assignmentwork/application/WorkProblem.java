package edu.aiplatform.assignmentwork.application;

import edu.aiplatform.assignments.contract.ValidationIssue;
import edu.aiplatform.shared.error.ApiException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;

/** Lỗi nghiệp vụ U08 kèm {@code issues} (ví dụ lỗi kiểm khi nộp) hoặc dữ liệu thêm. */
public class WorkProblem extends ApiException {

    private final Map<String, Object> properties;

    public WorkProblem(HttpStatus status, String code, String message, Map<String, Object> properties) {
        super(status, code, message);
        this.properties = Map.copyOf(properties);
    }

    public static WorkProblem issues(HttpStatus status, String code, String message, List<ValidationIssue> issues) {
        return new WorkProblem(status, code, message, Map.of("issues", List.copyOf(issues)));
    }

    public Map<String, Object> properties() {
        return properties;
    }
}
