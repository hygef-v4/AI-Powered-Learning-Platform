package edu.aiplatform.quizzes.controller;

import edu.aiplatform.quizzes.service.RuleViolationException;
import edu.aiplatform.shared.error.ApiException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/** Chỉ áp dụng cho controller của U10; không đổi handler của unit khác. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {QuestionController.class, SubjectQuizController.class,
        ClassQuizController.class, QuizController.class})
public class QuizzesExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ProblemDetail api(ApiException failure) {
        ProblemDetail result = problem(failure.status(), failure.code(), failure.getMessage());
        if (failure instanceof RuleViolationException invalid) {
            result.setProperty("violations", invalid.violations());
        }
        return result;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    public ProblemDetail invalid(Exception failure) {
        return problem(HttpStatus.BAD_REQUEST, "QUESTION_REQUEST_INVALID", "JSON, tham số hoặc tệp không hợp lệ");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integrity(DataIntegrityViolationException failure) {
        return problem(HttpStatus.CONFLICT, "QUESTION_DATA_CONFLICT", "Dữ liệu câu hỏi đã thay đổi, vui lòng tải lại");
    }

    @ExceptionHandler(DataAccessException.class)
    public ProblemDetail storage(DataAccessException failure) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "SYSTEM_BUSY", "Chưa truy cập được dữ liệu, vui lòng thử lại");
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail result = ProblemDetail.forStatusAndDetail(status, detail);
        result.setProperty("code", code);
        return result;
    }
}
