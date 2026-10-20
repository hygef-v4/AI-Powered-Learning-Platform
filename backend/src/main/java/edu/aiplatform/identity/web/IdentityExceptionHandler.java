package edu.aiplatform.identity.web;

import edu.aiplatform.identity.application.PasswordPolicyException;
import edu.aiplatform.identity.application.RoleChangeBlockedException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * Lỗi riêng của U01 cần thêm dữ liệu ngoài {@code code}; các lỗi khác đi qua {@code GlobalExceptionHandler}.
 * Ưu tiên cao nhất để thắng handler Problem Detail mặc định của Spring Boot, nhưng chỉ áp dụng cho controller U01.
 */
@RestControllerAdvice(basePackageClasses = IdentityExceptionHandler.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IdentityExceptionHandler {

    @ExceptionHandler(PasswordPolicyException.class)
    ProblemDetail handlePasswordPolicy(PasswordPolicyException e) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        detail.setProperty("code", "PASSWORD_POLICY");
        detail.setProperty("violations", e.violations().stream().map(Enum::name).sorted().toList());
        return detail;
    }

    /** BR-U01-64, MSG29: nêu môn/lớp còn phụ trách để Admin xử lý phân công trước. */
    @ExceptionHandler(RoleChangeBlockedException.class)
    ProblemDetail handleRoleChangeBlocked(RoleChangeBlockedException e) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        detail.setProperty("code", "ROLE_CHANGE_BLOCKED");
        detail.setProperty("managedSubjects", e.managedSubjects());
        detail.setProperty("taughtClasses", e.taughtClasses());
        return detail;
    }

    /**
     * JSON sai, có field không được phép (BR-U01-52), thiếu field hoặc vượt độ dài (Bean Validation), tham số sai kiểu:
     * cùng mã {@code INVALID_REQUEST} để frontend xử lý theo {@code code}; không nêu chi tiết nội bộ.
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentNotValidException.class,
            HandlerMethodValidationException.class, ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class})
    ProblemDetail handleInvalidRequest(Exception e) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Dữ liệu gửi lên không hợp lệ hoặc có trường không được phép sửa");
        detail.setProperty("code", "INVALID_REQUEST");
        return detail;
    }
}
