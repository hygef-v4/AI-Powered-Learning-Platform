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
    void staffOptInQueuesEmail() {
        when(notificationRepository.findPreferences(recipient))
                .thenReturn(Map.of("QUESTION_REVIEWED", true));
        emailDeliveryService.scheduleEmail(notice(), "QUESTION_REVIEWED", null);
        assertThat(captured().status).isEqualTo(EmailDelivery.Status.PENDING);
        verify(job).enqueue(eq(JobTypes.EMAIL_SEND), anyMap(), startsWith("email:"));
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
}
