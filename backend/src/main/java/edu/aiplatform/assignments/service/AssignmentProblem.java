package edu.aiplatform.assignments.service;

import edu.aiplatform.assignments.contract.ValidationIssue;
import edu.aiplatform.shared.error.ApiException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;

/** Lỗi nghiệp vụ kèm danh sách chỗ cần sửa ({@code issues}) hoặc dữ liệu thêm cho frontend. */
public class AssignmentProblem extends ApiException {

    private final Map<String, Object> properties;

    public AssignmentProblem(HttpStatus status, String code, String message, Map<String, Object> properties) {
        super(status, code, message);
        this.properties = Map.copyOf(properties);
    }

    public static AssignmentProblem issues(HttpStatus status, String code, String message, List<ValidationIssue> issues) {
        return new AssignmentProblem(status, code, message, Map.of("issues", List.copyOf(issues)));
    }

    public Map<String, Object> properties() {
        return properties;
    }
}
