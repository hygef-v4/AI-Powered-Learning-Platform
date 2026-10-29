package edu.aiplatform.admindashboard.web;

import edu.aiplatform.shared.error.ApiException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AdminDashboardController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AdminDashboardExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> problem(ApiException error) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(error.status(), error.getMessage());
        detail.setProperty("code", error.code());
        return ResponseEntity.status(error.status()).cacheControl(CacheControl.noStore()).body(detail);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ProblemDetail> unexpected(RuntimeException error) {
        return problem(ApiException.unavailable("DASHBOARD_UNAVAILABLE", "Không đọc được tổng quan hệ thống"));
    }
}
