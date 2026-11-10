package edu.aiplatform.academics.model;

/** Lỗi nghiệp vụ thuần U05; application/web sẽ ánh xạ sang HTTP khi được nối sau. */
public class AcademicRuleException extends RuntimeException {
    private final String code;

    /** Creates a domain exception with its stable error code and message. */
    public AcademicRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    /** Returns the domain error code. */
    public String code() {
        return code;
    }
}
