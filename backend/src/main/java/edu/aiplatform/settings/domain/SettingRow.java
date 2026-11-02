package edu.aiplatform.settings.domain;

import java.time.Instant;
import java.util.UUID;

/** Một dòng {@code system_settings}; {@code valueJson} rỗng là "chưa cấu hình". */
public record SettingRow(String key, String group, String valueJson, long version, UUID updatedBy, Instant updatedAt) {
}
