package edu.aiplatform.notifications.web;
import jakarta.persistence.PersistenceException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestControllerAdvice(basePackages={"edu.aiplatform.announcements.web","edu.aiplatform.notifications.web"})
public class NotificationExceptionHandler {
    @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    public ProblemDetail invalid(Exception ignored) {var p=ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"Dữ liệu không hợp lệ, kiểm tra nội dung và phiên bản");p.setProperty("code","NOTIFICATION_INPUT_INVALID");return p;}
    @ExceptionHandler(PersistenceException.class)
    public ProblemDetail database(PersistenceException ignored) {var p=ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,"Hệ thống tạm bận, vui lòng thử lại");p.setProperty("code","NOTIFICATION_UNAVAILABLE");return p;}
}
