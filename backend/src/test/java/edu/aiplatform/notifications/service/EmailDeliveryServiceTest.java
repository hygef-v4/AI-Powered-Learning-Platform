package edu.aiplatform.notifications.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import edu.aiplatform.notifications.model.*;
import edu.aiplatform.notifications.repository.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.jobs.contract.*;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;

class EmailDeliveryServiceTest {
    INotificationRepository notificationRepository;
    IJob job;
    IAccountLookup accountLookup;
    IMail mail;
    EmailCapPolicy emailCapPolicy;
    EmailDeliveryService emailDeliveryService;
    IAssignmentQuery assignmentQuery;
    Instant now = Instant.parse("2026-10-21T16:59:59Z");
    UUID recipient = UUID.randomUUID();
    Clock clock = Clock.fixed(now, ZoneOffset.UTC);

    @BeforeEach
    void setup() {
        notificationRepository = mock(INotificationRepository.class);
        job = mock(IJob.class);
        accountLookup = mock(IAccountLookup.class);
        mail = mock(IMail.class);
        assignmentQuery = mock(IAssignmentQuery.class);
        emailCapPolicy = new EmailCapPolicy(notificationRepository, 300);
        emailDeliveryService =
                new EmailDeliveryService(
                        notificationRepository,
                        emailCapPolicy,
                        job,
                        accountLookup,
                        mail,
                        clock,
                        assignmentQuery,
                        "https://learning.example.test");
        when(notificationRepository.findPreferences(any())).thenReturn(Map.of());
        when(accountLookup.getContact(recipient))
                .thenReturn(
                        Optional.of(
                                new AccountContact(
                                        recipient,
                                        "test@example.test",
                                        "Student",
                                        Role.STUDENT,
                                        AccountStatus.ACTIVE)));
    }

    @Test
    void reminderEmailAcceptsNewAndLegacyLinksAndStillChecksDeadline() {
        UUID assignmentId = UUID.randomUUID(), classId = UUID.randomUUID();
        var deadline = now.plusSeconds(1000);
        var source =
                new edu.aiplatform.assignments.contract.AssignmentView(
                        assignmentId,
                        edu.aiplatform.assignments.contract.AssignmentScope.CLASS,
                        classId,
                        UUID.randomUUID(),
                        1,
                        "Bài",
                        edu.aiplatform.assignments.contract.SubmissionMode.INDIVIDUAL,
                        edu.aiplatform.assignments.contract.GradingMode.GRADED,
                        false,
                        edu.aiplatform.assignments.contract.AssignmentStatus.OPEN,
                        now.minusSeconds(1000),
                        deadline,
                        null,
                        null,
                        false,
                        null,
                        List.of());
        when(assignmentQuery.get(assignmentId)).thenReturn(Optional.of(source));
        for (String prefix : List.of("/my-assignments/", "/assignments/")) {
            var e = pending();
            e.type = "DEADLINE_REMINDER";
            e.expiresAt = deadline;
            var n = notificationRepository.findNotification(e.notificationId).orElseThrow();
            n.link = prefix + assignmentId + "?classId=" + classId;
            emailDeliveryService.sendEmail(e.id);
            assertThat(e.status).isEqualTo(EmailDelivery.Status.SENT);
            verify(mail)
                    .send(
                            argThat(
                                    m ->
                                            m.textBody()
                                                    .contains(
                                                            "https://learning.example.test"
                                                                    + n.link)));
        }
        var stale = pending();
        stale.type = "DEADLINE_REMINDER";
        stale.expiresAt = deadline.plusSeconds(1);
        notificationRepository.findNotification(stale.notificationId).orElseThrow().link =
                "/my-assignments/" + assignmentId + "?classId=" + classId;
        emailDeliveryService.sendEmail(stale.id);
        assertThat(stale.status).isEqualTo(EmailDelivery.Status.SKIPPED);
        verify(mail, times(2)).send(any());
    }

    @Test
    void malformedReminderLinkIsSkippedWithoutQueryOrRetry() {
        for (String link :
                List.of(
                        "/my-assignments/not-a-uuid",
                        "/assignments/" + UUID.randomUUID() + "/extra",
                        "https://external.test/assignments/" + UUID.randomUUID())) {
            var e = pending();
            e.type = "DEADLINE_REMINDER";
            e.expiresAt = now.plusSeconds(1000);
            notificationRepository.findNotification(e.notificationId).orElseThrow().link = link;
            assertThatCode(() -> emailDeliveryService.sendEmail(e.id)).doesNotThrowAnyException();
            assertThat(e.status).isEqualTo(EmailDelivery.Status.SKIPPED);
        }
        verifyNoInteractions(assignmentQuery, mail);
    }

