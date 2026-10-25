package edu.aiplatform.quizzes.application;

import edu.aiplatform.settings.contract.ISettings;
import edu.aiplatform.settings.contract.SettingKey;
import org.springframework.stereotype.Component;

/**
 * Ghi nhận hoạt động chỉ bật được khi Admin đã đặt {@code activity.retentionDays} (BR-U10-15, K1). Khóa do U08 khai
 * báo; U03 không trả lời thì coi như chưa đặt, giống cách U07 kiểm.
 */
@Component
class QuizActivityPolicy {

    static final SettingKey<Integer> RETENTION_DAYS = new SettingKey<>("activity.retentionDays", Integer.class);

    private final ISettings settings;

    QuizActivityPolicy(ISettings settings) {
        this.settings = settings;
    }

    boolean retentionConfigured() {
        try {
            return settings.get(RETENTION_DAYS).isPresent();
        } catch (RuntimeException unavailable) {
            return false;
        }
    }
}
