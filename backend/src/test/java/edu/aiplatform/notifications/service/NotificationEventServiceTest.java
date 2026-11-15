package edu.aiplatform.notifications.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.aiplatform.jobs.contract.*;
import edu.aiplatform.notifications.model.NotificationType;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NotificationEventServiceTest {
    final RecipientResolver recipientResolver = mock(RecipientResolver.class);
    final NotificationService notificationService = mock(NotificationService.class);
    final NotificationEventService notificationEventService =
            new NotificationEventService(recipientResolver, notificationService);
    final UUID recipient = UUID.randomUUID();

    @Test
    void teachingRequestExplainsTheDecisionWithoutChangingResolvedDelivery() {
        assertThat(
                        summary(
                                EventTypes.GROUP_LEADER_CHANGE_REQUESTED,
                                "U12",
                                NotificationType.LEADER_CHANGE_REQUESTED,
                                Map.of(),
                                null))
                .contains("phê duyệt", "từ chối")
                .doesNotContain("Đăng nhập");
    }

    @Test
    void adminFailureDirectsToBackupHandlingInsteadOfStudentActions() {
        assertThat(
                        summary(
                                EventTypes.BACKUP_FAILED,
                                "U03",
                                NotificationType.BACKUP_FAILED,
                                Map.of(),
                                "BACKUP_FAILED"))
                .contains("sao lưu", "xử lý")
                .doesNotContain("Đăng nhập", "luyện tập", "điểm");
    }

    @Test
    void gradeSummaryDoesNotCopyPrivateEventPayload() {
        assertThat(
                        summary(
                                EventTypes.GRADE_PUBLISHED,
                                "U09",
                                NotificationType.GRADE_PUBLISHED,
                                Map.of("score", "9.5", "comment", "PRIVATE_GRADE_COMMENT"),
                                "GRADE_PUBLISHED"))
                .contains("kết quả")
                .doesNotContain("9.5", "PRIVATE_GRADE_COMMENT", "Đăng nhập");
    }

    private String summary(
            String eventType,
            String source,
            NotificationType type,
            Map<String, Object> payload,
            String emailType) {
        var event =
                new DomainEvent(
                        1, UUID.randomUUID(), eventType, source, Instant.now(), "qa-copy", payload);
        String link = "/classes/" + UUID.randomUUID();
        when(recipientResolver.resolveRecipients(event))
                .thenReturn(
                        List.of(
                                new RecipientResolver.Resolved(
                                        type, List.of(recipient), link, emailType)));
        notificationEventService.handleEvent(event);
        var copy = ArgumentCaptor.forClass(String.class);
        verify(notificationService)
                .createNotification(
                        eq(event.eventId()),
                        eq(type),
                        eq(List.of(recipient)),
                        eq(NotificationEventService.resolveTitle(type)),
                        copy.capture(),
                        eq(link),
                        eq(emailType),
                        isNull());
        return copy.getValue();
    }
}
