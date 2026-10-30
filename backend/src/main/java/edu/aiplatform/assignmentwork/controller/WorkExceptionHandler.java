package edu.aiplatform.assignmentwork.controller;

import edu.aiplatform.assignmentwork.service.WorkProblem;
import edu.aiplatform.shared.error.ApiException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/** Problem Detail của U08: thêm {@code issues} khi có; body sai trả 400. */
@RestControllerAdvice(assignableTypes = {StudentAssignmentController.class, AttemptController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WorkExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ProblemDetail problem(ApiException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.status(), exception.getMessage());
        problem.setProperty("code", exception.code());
        if (exception instanceof WorkProblem work) {
            work.properties().forEach(problem::setProperty);
        }
        return problem;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integrity(DataIntegrityViolationException exception) {
        return problem(ApiException.conflict("ATTEMPT_CONFLICT", "Dữ liệu đã thay đổi, hãy tải lại"));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    public ProblemDetail invalid(Exception exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dữ liệu yêu cầu không hợp lệ");
        problem.setProperty("code", "INVALID_REQUEST");
        return problem;
    }
}
