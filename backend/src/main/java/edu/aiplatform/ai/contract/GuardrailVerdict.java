package edu.aiplatform.ai.contract;

/**
 * Kết quả kiểm duyệt. {@code tier} là tầng đã quyết định (0 khi hợp lệ); {@code ruleId} để log và kiểm thử, không hiển
 * thị cho người dùng; {@code message} là câu thân thiện, không lộ quy tắc hay nội dung bị chặn.
 */
public record GuardrailVerdict(GuardrailCategory category, int tier, String ruleId, String message) {

    private static final GuardrailVerdict VALID = new GuardrailVerdict(GuardrailCategory.VALID, 0, null, null);

    public static GuardrailVerdict valid() {
        return VALID;
    }

    public static GuardrailVerdict reject(GuardrailCategory category, int tier, String ruleId, String message) {
        if (category == null || category == GuardrailCategory.VALID) {
            throw new IllegalArgumentException("Verdict từ chối cần phân loại vi phạm");
        }
        return new GuardrailVerdict(category, tier, ruleId, message);
    }

    public boolean isAllowed() {
        return category == GuardrailCategory.VALID;
    }
}
