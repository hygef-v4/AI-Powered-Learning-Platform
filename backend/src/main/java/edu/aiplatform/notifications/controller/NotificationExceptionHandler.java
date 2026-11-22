package edu.aiplatform.notifications.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(
        basePackages = {
            "edu.aiplatform.announcements.controller",
            "edu.aiplatform.notifications.controller"
        })
public class NotificationExceptionHandler {
    @ExceptionHandler({
        org.springframework.web.bind.MethodArgumentNotValidException.class,
        org.springframework.http.converter.HttpMessageNotReadableException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
    })
    public ProblemDetail handleInvalidRequest(Exception ignored) {
        var problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        "Dữ liệu không hợp lệ, kiểm tra nội dung và phiên bản");
        problem.setProperty("code", "NOTIFICATION_INPUT_INVALID");
        return problem;
    }
}