    @Test
    void reminderLookupFailureRemainsRetryable() {
        var e = pending();
        e.type = "DEADLINE_REMINDER";
        e.expiresAt = now.plusSeconds(1000);
        UUID assignmentId = UUID.randomUUID();
        notificationRepository.findNotification(e.notificationId).orElseThrow().link =
                "/my-assignments/" + assignmentId + "?classId=" + UUID.randomUUID();
        when(assignmentQuery.get(assignmentId))
                .thenThrow(new IllegalStateException("private provider error"));
        assertThatThrownBy(() -> emailDeliveryService.sendEmail(e.id))
                .isInstanceOf(RetryableJobException.class)
                .hasMessage("NOTIFICATION_ASSIGNMENT_UNAVAILABLE");
        assertThat(e.status).isEqualTo(EmailDelivery.Status.PENDING);
        verifyNoInteractions(mail);
    }

    Notification notice() {
        var n = new Notification();
        n.id = UUID.randomUUID();
        n.recipientId = recipient;
        n.title = "Có điểm mới";
        n.link = "/classes/1";
        when(notificationRepository.findNotification(n.id)).thenReturn(Optional.of(n));
        return n;
    }

    EmailDelivery pending() {
        var n = notice();
        var e = new EmailDelivery();
        e.id = UUID.randomUUID();
        e.notificationId = n.id;
        e.recipientId = recipient;
        e.type = "GRADE_PUBLISHED";
        e.status = EmailDelivery.Status.PENDING;
        e.scheduledFor = now;
        e.createdAt = now;
        when(notificationRepository.lockEmailDelivery(e.id)).thenReturn(Optional.of(e));
        return e;
    }

    EmailDelivery captured() {
        var captor = ArgumentCaptor.forClass(EmailDelivery.class);
        verify(notificationRepository).insertEmailDelivery(captor.capture());
        return captor.getValue();
    }

    @Test
    void defaultPreferenceQueuesEmail() {
        emailDeliveryService.scheduleEmail(notice(), "ENROLLED", null);
        assertThat(captured().status).isEqualTo(EmailDelivery.Status.PENDING);
        verify(job).enqueue(eq(JobTypes.EMAIL_SEND), anyMap(), startsWith("email:"));
    }

    @Test
    void staffEmailsDefaultOnUntilTurnedOff() {
        emailDeliveryService.scheduleEmail(notice(), "QUESTION_REVIEWED", null);
        assertThat(captured().status).isEqualTo(EmailDelivery.Status.PENDING);
        verify(job).enqueue(eq(JobTypes.EMAIL_SEND), anyMap(), startsWith("email:"));
    }

    @Test
    void staffCanTurnAnEmailOff() {
        when(notificationRepository.findPreferences(recipient))
                .thenReturn(Map.of("QUESTION_REVIEWED", false));
        emailDeliveryService.scheduleEmail(notice(), "QUESTION_REVIEWED", null);
        assertThat(captured().status).isEqualTo(EmailDelivery.Status.SKIPPED);
        verifyNoInteractions(job);
    }

    @Test
    void staffOptInQueuesEmail() {
        when(notificationRepository.findPreferences(recipient))
                .thenReturn(Map.of("QUESTION_REVIEWED", true));
        emailDeliveryService.scheduleEmail(notice(), "QUESTION_REVIEWED", null);
        assertThat(captured().status).isEqualTo(EmailDelivery.Status.PENDING);
        verify(job).enqueue(eq(JobTypes.EMAIL_SEND), anyMap(), startsWith("email:"));
    }

    @Test
    void teacherEmailCanBeDeliveredButCannotReceiveManagerOnlyEmail() {
        when(accountLookup.getContact(recipient))
                .thenReturn(
                        Optional.of(
                                new AccountContact(
                                        recipient,
                                        "t@example.test",
                                        "Teacher",
                                        Role.TEACHER,
                                        AccountStatus.ACTIVE)));
        var allowed = pending();
        allowed.type = "QUESTION_REVIEWED";
        emailDeliveryService.sendEmail(allowed.id);
        assertThat(allowed.status).isEqualTo(EmailDelivery.Status.SENT);
        var denied = pending();
        denied.type = "QUESTION_PROPOSED";
        emailDeliveryService.sendEmail(denied.id);
        assertThat(denied.status).isEqualTo(EmailDelivery.Status.SKIPPED);
        verify(mail, times(1)).send(any());
    }

