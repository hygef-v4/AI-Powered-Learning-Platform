package edu.aiplatform.notifications.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import edu.aiplatform.notifications.model.*;
import edu.aiplatform.notifications.repository.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.groupdocs.contract.ISseHub;
import edu.aiplatform.audit.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import edu.aiplatform.groupdocs.integration.RabbitSseHub;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

class NotificationServiceTest {
    INotificationRepository notificationRepository;
    EmailDeliveryService emailDeliveryService;
    ISseHub sseHub;
    IAuthorization authorization;
    IAudit audit;
    NotificationAccess notificationAccess;
    NotificationService notificationService;
    UUID account = UUID.randomUUID();
    Actor actor = new Actor(account, Role.STUDENT);
    Clock clock = Clock.fixed(Instant.parse("2026-10-21T08:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setup() {
        notificationRepository = mock(INotificationRepository.class);
        emailDeliveryService = mock(EmailDeliveryService.class);
        sseHub = mock(ISseHub.class);
        authorization = mock(IAuthorization.class);
        audit = mock(IAudit.class);
        when(authorization.authorize(any(), any(), any()))
                .thenReturn(AuthorizationDecision.allow());
        notificationAccess = new NotificationAccess(authorization, audit, clock);
        notificationService =
                new NotificationService(
                        notificationRepository,
                        notificationAccess,
                        emailDeliveryService,
                        sseHub,
                        clock);
    }

    @AfterEach
    void cleanup() {
        if (TransactionSynchronizationManager.isSynchronizationActive())
            TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void sseReachesBrokerOnlyAfterCommitAndOneNoticePerRecipient() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        NotificationService service = createServiceWithRealHub(rabbit);
        when(notificationRepository.insertNotification(any())).thenReturn(true);
        TransactionSynchronizationManager.initSynchronization();
        service.createNotification(
                UUID.randomUUID(),
                NotificationType.ENROLLED,
                List.of(account, account),
                "t",
                "s",
                "/classes/1",
                "ENROLLED",
                null);
        verify(notificationRepository, times(1)).insertNotification(any());
        verify(emailDeliveryService).scheduleEmail(any(), eq("ENROLLED"), isNull());
        verifyNoInteractions(rabbit);
        // Same snapshot-based callback run as Spring's commit (IT-U14-M03 regression).
        TransactionSynchronizationUtils.triggerAfterCommit();
        verify(rabbit, times(1))
                .convertAndSend(
                        eq("platform.realtime"),
                        eq(""),
                        eq(
                                Map.of(
                                        "channel",
                                        NotificationService.buildChannel(account),
                                        "event",
                                        Map.of("type", "notification-changed"))));
    }

    @Test
    void markReadReachesBrokerAfterCommit() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        NotificationService service = createServiceWithRealHub(rabbit);
        when(notificationRepository.markRead(any(), eq(account), any())).thenReturn(true);
        TransactionSynchronizationManager.initSynchronization();
        service.markRead(actor, UUID.randomUUID());
        verifyNoInteractions(rabbit);
        TransactionSynchronizationUtils.triggerAfterCommit();
        verify(rabbit).convertAndSend(eq("platform.realtime"), eq(""), any(Object.class));
    }

    private NotificationService createServiceWithRealHub(RabbitTemplate rabbit) {
        return new NotificationService(
                notificationRepository,
                notificationAccess,
                emailDeliveryService,
                new RabbitSseHub(rabbit),
                clock);
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void everyRoleHasOnlyItsOwnSettings(Role role) {
        var notificationPreferenceService =
                new NotificationPreferenceService(notificationRepository, notificationAccess);
        when(notificationRepository.findPreferences(account)).thenReturn(Map.of());
        var values = notificationPreferenceService.loadPreferences(new Actor(account, role));
        var expected =
                switch (role) {
                    case STUDENT ->
                            Set.of(
                                    "ENROLLED",
                                    "ASSIGNMENT_OPENED",
                                    "GRADE_PUBLISHED",
                                    "DEADLINE_REMINDER",
                                    "ANNOUNCEMENT_IMPORTANT");
                    case TEACHER -> Set.of("LEADER_CHANGE_REQUESTED", "QUESTION_REVIEWED");
                    case SUBJECT_MANAGER ->
                            Set.of(
                                    "LEADER_CHANGE_REQUESTED",
                                    "QUESTION_REVIEWED",
                                    "QUESTION_PROPOSED");
                    case ADMIN -> Set.of("BACKUP_FAILED");
                };
        assertThat(values.keySet()).isEqualTo(expected);
        // Mọi vai trò mặc định bật hết (yêu cầu 2026-11-05).
        assertThat(values.values()).allMatch(Boolean.TRUE::equals);
    }

    @ParameterizedTest
    @EnumSource(
            value = Role.class,
            names = {"TEACHER", "SUBJECT_MANAGER"})
    void staffCanSaveOwnPreferencesButNotStudentOrBackupTypes(Role role) {
        var notificationPreferenceService =
                new NotificationPreferenceService(notificationRepository, notificationAccess);
        var staff = new Actor(account, role);
        when(notificationRepository.findPreferences(account))
                .thenReturn(Map.of("QUESTION_REVIEWED", true, "ENROLLED", false));
        assertThat(
                notificationPreferenceService.updatePreferences(
                        staff, Map.of("QUESTION_REVIEWED", true)))
                .containsEntry("QUESTION_REVIEWED", true)
                .doesNotContainKey("ENROLLED");
        verify(notificationRepository).savePreference(account, "QUESTION_REVIEWED", true);
        assertThatThrownBy(
                () -> notificationPreferenceService.updatePreferences(
                        staff, Map.of("ENROLLED", false)))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(
                () -> notificationPreferenceService.updatePreferences(
                        staff, Map.of("BACKUP_FAILED", false)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void storedOptOutIsPreservedWhileOtherDefaultsStayEnabled() {
        var notificationPreferenceService =
                new NotificationPreferenceService(notificationRepository, notificationAccess);
        when(notificationRepository.findPreferences(account))
                .thenReturn(Map.of("QUESTION_REVIEWED", false));
        assertThat(notificationPreferenceService.loadPreferences(new Actor(account, Role.TEACHER)))
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of("QUESTION_REVIEWED", false, "LEADER_CHANGE_REQUESTED", true));
        verify(notificationRepository, never()).savePreference(any(), anyString(), anyBoolean());
    }

    @Test
    void specificReminderAndNewCopyArePreservedInFeed() {
        var reminder = row(NotificationType.DEADLINE_REMINDER, "Hạn nộp bài còn khoảng24giờ.");
        var request =
                row(
                        NotificationType.LEADER_CHANGE_REQUESTED,
                        "Xem yêu cầu để phê duyệt hoặc từ chối.");
        when(notificationRepository.listNotifications(account, true, 1, 20))
                .thenReturn(List.of(reminder, request));

        assertThat(notificationService.listNotifications(actor, true, 1, 20).items())
                .extracting(NotificationService.Item::summary)
                .containsExactly(reminder.summary, request.summary);
        assertThat(reminder.readAt).isNull();
        assertThat(request.readAt).isNull();
        verifyNoInteractions(emailDeliveryService, sseHub);
    }

    private Notification row(NotificationType type, String summary) {
        var n = new Notification();
        n.id = UUID.randomUUID();
        n.recipientId = account;
        n.sourceEventId = UUID.randomUUID();
        n.type = type;
        n.title = "Existing title";
        n.summary = summary;
        n.link = "/classes/" + UUID.randomUUID();
        n.createdAt = clock.instant().minusSeconds(120);
        return n;
    }
}
