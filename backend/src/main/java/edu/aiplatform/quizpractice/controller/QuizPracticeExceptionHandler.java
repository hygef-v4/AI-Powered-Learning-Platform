package edu.aiplatform.quizpractice.controller;

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

/** Chỉ áp dụng cho controller của U11; không đổi handler của unit khác. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {QuizPracticeController.class, TeacherQuizAttemptController.class})
public class QuizPracticeExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ProblemDetail api(ApiException failure) {
        return problem(failure.status(), failure.code(), failure.getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ProblemDetail invalid(Exception failure) {
        return problem(HttpStatus.BAD_REQUEST, "QUIZ_PRACTICE_REQUEST_INVALID", "JSON hoặc tham số không hợp lệ");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integrity(DataIntegrityViolationException failure) {
        return problem(HttpStatus.CONFLICT, "QUIZ_ATTEMPT_CONFLICT", "Lượt làm đã thay đổi, vui lòng tải lại");
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
