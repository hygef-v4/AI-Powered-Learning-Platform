package edu.aiplatform.shared.error;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** Đổi lỗi thành Problem Detail kèm {@code code}; không lộ stack trace hay chi tiết nội bộ. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ProblemDetail handleApi(ApiException e) {
        return problem(e.status(), e.code(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException e) {
        boolean feedbackTooLong = e.getBindingResult().getFieldErrors().stream()
                .anyMatch(error -> "feedback".equals(error.getField()) && "Size".equals(error.getCode()));
        if (feedbackTooLong) {
            return problem(HttpStatus.BAD_REQUEST, "FEEDBACK_TOO_LONG", "Nhận xét tối đa 10.000 ký tự.");
        }
        return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Dữ liệu yêu cầu không hợp lệ");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail handleUploadSize(MaxUploadSizeExceededException e) {
        return problem(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "Tệp vượt dung lượng cho phép");
    }

    /** Redis hoặc database không khả dụng: trả "hệ thống tạm bận", không lộ chi tiết (BR-U01-94, 95; NFR-U01-40, 41). */
    @ExceptionHandler({DataAccessResourceFailureException.class, QueryTimeoutException.class,
            RedisSystemException.class})
    ProblemDetail handleUnavailable(RuntimeException e) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "SYSTEM_BUSY", "Hệ thống tạm bận, vui lòng thử lại sau");
    }

    static ProblemDetail problem(HttpStatus status, String code, String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setProperty("code", code);
        return detail;
    }
}
