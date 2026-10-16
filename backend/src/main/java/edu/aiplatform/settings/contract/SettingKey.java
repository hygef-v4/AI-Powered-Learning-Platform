package edu.aiplatform.settings.contract;

/** Khóa cài đặt kèm kiểu giá trị, để {@link ISettings#get(SettingKey)} trả đúng kiểu. */
public record SettingKey<T>(String key, Class<T> valueType) {
}
