package edu.aiplatform.assignments.web;

import edu.aiplatform.assignments.application.AssignmentProblem;
import edu.aiplatform.shared.error.ApiException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/** Problem Detail của U07: thêm {@code issues} khi có; lệch {@code version} trả 409; body sai trả 400. */
@RestControllerAdvice(assignableTypes = {ClassAssignmentController.class, SubjectAssignmentController.class,
        AssignmentController.class, RubricController.class, AssignmentAiDraftController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AssignmentExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ProblemDetail problem(ApiException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.status(), exception.getMessage());
        problem.setProperty("code", exception.code());
        if (exception instanceof AssignmentProblem assignment) {
            assignment.properties().forEach(problem::setProperty);
        }
        return problem;
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail conflict(OptimisticLockingFailureException exception) {
        return problem(ApiException.conflict("ASSIGNMENT_VERSION_CONFLICT", "Bài đã thay đổi ở nơi khác. Hãy tải lại"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integrity(DataIntegrityViolationException exception) {
        return problem(ApiException.conflict("ASSIGNMENT_DATA_CONFLICT", "Dữ liệu không hợp lệ hoặc đã thay đổi"));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    public ProblemDetail invalid(Exception exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dữ liệu yêu cầu không hợp lệ");
        problem.setProperty("code", "INVALID_REQUEST");
        return problem;
    }
}
