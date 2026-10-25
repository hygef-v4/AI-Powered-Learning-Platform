package edu.aiplatform.quizzes.application;

import org.springframework.stereotype.Component;

/**
 * Ghi nhận hoạt động (BR-U10-15, K1). Thời hạn giữ dữ liệu hoạt động giờ cố định 1 ngày (U08
 * {@code ActivityService.RETENTION}), không còn cài đặt Admin phải đặt trước nên luôn bật được.
 */
@Component
class QuizActivityPolicy {

    boolean retentionConfigured() {
        return true;
    }
}
