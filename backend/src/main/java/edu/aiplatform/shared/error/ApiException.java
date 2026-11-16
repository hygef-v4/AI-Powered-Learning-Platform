package edu.aiplatform.shared.error;

import org.springframework.http.HttpStatus;

/** Lỗi trả cho client dạng Problem Detail: mã HTTP, mã lỗi máy đọc được và thông điệp an toàn. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus readStatus() {
        return status;
    }

    public String readCode() {
        return code;
    }

    public static ApiException createBadRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static ApiException createForbidden(String code) {
        return new ApiException(HttpStatus.FORBIDDEN, code, "Không có quyền thực hiện thao tác này");
    }

    public static ApiException createNotFound(String code) {
        return new ApiException(HttpStatus.NOT_FOUND, code, "Không tìm thấy");
    }

    public static ApiException createConflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }

    public static ApiException createGone(String code, String message) {
        return new ApiException(HttpStatus.GONE, code, message);
    }

    public static ApiException createPayloadTooLarge(String code, String message) {
        return new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, code, message);
    }

    public static ApiException createUnsupportedMediaType(String code, String message) {
        return new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, code, message);
    }

    public static ApiException createUnavailable(String code, String message) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, code, message);
    }
}
