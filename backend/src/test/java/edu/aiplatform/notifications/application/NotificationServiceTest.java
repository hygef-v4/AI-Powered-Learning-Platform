package edu.aiplatform.notifications.application;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import edu.aiplatform.notifications.domain.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.groupdocs.contract.ISseHub;
import edu.aiplatform.audit.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class NotificationServiceTest {
    INotificationRepository repo;EmailDeliveryService emails;ISseHub sse;IAuthorization auth;IAudit audit;NotificationAccess access;NotificationService service;
    UUID account=UUID.randomUUID();Actor actor=new Actor(account,Role.STUDENT);Clock clock=Clock.fixed(Instant.parse("2026-10-21T08:00:00Z"),ZoneOffset.UTC);
    @BeforeEach void setup(){repo=mock(INotificationRepository.class);emails=mock(EmailDeliveryService.class);sse=mock(ISseHub.class);auth=mock(IAuthorization.class);audit=mock(IAudit.class);when(auth.authorize(any(),any(),any())).thenReturn(AuthorizationDecision.allow());access=new NotificationAccess(auth,audit,clock);service=new NotificationService(repo,access,emails,sse,clock);}
    @AfterEach void cleanup(){if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.clearSynchronization();}
    @Test void duplicateEventDoesNotScheduleMailOrSse(){when(repo.insert(any())).thenReturn(false);service.create(UUID.randomUUID(),NotificationType.ENROLLED,List.of(account),"t","s","/classes/1","ENROLLED",null);verifyNoInteractions(emails,sse);}
    @Test void sseOnlyAfterCommitAndOneNoticePerRecipient(){when(repo.insert(any())).thenReturn(true);TransactionSynchronizationManager.initSynchronization();service.create(UUID.randomUUID(),NotificationType.ENROLLED,List.of(account,account),"t","s","/classes/1","ENROLLED",null);verify(repo,times(1)).insert(any());verify(emails).schedule(any(),eq("ENROLLED"),isNull());verifyNoInteractions(sse);TransactionSynchronizationManager.getSynchronizations().forEach(sync->sync.afterCommit());verify(sse).publish(eq(NotificationService.channel(account)),any());}
    @Test void sseFailureDoesNotFailCommittedFanout(){when(repo.insert(any())).thenReturn(true);doThrow(new IllegalStateException()).when(sse).publish(anyString(),any());assertThatCode(()->service.create(UUID.randomUUID(),NotificationType.ANNOUNCEMENT,List.of(account),"t","s","/announcements",null,null)).doesNotThrowAnyException();}
    @Test void foreignReadReturns404AndAudits(){when(repo.read(any(),eq(account),any())).thenReturn(false);assertThatThrownBy(()->service.read(actor,UUID.randomUUID())).isInstanceOf(ApiException.class);verify(audit).record(argThat(e->e.action().equals("ACCESS_DENIED")));}
    @Test void readAllUsesPrincipalOnly(){service.readAll(actor);verify(repo).readAll(eq(account),any());assertThat(service.authorizedChannel(actor)).isEqualTo("notifications:account:"+account);}
    @Test void disabledAccountCannotReadEvenEmptyFeed(){when(auth.authorize(any(),any(),any())).thenReturn(AuthorizationDecision.deny("DISABLED"));assertThatThrownBy(()->service.unread(actor)).isInstanceOf(ApiException.class);verify(repo,never()).unread(any());}
    @Test void preferencesDefaultOnAndWhitelist(){var prefs=new NotificationPreferenceService(repo,access);when(repo.preferences(account)).thenReturn(Map.of());assertThat(prefs.get(actor)).hasSize(5).containsEntry("ANNOUNCEMENT_IMPORTANT",true);assertThatThrownBy(()->prefs.put(actor,Map.of("BACKUP_FAILED",false))).isInstanceOf(ApiException.class);prefs.put(actor,Map.of("ENROLLED",false));verify(repo).preference(account,"ENROLLED",false);}
    @Test void staffCannotSetLearningPreferences(){var prefs=new NotificationPreferenceService(repo,access);assertThatThrownBy(()->prefs.get(new Actor(account,Role.ADMIN))).isInstanceOf(ApiException.class);}
    @Test void unsafeLinksAndOversizePagesRejected(){assertThatThrownBy(()->service.create(UUID.randomUUID(),NotificationType.ENROLLED,List.of(account),"t","s","//external.test",null,null)).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->service.list(actor,false,1,51)).isInstanceOf(ApiException.class);}

    @ParameterizedTest @EnumSource(NotificationType.class)
    void legacyFeedCopyChangesWithoutRewritingHistoryOrReadState(NotificationType type) {
        var n = row(type, "Đăng nhập để xem thông tin liên quan.");
        n.readAt = clock.instant().minusSeconds(60);
        when(repo.list(account, false, 1, 20)).thenReturn(List.of(n));

        var item = service.list(actor, false, 1, 20).items().get(0);

        assertThat(item.summary()).isNotBlank().hasSizeLessThanOrEqualTo(500).doesNotContain("Đăng nhập");
        assertThat(item.id()).isEqualTo(n.id);
        assertThat(item.type()).isEqualTo(type.name());
        assertThat(item.link()).isEqualTo(n.link);
        assertThat(item.readAt()).isEqualTo(n.readAt);
        assertThat(item.createdAt()).isEqualTo(n.createdAt);
        assertThat(n.summary).isEqualTo("Đăng nhập để xem thông tin liên quan.");
        verify(repo, never()).insert(any());
        verifyNoInteractions(emails, sse);
    }

    @Test void specificReminderAndNewCopyArePreservedInFeed() {
        var reminder = row(NotificationType.DEADLINE_REMINDER, "Hạn nộp bài còn khoảng24giờ.");
        var request = row(NotificationType.LEADER_CHANGE_REQUESTED, "Xem yêu cầu để phê duyệt hoặc từ chối.");
        when(repo.list(account, true, 1, 20)).thenReturn(List.of(reminder, request));

        assertThat(service.list(actor, true, 1, 20).items()).extracting(NotificationService.Item::summary)
                .containsExactly(reminder.summary, request.summary);
        assertThat(reminder.readAt).isNull();
        assertThat(request.readAt).isNull();
        verifyNoInteractions(emails, sse);
    }

    private Notification row(NotificationType type, String summary) {
        var n = new Notification();
        n.id = UUID.randomUUID(); n.recipientId = account; n.sourceEventId = UUID.randomUUID();
        n.type = type; n.title = "Existing title"; n.summary = summary;
        n.link = "/classes/" + UUID.randomUUID(); n.createdAt = clock.instant().minusSeconds(120);
        return n;
    }
}
