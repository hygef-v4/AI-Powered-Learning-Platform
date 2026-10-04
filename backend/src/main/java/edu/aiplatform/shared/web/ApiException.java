package edu.aiplatform.shared.web;

import org.springframework.http.HttpStatus;

/** Lỗi nghiệp vụ trả cho client: `code` ổn định, `safeMessage` không lộ dữ liệu nội bộ. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String safeMessage) {
        super(safeMessage);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }
}
