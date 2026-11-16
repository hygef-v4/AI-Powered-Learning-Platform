package edu.aiplatform.academics.controller;

import edu.aiplatform.academics.service.AcademicsProblem;
import edu.aiplatform.shared.error.ApiException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = AcademicsController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AcademicsExceptionHandler {
    /**
     * Translates domain ApiException and AcademicsProblem into structured RFC 7807 ProblemDetail responses.
     */
    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleAcademicProblem(ApiException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.readStatus(),
                exception.getMessage());
        problem.setProperty("code", exception.readCode());
        if (exception instanceof AcademicsProblem academics)
            academics.describeProperties().forEach(problem::setProperty);
        return problem;
    }

    /**
     * Translates Spring input validation, parsing, and type mismatch exceptions into BAD_REQUEST ProblemDetail.
     */
    @ExceptionHandler({ MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
    public ProblemDetail handleInvalidInput(Exception exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Dữ liệu yêu cầu không hợp lệ");
        problem.setProperty("code", "INVALID_REQUEST");
        return problem;
    }
}
