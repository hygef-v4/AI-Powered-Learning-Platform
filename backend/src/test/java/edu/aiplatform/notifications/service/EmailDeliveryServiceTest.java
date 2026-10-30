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
    INotificationRepository repo;IJob jobs;IAccountLookup accounts;IMail mail;EmailCapPolicy cap;EmailDeliveryService service;
    IAssignmentQuery assignments;
    Instant now=Instant.parse("2026-10-21T16:59:59Z");UUID recipient=UUID.randomUUID();Clock clock=Clock.fixed(now,ZoneOffset.UTC);
    @BeforeEach void setup(){repo=mock(INotificationRepository.class);jobs=mock(IJob.class);accounts=mock(IAccountLookup.class);mail=mock(IMail.class);assignments=mock(IAssignmentQuery.class);cap=new EmailCapPolicy(repo,300);service=new EmailDeliveryService(repo,cap,jobs,accounts,mail,clock,assignments,"https://learning.example.test");when(repo.preferences(any())).thenReturn(Map.of());when(accounts.getContact(recipient)).thenReturn(Optional.of(new AccountContact(recipient,"test@example.test","Student",Role.STUDENT,AccountStatus.ACTIVE)));}
    @Test void reminderEmailAcceptsNewAndLegacyLinksAndStillChecksDeadline() {
        UUID assignmentId=UUID.randomUUID(),classId=UUID.randomUUID();
        var deadline=now.plusSeconds(1000);
        var source=new edu.aiplatform.assignments.contract.AssignmentView(assignmentId,
                edu.aiplatform.assignments.contract.AssignmentScope.CLASS,classId,UUID.randomUUID(),1,"Bài",
                edu.aiplatform.assignments.contract.SubmissionMode.INDIVIDUAL,edu.aiplatform.assignments.contract.GradingMode.GRADED,
                false,edu.aiplatform.assignments.contract.AssignmentStatus.OPEN,now.minusSeconds(1000),deadline,null,null,false,null,List.of());
        when(assignments.get(assignmentId)).thenReturn(Optional.of(source));
        for(String prefix:List.of("/my-assignments/","/assignments/")) {
            var e=pending();e.type="DEADLINE_REMINDER";e.expiresAt=deadline;
            var n=repo.find(e.notificationId).orElseThrow();n.link=prefix+assignmentId+"?classId="+classId;
            service.send(e.id);
            assertThat(e.status).isEqualTo(EmailDelivery.Status.SENT);
            verify(mail).send(argThat(m->m.textBody().contains("https://learning.example.test"+n.link)));
        }
        var stale=pending();stale.type="DEADLINE_REMINDER";stale.expiresAt=deadline.plusSeconds(1);
        repo.find(stale.notificationId).orElseThrow().link="/my-assignments/"+assignmentId+"?classId="+classId;
        service.send(stale.id);
        assertThat(stale.status).isEqualTo(EmailDelivery.Status.SKIPPED);
        verify(mail,times(2)).send(any());
    }
    @Test void malformedReminderLinkIsSkippedWithoutQueryOrRetry() {
        for(String link:List.of("/my-assignments/not-a-uuid","/assignments/"+UUID.randomUUID()+"/extra","https://external.test/assignments/"+UUID.randomUUID())) {
            var e=pending();e.type="DEADLINE_REMINDER";e.expiresAt=now.plusSeconds(1000);
            repo.find(e.notificationId).orElseThrow().link=link;
            assertThatCode(()->service.send(e.id)).doesNotThrowAnyException();
            assertThat(e.status).isEqualTo(EmailDelivery.Status.SKIPPED);
        }
        verifyNoInteractions(assignments,mail);
    }
    @Test void reminderLookupFailureRemainsRetryable() {
        var e=pending();e.type="DEADLINE_REMINDER";e.expiresAt=now.plusSeconds(1000);
        UUID assignmentId=UUID.randomUUID();
        repo.find(e.notificationId).orElseThrow().link="/my-assignments/"+assignmentId+"?classId="+UUID.randomUUID();
        when(assignments.get(assignmentId)).thenThrow(new IllegalStateException("private provider error"));
        assertThatThrownBy(()->service.send(e.id)).isInstanceOf(RetryableJobException.class).hasMessage("NOTIFICATION_ASSIGNMENT_UNAVAILABLE");
        assertThat(e.status).isEqualTo(EmailDelivery.Status.PENDING);
        verifyNoInteractions(mail);
    }
    Notification notice(){var n=new Notification();n.id=UUID.randomUUID();n.recipientId=recipient;n.title="Có điểm mới";n.link="/classes/1";when(repo.find(n.id)).thenReturn(Optional.of(n));return n;}
    EmailDelivery pending(){var n=notice();var e=new EmailDelivery();e.id=UUID.randomUUID();e.notificationId=n.id;e.recipientId=recipient;e.type="GRADE_PUBLISHED";e.status=EmailDelivery.Status.PENDING;e.scheduledFor=now;e.createdAt=now;when(repo.lockEmail(e.id)).thenReturn(Optional.of(e));return e;}
    EmailDelivery captured(){var captor=ArgumentCaptor.forClass(EmailDelivery.class);verify(repo).addEmail(captor.capture());return captor.getValue();}
    @Test void defaultPreferenceQueuesEmail(){service.schedule(notice(),"ENROLLED",null);assertThat(captured().status).isEqualTo(EmailDelivery.Status.PENDING);verify(jobs).enqueue(eq(JobTypes.EMAIL_SEND),anyMap(),startsWith("email:"));}
    @Test void disabledPreferenceStillKeepsNotificationButSkipsEmail(){when(repo.preferences(recipient)).thenReturn(Map.of("ENROLLED",false));service.schedule(notice(),"ENROLLED",null);assertThat(captured().status).isEqualTo(EmailDelivery.Status.SKIPPED);verifyNoInteractions(jobs);}
    @Test void capReachedDefersUntilVietnamMidnight(){when(repo.reserved(any(),any())).thenReturn(300L);service.schedule(notice(),"ENROLLED",null);var e=captured();assertThat(e.status).isEqualTo(EmailDelivery.Status.DEFERRED);assertThat(e.scheduledFor).isEqualTo(Instant.parse("2026-10-21T17:00:00Z"));verifyNoInteractions(jobs);}
    @Test void backupIgnoresLearningPreferences(){when(repo.preferences(recipient)).thenReturn(Map.of("BACKUP_FAILED",false));service.schedule(notice(),"BACKUP_FAILED",null);assertThat(captured().status).isEqualTo(EmailDelivery.Status.PENDING);}
    @Test void expiredReminderSkippedBeforeQueue(){service.schedule(notice(),"DEADLINE_REMINDER",now);assertThat(captured().status).isEqualTo(EmailDelivery.Status.SKIPPED);verifyNoInteractions(jobs);}
    @Test void sendContainsNoGradeOrBodyAndSentIsTerminal(){var e=pending();repo.find(e.notificationId).orElseThrow().summary="9.5 PRIVATE_GRADE_COMMENT";service.send(e.id);assertThat(e.status).isEqualTo(EmailDelivery.Status.SENT);service.send(e.id);service.failed(e.id);verify(mail,times(1)).send(argThat(m->m.htmlBody()==null&&m.textBody().contains("Đăng nhập để xem: https://learning.example.test/classes/1")&&!m.textBody().contains("9.5")&&!m.textBody().contains("PRIVATE_GRADE_COMMENT")));assertThat(e.status).isEqualTo(EmailDelivery.Status.SENT);}
    @Test void smtpFailureIsRetryableAndSanitized(){var e=pending();doThrow(new IllegalStateException("private-email@example.test secret")).when(mail).send(any());assertThatThrownBy(()->service.send(e.id)).isInstanceOf(RetryableJobException.class).hasMessage("NOTIFICATION_MAIL_UNAVAILABLE");assertThat(e.status).isEqualTo(EmailDelivery.Status.PENDING);service.failed(e.id);assertThat(e.lastError).isEqualTo("MAIL_RETRIES_EXHAUSTED");assertThat(e.attempts).isGreaterThanOrEqualTo(5);}
    @Test void disabledAccountIsSkipped(){var e=pending();when(accounts.getContact(recipient)).thenReturn(Optional.of(new AccountContact(recipient,"t@example.test","x",Role.STUDENT,AccountStatus.DISABLED)));service.send(e.id);assertThat(e.status).isEqualTo(EmailDelivery.Status.SKIPPED);verifyNoInteractions(mail);}
    @Test void staleReservationRechecksNewDayBudget(){var e=pending();e.scheduledFor=now.minus(Duration.ofDays(1));when(repo.reserved(any(),any())).thenReturn(300L);service.send(e.id);assertThat(e.status).isEqualTo(EmailDelivery.Status.DEFERRED);verifyNoInteractions(mail);}
    @Test void deferredExpiredReminderSkippedAndPriorityOrderRetained(){var expired=pending();expired.status=EmailDelivery.Status.DEFERRED;expired.expiresAt=now;var backup=new EmailDelivery();backup.id=UUID.randomUUID();backup.status=EmailDelivery.Status.DEFERRED;backup.scheduledFor=now;when(repo.lockEmail(backup.id)).thenReturn(Optional.of(backup));when(repo.deferred(now)).thenReturn(List.of(expired,backup));service.releaseDeferred(now);assertThat(expired.status).isEqualTo(EmailDelivery.Status.SKIPPED);assertThat(backup.status).isEqualTo(EmailDelivery.Status.PENDING);verify(jobs).enqueue(anyString(),eq(Map.of("deliveryId",backup.id.toString())),anyString());assertThat(EmailCapPolicy.PRIORITY.get("BACKUP_FAILED")).isGreaterThan(EmailCapPolicy.PRIORITY.get("DEADLINE_REMINDER"));}
    @Test void sweeperUsesBoundedPendingAndHandlerUsesEmailQueue(){var e=pending();when(repo.pending(now)).thenReturn(List.of(e));var handler=new EmailSendJobHandler(service,repo);assertThat(handler.queue()).isEqualTo(JobQueue.EMAIL);assertThat(handler.findStalePending(now)).extracting(PendingJob::idempotencyKey).containsExactly("email:"+e.id);}
    @Test void onlyRemainingBudgetGoesToHighestPriorityDeferredEmail(){var low=pending();low.status=EmailDelivery.Status.DEFERRED;low.priority=1;var high=new EmailDelivery();high.id=UUID.randomUUID();high.status=EmailDelivery.Status.DEFERRED;high.priority=6;when(repo.lockEmail(high.id)).thenReturn(Optional.of(high));when(repo.deferred(now)).thenReturn(List.of(low,high));when(repo.reserved(any(),any())).thenReturn(299L,300L);service.releaseDeferred(now);assertThat(high.status).isEqualTo(EmailDelivery.Status.PENDING);assertThat(low.status).isEqualTo(EmailDelivery.Status.DEFERRED);verify(jobs).enqueue(anyString(),eq(Map.of("deliveryId",high.id.toString())),anyString());}
    @Test void missingOrRetiredReminderSourceIsSkippedBeforeSmtp(){var e=pending();e.type="DEADLINE_REMINDER";e.expiresAt=now.plusSeconds(1000);var n=repo.find(e.notificationId).orElseThrow();n.link="/assignments/"+UUID.randomUUID();service.send(e.id);assertThat(e.status).isEqualTo(EmailDelivery.Status.SKIPPED);verifyNoInteractions(mail);}
    @Test void invalidCapAndBaseUrlFailStartup(){assertThatThrownBy(()->new EmailCapPolicy(repo,-1)).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->new EmailDeliveryService(repo,cap,jobs,accounts,mail,clock,mock(IAssignmentQuery.class),"javascript:alert(1)")).isInstanceOf(IllegalArgumentException.class);}
}
