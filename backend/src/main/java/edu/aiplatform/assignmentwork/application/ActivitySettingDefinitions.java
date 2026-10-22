package edu.aiplatform.assignmentwork.application;

import edu.aiplatform.settings.contract.SettingDefinition;
import edu.aiplatform.settings.contract.SettingGroup;
import edu.aiplatform.settings.contract.SettingType;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Khai báo {@code activity.retentionDays} (BR-U08-54): nhóm ACTIVITY, 1–365 ngày, không có mặc định; chưa đặt thì U07,
 * U10 không cho bật theo dõi hoạt động. Khóa ràng buộc theo quy ước của U03 ({@code min}, {@code max}, {@code unit}).
 */
@Configuration
public class ActivitySettingDefinitions {

    @Bean
    SettingDefinition activityRetentionDays() {
        return new SettingDefinition(ActivityService.RETENTION_DAYS.key(), SettingGroup.ACTIVITY,
                "Thời hạn giữ dữ liệu hoạt động",
                "Số ngày giữ sự kiện rời trang của bài làm; quá hạn thì tự xóa. Chưa đặt thì không bật được ghi nhận "
                        + "hoạt động", SettingType.INTEGER, null, Map.of("min", 1, "max", 365, "unit", "ngày"));
    }
}
