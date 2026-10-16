package edu.aiplatform.backup.application;

import edu.aiplatform.settings.contract.SettingDefinition;
import edu.aiplatform.settings.contract.SettingGroup;
import edu.aiplatform.settings.contract.SettingKey;
import edu.aiplatform.settings.contract.SettingType;
import edu.aiplatform.settings.domain.SettingValidator;
import java.time.LocalTime;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Nhóm cài đặt Backup (BR-U03-88): mặc định tắt, giờ chạy không có mặc định. */
@Configuration
public class BackupSettingDefinitions {

    public static final SettingKey<Boolean> ENABLED = new SettingKey<>("backup.enabled", Boolean.class);
    public static final SettingKey<LocalTime> DAILY_AT = new SettingKey<>("backup.dailyAt", LocalTime.class);

    @Bean
    SettingDefinition backupEnabled() {
        return new SettingDefinition(ENABLED.key(), SettingGroup.BACKUP, "Bật backup database",
                "Mỗi ngày backup database một lần vào giờ đã đặt; các bản được giữ 7 ngày", SettingType.BOOLEAN, false,
                Map.of(SettingValidator.REQUIRES_WHEN_TRUE, DAILY_AT.key()));
    }

    @Bean
    SettingDefinition backupDailyAt() {
        return new SettingDefinition(DAILY_AT.key(), SettingGroup.BACKUP, "Giờ chạy backup",
                "Giờ:phút theo giờ Việt Nam (Asia/Ho_Chi_Minh); nên chọn ngoài giờ cao điểm", SettingType.TIME_OF_DAY,
                null, Map.of());
    }
}
