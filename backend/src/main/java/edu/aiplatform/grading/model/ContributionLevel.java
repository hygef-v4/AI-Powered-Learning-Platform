package edu.aiplatform.grading.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ContributionLevel {
    EXCEEDS_EXPECTATIONS,
    MEETS_EXPECTATIONS,
    PARTIALLY_MEETS,
    NEEDS_SUPPORT,
    NOT_COMPLETED;

    // Chuyển tên enum đã lưu thành mức đóng góp, báo lỗi nếu giá trị không hợp lệ.
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ContributionLevel parseLevel(String value) {
        return valueOf(value);
    }
}
