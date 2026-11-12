package edu.aiplatform.ai.contract;

/** Phân loại kết quả kiểm duyệt prompt; chỉ {@link #VALID} được gửi tới mô hình chính. */
public enum GuardrailCategory {
    VALID,
    PROMPT_INJECTION,
    OFF_TOPIC,
    HARMFUL_MALICIOUS
}
