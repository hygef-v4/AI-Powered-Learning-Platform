package edu.aiplatform.materials.controller;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import edu.aiplatform.shared.error.ApiException;

@RestControllerAdvice(basePackageClasses = MaterialController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MaterialExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApiException(ApiException failure) {
        return createProblem(failure.readStatus(), failure.readCode(), failure.getMessage());
    }

    @ExceptionHandler({
        IllegalArgumentException.class,
        MethodArgumentNotValidException.class,
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ProblemDetail handleInvalidRequest(Exception ignored) {
        return createProblem(
                HttpStatus.BAD_REQUEST,
                "MATERIAL_REQUEST_INVALID",
                "Thông tin học liệu không hợp lệ");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedFailure(Exception failure) {
        // Lỗi request do Spring MVC phân loại (sai Content-Type 415, sai method 405...) giữ status 4xx.
        if (failure instanceof ErrorResponse response
                && response.getStatusCode().is4xxClientError()) {
            return createProblem(
                    HttpStatus.valueOf(response.getStatusCode().value()),
                    "MATERIAL_REQUEST_INVALID",
                    "Thông tin học liệu không hợp lệ");
        }
        return createProblem(
                HttpStatus.SERVICE_UNAVAILABLE,
                "MATERIAL_UNAVAILABLE",
                "Không xử lý được học liệu, vui lòng thử lại");
    }

    private ProblemDetail createProblem(HttpStatus status, String code, String message) {
        var detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setProperty("code", code);
        return detail;
    }
}
