package edu.aiplatform.backup.application;

import edu.aiplatform.settings.contract.SettingDefinition;
import edu.aiplatform.settings.contract.SettingGroup;
import edu.aiplatform.settings.contract.SettingKey;
import edu.aiplatform.settings.contract.SettingType;
import java.time.LocalTime;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Nhóm cài đặt Backup (BR-U03-88): mặc định tắt, giờ chạy không có mặc định. Bật được khi chưa đặt giờ; chưa có giờ
 * thì chưa chạy bản nào ({@code BackupScheduleScanner}).
 */
@Configuration
public class BackupSettingDefinitions {

    public static final SettingKey<Boolean> ENABLED = new SettingKey<>("backup.enabled", Boolean.class);
    public static final SettingKey<LocalTime> DAILY_AT = new SettingKey<>("backup.dailyAt", LocalTime.class);

    @Bean
    SettingDefinition backupEnabled() {
        return new SettingDefinition(ENABLED.key(), SettingGroup.BACKUP, "Bật sao lưu",
                "Sao lưu cơ sở dữ liệu hằng ngày.", SettingType.BOOLEAN, false,
                Map.of());
    }

    @Bean
    SettingDefinition backupDailyAt() {
        return new SettingDefinition(DAILY_AT.key(), SettingGroup.BACKUP, "Giờ chạy hằng ngày",
                "Giờ sao lưu, theo giờ Việt Nam.", SettingType.TIME_OF_DAY,
                null, Map.of());
    }
}
