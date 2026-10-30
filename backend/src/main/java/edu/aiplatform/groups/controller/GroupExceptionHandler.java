package edu.aiplatform.groups.controller;

import edu.aiplatform.groupdocs.service.GroupSubmissionService.SubmitInvalidException;
import edu.aiplatform.groupdocs.controller.GroupDocController;
import edu.aiplatform.groups.service.GroupSetInvalidException;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Lỗi có danh sách chi tiết của U12: bộ nhóm mâu thuẫn trả từng lỗi theo nhóm (BR-U12-04), tài liệu chưa đủ điều
 * kiện nộp trả danh sách cần sửa (BR-U12-40). Lỗi khác do {@code GlobalExceptionHandler} trả.
 */
@RestControllerAdvice(assignableTypes = {GroupController.class, GroupDocController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GroupExceptionHandler {

    @ExceptionHandler(GroupSetInvalidException.class)
    public ProblemDetail invalidSet(GroupSetInvalidException e) {
        ProblemDetail problem = problem(e.status(), e.code(), e.getMessage());
        problem.setProperty("issues", e.issues().stream()
                .map(i -> Map.of("groupKey", i.groupKey(), "code", i.code(), "message", i.message())).toList());
        return problem;
    }

    @ExceptionHandler(SubmitInvalidException.class)
    public ProblemDetail invalidSubmit(SubmitInvalidException e) {
        ProblemDetail problem = problem(e.status(), e.code(), e.getMessage());
        problem.setProperty("issues", e.issues().stream()
                .map(i -> Map.of("code", i.code(), "message", i.message(),
                        "blockId", i.blockId() == null ? "" : i.blockId().toString())).toList());
        return problem;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ProblemDetail unreadable(Exception e) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Dữ liệu yêu cầu không hợp lệ");
    }

    private static ProblemDetail problem(HttpStatusCode status, String code, String message) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message);
        problem.setProperty("code", code);
        return problem;
    }
}
