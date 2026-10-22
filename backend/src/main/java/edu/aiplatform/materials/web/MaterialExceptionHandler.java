package edu.aiplatform.materials.web;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import edu.aiplatform.shared.error.ApiException;

@RestControllerAdvice(basePackageClasses=MaterialController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MaterialExceptionHandler {
    @ExceptionHandler(ApiException.class) public ProblemDetail api(ApiException e) {return problem(e.status(),e.code(),e.getMessage());}
    @ExceptionHandler({IllegalArgumentException.class,MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class})
    public ProblemDetail invalid(Exception ignored) {return problem(HttpStatus.BAD_REQUEST,"MATERIAL_REQUEST_INVALID","Thông tin học liệu không hợp lệ");}
    @ExceptionHandler(Exception.class) public ProblemDetail unexpected(Exception ignored) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE,"MATERIAL_UNAVAILABLE","Không xử lý được học liệu, vui lòng thử lại");
    }
    private ProblemDetail problem(HttpStatus status,String code,String message) {
        var detail=ProblemDetail.forStatusAndDetail(status,message);detail.setProperty("code",code);return detail;
    }
}
