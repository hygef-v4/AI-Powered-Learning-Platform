package edu.aiplatform.admindashboard.controller;

import edu.aiplatform.shared.error.ApiException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AdminDashboardController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AdminDashboardExceptionHandler {
    /**
     * Handles known API exceptions and converts them into RFC 7807 ProblemDetail responses.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApiProblem(ApiException error) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(error.status(), error.getMessage());
        detail.setProperty("code", error.code());
        return ResponseEntity.status(error.status()).cacheControl(CacheControl.noStore())
                .body(detail);
    }

    /**
     * Catches unexpected runtime exceptions and converts them into a safe DASHBOARD_UNAVAILABLE error.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedError(RuntimeException error) {
        return handleApiProblem(ApiException.unavailable("DASHBOARD_UNAVAILABLE",
                "Không đọc được tổng quan hệ thống"));
    }
}
