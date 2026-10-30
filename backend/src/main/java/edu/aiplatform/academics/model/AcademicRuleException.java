package edu.aiplatform.academics.model;

/** Lỗi nghiệp vụ thuần U05; application/web sẽ ánh xạ sang HTTP khi được nối sau. */
public class AcademicRuleException extends RuntimeException {
    private final String code;

    public AcademicRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() { return code; }
}
