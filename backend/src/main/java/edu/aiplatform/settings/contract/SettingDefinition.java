package edu.aiplatform.settings.contract;

import java.util.Map;

/**
 * Unit sở hữu khai báo mục cài đặt khóa–giá trị bằng bean {@code SettingDefinition} (U03, U08).
 * {@code defaultValue} rỗng là "chưa cấu hình".
 */
public record SettingDefinition(String key, SettingGroup group, String name, String description, SettingType type,
                                Object defaultValue, Map<String, Object> constraints) {
}