    @Test
    void managerReceivesProposalEmailButNotStudentGradeEmail() {
        when(accountLookup.getContact(recipient))
                .thenReturn(
                        Optional.of(
                                new AccountContact(
                                        recipient,
                                        "m@example.test",
                                        "Manager",
                                        Role.SUBJECT_MANAGER,
                                        AccountStatus.ACTIVE)));
        var allowed = pending();
        allowed.type = "QUESTION_PROPOSED";
        emailDeliveryService.sendEmail(allowed.id);
        assertThat(allowed.status).isEqualTo(EmailDelivery.Status.SENT);
        var denied = pending();
        emailDeliveryService.sendEmail(denied.id);
        assertThat(denied.status).isEqualTo(EmailDelivery.Status.SKIPPED);
        verify(mail, times(1)).send(any());
    }

    @Test
    void disabledPreferenceStillKeepsNotificationButSkipsEmail() {
        when(notificationRepository.findPreferences(recipient))
                .thenReturn(Map.of("ENROLLED", false));
        emailDeliveryService.scheduleEmail(notice(), "ENROLLED", null);
        assertThat(captured().status).isEqualTo(EmailDelivery.Status.SKIPPED);
        verifyNoInteractions(job);
    }

    @Test
    void capReachedDefersUntilVietnamMidnight() {
        when(notificationRepository.countReservedEmails(any(), any())).thenReturn(300L);
        emailDeliveryService.scheduleEmail(notice(), "ENROLLED", null);
        var e = captured();
        assertThat(e.status).isEqualTo(EmailDelivery.Status.DEFERRED);
        assertThat(e.scheduledFor).isEqualTo(Instant.parse("2026-10-21T17:00:00Z"));
        verifyNoInteractions(job);
    }

    @Test
    void backupIgnoresLearningPreferences() {
        when(notificationRepository.findPreferences(recipient))
                .thenReturn(Map.of("BACKUP_FAILED", false));
        emailDeliveryService.scheduleEmail(notice(), "BACKUP_FAILED", null);
        assertThat(captured().status).isEqualTo(EmailDelivery.Status.PENDING);
    }

    @Test
    void expiredReminderSkippedBeforeQueue() {
        emailDeliveryService.scheduleEmail(notice(), "DEADLINE_REMINDER", now);
        assertThat(captured().status).isEqualTo(EmailDelivery.Status.SKIPPED);
        verifyNoInteractions(job);
    }

    @Test
    void sendContainsNoGradeOrBodyAndSentIsTerminal() {
        var e = pending();
        notificationRepository
                .findNotification(e.notificationId)
                .orElseThrow().summary = "9.5 PRIVATE_GRADE_COMMENT";
        emailDeliveryService.sendEmail(e.id);
        assertThat(e.status).isEqualTo(EmailDelivery.Status.SENT);
        emailDeliveryService.sendEmail(e.id);
        emailDeliveryService.markEmailFailed(e.id);
        verify(mail, times(1))
                .send(
                        argThat(
                                m ->
                                        m.htmlBody() == null
                                                && m.textBody()
                                                        .contains(
                                                                "Đăng nhập để xem: https://learning.example.test/classes/1")
                                                && !m.textBody().contains("9.5")
                                                && !m.textBody()
                                                        .contains("PRIVATE_GRADE_COMMENT")));
        assertThat(e.status).isEqualTo(EmailDelivery.Status.SENT);
    }

    @Test
    void smtpFailureIsRetryableAndSanitized() {
        var e = pending();
        doThrow(new IllegalStateException("private-email@example.test secret"))
                .when(mail)
                .send(any());
        assertThatThrownBy(() -> emailDeliveryService.sendEmail(e.id))
                .isInstanceOf(RetryableJobException.class)
                .hasMessage("NOTIFICATION_MAIL_UNAVAILABLE");
        assertThat(e.status).isEqualTo(EmailDelivery.Status.PENDING);
        emailDeliveryService.markEmailFailed(e.id);
        assertThat(e.lastError).isEqualTo("MAIL_RETRIES_EXHAUSTED");
        assertThat(e.attempts).isGreaterThanOrEqualTo(5);
    }

