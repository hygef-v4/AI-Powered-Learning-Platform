package edu.aiplatform.notifications.service;

import static org.assertj.core.api.Assertions.assertThat;

import edu.aiplatform.notifications.model.NotificationType;
import org.junit.jupiter.api.Test;

class NotificationEmailTextTest {

    @Test
    void subjectPutsVietnameseFirstThenEnglish() {
        assertThat(NotificationEmailText.buildSubject(NotificationType.ANNOUNCEMENT, "Có thông báo mới của lớp"))
                .isEqualTo("Có thông báo mới của lớp / New class announcement");
    }

    @Test
    void bodyRepeatsTheSignInLinkInBothLanguages() {
        String body = NotificationEmailText.buildBody("https://learning.example.test/classes/1/announcements");
        assertThat(body)
                .startsWith("Bạn có thông báo mới trên nền tảng học tập. Đăng nhập để xem: "
                        + "https://learning.example.test/classes/1/announcements")
                .endsWith("You have a new notification on the learning platform. Sign in to view it: "
                        + "https://learning.example.test/classes/1/announcements");
    }

    @Test
    void everyNotificationTypeHasAnEnglishTitle() {
        for (NotificationType type : NotificationType.values()) {
            assertThat(NotificationEmailText.resolveEnglishTitle(type)).isNotBlank();
        }
        assertThat(NotificationEmailText.resolveEnglishTitle(null)).isEqualTo("You have a new notification");
    }
}
