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

@RestControllerAdvice(assignableTypes=AcademicsController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AcademicsExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ProblemDetail problem(ApiException exception) {
        ProblemDetail problem=ProblemDetail.forStatusAndDetail(exception.status(),exception.getMessage());
        problem.setProperty("code",exception.code());
        if(exception instanceof AcademicsProblem academics) academics.properties().forEach(problem::setProperty);
        return problem;
    }
    @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class})
    public ProblemDetail invalid(Exception exception) {
        ProblemDetail problem=ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"Dữ liệu yêu cầu không hợp lệ");
        problem.setProperty("code","INVALID_REQUEST");
        return problem;
    }
}
