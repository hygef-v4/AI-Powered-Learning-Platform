package edu.aiplatform.subjects.web;

import edu.aiplatform.shared.error.ApiException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Chỉ áp dụng controller U04; không đổi exception handler/Jackson/security của unit khác. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = SubjectAdminController.class)
public class SubjectExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ProblemDetail api(ApiException failure) {
        ProblemDetail result = problem(failure.status(), failure.code(), failure.getMessage());
        if (failure instanceof SubjectRequests.InvalidRequest invalid) result.setProperty("fieldErrors", invalid.fieldErrors());
        return result;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ProblemDetail invalid(Exception failure) {
        return problem(HttpStatus.BAD_REQUEST, "SUBJECT_REQUEST_INVALID", "JSON, mã định danh hoặc tham số không hợp lệ");
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail stale(OptimisticLockingFailureException failure) {
        return problem(HttpStatus.CONFLICT, "SUBJECT_VERSION_CONFLICT", "Dữ liệu môn đã thay đổi. Hãy tải lại trước khi sửa");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integrity(DataIntegrityViolationException failure) {
        return problem(HttpStatus.CONFLICT, "SUBJECT_DATA_CONFLICT", "Dữ liệu liên quan đã thay đổi, vui lòng tải lại");
    }

    @ExceptionHandler(DataAccessException.class)
    public ProblemDetail storage(DataAccessException failure) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "SUBJECT_STORAGE_UNAVAILABLE", "Chưa truy cập được dữ liệu, vui lòng thử lại");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail unexpected(Exception failure) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "SUBJECT_INTERNAL_ERROR", "Chưa xử lý được yêu cầu, vui lòng thử lại");
    }

    private ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail result = ProblemDetail.forStatusAndDetail(status, detail);
        result.setProperty("code", code);
        return result;
    }
}