    @Test
    void disabledAccountIsSkipped() {
        var e = pending();
        when(accountLookup.getContact(recipient))
                .thenReturn(
                        Optional.of(
                                new AccountContact(
                                        recipient,
                                        "t@example.test",
                                        "x",
                                        Role.STUDENT,
                                        AccountStatus.DISABLED)));
        emailDeliveryService.sendEmail(e.id);
        assertThat(e.status).isEqualTo(EmailDelivery.Status.SKIPPED);
        verifyNoInteractions(mail);
    }

    @Test
    void staleReservationRechecksNewDayBudget() {
        var e = pending();
        e.scheduledFor = now.minus(Duration.ofDays(1));
        when(notificationRepository.countReservedEmails(any(), any())).thenReturn(300L);
        emailDeliveryService.sendEmail(e.id);
        assertThat(e.status).isEqualTo(EmailDelivery.Status.DEFERRED);
        verifyNoInteractions(mail);
    }

    @Test
    void deferredExpiredReminderSkippedAndPriorityOrderRetained() {
        var expired = pending();
        expired.status = EmailDelivery.Status.DEFERRED;
        expired.expiresAt = now;
        var backup = new EmailDelivery();
        backup.id = UUID.randomUUID();
        backup.status = EmailDelivery.Status.DEFERRED;
        backup.scheduledFor = now;
        when(notificationRepository.lockEmailDelivery(backup.id)).thenReturn(Optional.of(backup));
        when(notificationRepository.listDeferredEmails(now)).thenReturn(List.of(expired, backup));
        emailDeliveryService.releaseDeferredEmails(now);
        assertThat(expired.status).isEqualTo(EmailDelivery.Status.SKIPPED);
        assertThat(backup.status).isEqualTo(EmailDelivery.Status.PENDING);
        verify(job)
                .enqueue(anyString(), eq(Map.of("deliveryId", backup.id.toString())), anyString());
        assertThat(EmailCapPolicy.PRIORITY.get("BACKUP_FAILED"))
                .isGreaterThan(EmailCapPolicy.PRIORITY.get("DEADLINE_REMINDER"));
    }

    @Test
    void sweeperUsesBoundedPendingAndHandlerUsesEmailQueue() {
        var e = pending();
        when(notificationRepository.listPendingEmails(now)).thenReturn(List.of(e));
        var handler = new EmailSendJobHandler(emailDeliveryService, notificationRepository);
        assertThat(handler.queue()).isEqualTo(JobQueue.EMAIL);
        assertThat(handler.findStalePending(now))
                .extracting(PendingJob::idempotencyKey)
                .containsExactly("email:" + e.id);
    }

    @Test
    void onlyRemainingBudgetGoesToHighestPriorityDeferredEmail() {
        var low = pending();
        low.status = EmailDelivery.Status.DEFERRED;
        low.priority = 1;
        var high = new EmailDelivery();
        high.id = UUID.randomUUID();
        high.status = EmailDelivery.Status.DEFERRED;
        high.priority = 6;
        when(notificationRepository.lockEmailDelivery(high.id)).thenReturn(Optional.of(high));
        when(notificationRepository.listDeferredEmails(now)).thenReturn(List.of(low, high));
        when(notificationRepository.countReservedEmails(any(), any())).thenReturn(299L, 300L);
        emailDeliveryService.releaseDeferredEmails(now);
        assertThat(high.status).isEqualTo(EmailDelivery.Status.PENDING);
        assertThat(low.status).isEqualTo(EmailDelivery.Status.DEFERRED);
        verify(job)
                .enqueue(anyString(), eq(Map.of("deliveryId", high.id.toString())), anyString());
    }

    @Test
    void missingOrRetiredReminderSourceIsSkippedBeforeSmtp() {
        var e = pending();
        e.type = "DEADLINE_REMINDER";
        e.expiresAt = now.plusSeconds(1000);
        var n = notificationRepository.findNotification(e.notificationId).orElseThrow();
        n.link = "/assignments/" + UUID.randomUUID();
        emailDeliveryService.sendEmail(e.id);
        assertThat(e.status).isEqualTo(EmailDelivery.Status.SKIPPED);
        verifyNoInteractions(mail);
    }

    @Test
    void invalidCapAndBaseUrlFailStartup() {
        assertThatThrownBy(() -> new EmailCapPolicy(notificationRepository, -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                new EmailDeliveryService(
                                        notificationRepository,
                                        emailCapPolicy,
                                        job,
                                        accountLookup,
                                        mail,
                                        clock,
                                        mock(IAssignmentQuery.class),
                                        "javascript:alert(1)"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
