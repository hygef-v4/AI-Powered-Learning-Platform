package edu.aiplatform.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.json.JsonMapper;
import edu.aiplatform.ai.application.IProviderGateway.FailureKind;
import edu.aiplatform.ai.application.IProviderGateway.ProviderException;
import edu.aiplatform.ai.application.IProviderGateway.Response;
import edu.aiplatform.ai.application.IProviderGateway.Usage;
import edu.aiplatform.ai.contract.AiPrompt;
import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.ai.contract.AiTaskFailure;
import edu.aiplatform.ai.contract.AiTaskRequest;
import edu.aiplatform.ai.contract.AiTaskResult;
import edu.aiplatform.ai.contract.AiTaskState;
import edu.aiplatform.ai.contract.AiTaskView;
import edu.aiplatform.ai.contract.ApplyOutcome;
import edu.aiplatform.ai.contract.IAiTaskResultHandler;
import edu.aiplatform.ai.contract.UsageMeasurement;
import edu.aiplatform.ai.domain.AiConnection;
import edu.aiplatform.ai.domain.AiConnectionRepository;
import edu.aiplatform.ai.domain.AiTask;
import edu.aiplatform.ai.domain.AiUsageRecord;
import edu.aiplatform.ai.domain.AiUsageRepository;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.AccountContact;
import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.jobs.contract.IJob;
import edu.aiplatform.jobs.contract.JobMessage;
import edu.aiplatform.jobs.contract.RetryableJobException;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class AiTaskFlowTest {

    static final Instant NOW = Instant.parse("2026-10-21T03:00:00Z");
    static final String SCHEMA = "{\"type\":\"object\",\"required\":[\"score\"],\"properties\":{\"score\":{\"type\":\"number\"}}}";
    private final JsonMapper json = JsonMapper.builder().findAndAddModules().build();
    private final FakeAiTaskRepository tasks = new FakeAiTaskRepository();
    private final AiConnectionRepository connections = mock(AiConnectionRepository.class);
    private final AiUsageRepository usageRepository = mock(AiUsageRepository.class);
    private final QuotaReservationService quota = mock(QuotaReservationService.class);
    private final IProviderGateway gateway = mock(IProviderGateway.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final IAccountLookup accounts = mock(IAccountLookup.class);
    private final IJob jobs = mock(IJob.class);
    private final IAiTaskResultHandler handler = mock(IAiTaskResultHandler.class);
    private final PlatformTransactionManager tx = mock(PlatformTransactionManager.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final AiProperties properties = KeyCipherAndCatalogTest.properties(KeyCipherAndCatalogTest.KEY_V1, null);
    private final KeyCipher cipher = new KeyCipher(properties);
    private final Actor teacher = new Actor(UUID.randomUUID(), Role.TEACHER);
    private final Actor student = new Actor(UUID.randomUUID(), Role.STUDENT);
    private AiTaskService service;
    private AiTaskJobHandler worker;
    private AiConnection shared;
    private AiConnection personal;

    @BeforeEach
    void setUp() {
        when(tx.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(handler.taskType()).thenReturn("GRADING_PROPOSAL");
        AiTaskResultHandlerRegistry registry = AiTaskResultHandlerRegistry.of(List.of(handler));
        AiAccess access = new AiAccess(authorization, mock(IAudit.class), clock);
        AiTaskDelivery delivery = new AiTaskDelivery(tasks, registry, accounts, tx);
        JsonSchemaChecker schemas = new JsonSchemaChecker(json);
        service = new AiTaskService(tasks, connections, usageRepository, registry, quota, new ProviderCatalog(properties),
                schemas, access, delivery, jobs, json, tx, clock);
        worker = new AiTaskJobHandler(tasks, connections, gateway, cipher, schemas, quota,
                new AiUsageService(usageRepository, access, clock), delivery, properties, json, tx, clock);
        when(authorization.authorize(eq(teacher), any(), any())).thenReturn(AuthorizationDecision.allow());
        when(authorization.authorize(eq(student), eq(Actions.AI_PERSONAL), any())).thenReturn(AuthorizationDecision.allow());
        when(authorization.authorize(eq(student), eq(Actions.AI_SHARED_USE), any()))
                .thenReturn(AuthorizationDecision.deny("ROLE"));
        shared = AiConnection.create(UUID.randomUUID(), AiConnection.Scope.INSTITUTIONAL, null, AiProvider.GEMINI,
                "gemini-2.5-flash", cipher.encrypt("school-key-1234"), "1234", NOW);
        personal = AiConnection.create(UUID.randomUUID(), AiConnection.Scope.PERSONAL, teacher.accountId(),
                AiProvider.OPENAI, "gpt-4.1-mini", cipher.encrypt("my-key-abcd"), "abcd", NOW);
        when(connections.findInstitutional(any())).thenReturn(Optional.empty());
        when(connections.findInstitutional(AiProvider.GEMINI)).thenReturn(Optional.of(shared));
        when(connections.findPersonal(any(), any())).thenReturn(Optional.empty());
        when(connections.findPersonal(teacher.accountId(), AiProvider.OPENAI)).thenReturn(Optional.of(personal));
        when(connections.findById(shared.id())).thenReturn(Optional.of(shared));
        when(connections.findById(personal.id())).thenReturn(Optional.of(personal));
        when(quota.reserve(any(), anyLong())).thenAnswer(c -> new QuotaReservationService.Reservation(LocalDate.of(2026, 10, 21),
                c.getArgument(1)));
        when(accounts.getContact(teacher.accountId())).thenReturn(Optional.of(new AccountContact(teacher.accountId(),
                "gv@fpt.edu.vn", "GV", Role.TEACHER, AccountStatus.ACTIVE)));
        when(handler.onReady(any())).thenReturn(ApplyOutcome.APPLIED);
    }

    private AiTaskRequest request(Actor actor, AiSource source, AiProvider provider, String ref) {
        return new AiTaskRequest(actor, source, provider, "GRADING_PROPOSAL", "evaluation:1", 3, ref,
                new AiPrompt("Bạn là trợ lý chấm", "Bài làm".repeat(10)), SCHEMA, 500);
    }

    private JobMessage message(UUID taskId) {
        return new JobMessage(1, "AI_TASK", taskId.toString(), Map.of("taskId", taskId.toString()), 1, null);
    }

    // ---- gửi task -----------------------------------------------------------------------------------------

    @Test
    void sharedSourceReservesQuotaAndQueuesAJob() {
        AiTaskView view = service.submit(request(teacher, AiSource.SHARED, null, "r1"));

        assertThat(view.state()).isEqualTo(AiTaskState.QUEUED);
        assertThat(view.provider()).isEqualTo(AiProvider.GEMINI);
        verify(quota).reserve(teacher.accountId(), QuotaReservationService.estimate(
                "Bạn là trợ lý chấm".length() + "Bài làm".repeat(10).length(), 500));
        verify(jobs).enqueue(eq("AI_TASK"), eq(Map.of("taskId", view.taskId().toString())), anyString());
    }

    @Test
    void studentCannotUseSharedAndNothingFallsBackToPersonal() {
        assertThatThrownBy(() -> service.submit(request(student, AiSource.SHARED, null, "r1")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("AI_SHARED_NOT_ALLOWED"));
        when(connections.findInstitutional(AiProvider.GEMINI)).thenReturn(Optional.of(shared.withStatus(
                AiConnection.Status.DISABLED, NOW)));
        assertThatThrownBy(() -> service.submit(request(teacher, AiSource.SHARED, null, "r2")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("AI_SHARED_NOT_CONFIGURED"));
        assertThatThrownBy(() -> service.submit(request(teacher, AiSource.PERSONAL, AiProvider.CLAUDE, "r3")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("AI_PERSONAL_NOT_CONFIGURED"));
        assertThat(tasks.rows).isEmpty();
        verify(jobs, never()).enqueue(any(), any(), any());
    }

    @Test
    void sameRequestRefReturnsTheTaskAndDifferentFingerprintConflicts() {
        AiTaskView first = service.submit(request(teacher, AiSource.PERSONAL, AiProvider.OPENAI, "dup"));
        AiTaskView again = service.submit(request(teacher, AiSource.PERSONAL, AiProvider.OPENAI, "dup"));
        assertThat(again.taskId()).isEqualTo(first.taskId());
        assertThatThrownBy(() -> service.submit(request(teacher, AiSource.SHARED, null, "dup")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT));
        verify(quota, never()).reserve(any(), anyLong());
    }

    @Test
    void atMostThreeActiveTasksPerAccount() {
        for (int i = 0; i < 3; i++) {
            service.submit(request(teacher, AiSource.PERSONAL, AiProvider.OPENAI, "r" + i));
        }
        assertThatThrownBy(() -> service.submit(request(teacher, AiSource.PERSONAL, AiProvider.OPENAI, "r4")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("AI_TOO_MANY_TASKS"));
    }

    @Test
    void unknownTaskTypeAndBadSchemaAreRejected() {
        AiTaskRequest unknown = new AiTaskRequest(teacher, AiSource.PERSONAL, AiProvider.OPENAI, "NOPE", "x", 1, "r",
                new AiPrompt(null, "u"), SCHEMA, 10);
        assertThatThrownBy(() -> service.submit(unknown))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("AI_TASK_TYPE_UNKNOWN"));
        AiTaskRequest badSchema = new AiTaskRequest(teacher, AiSource.PERSONAL, AiProvider.OPENAI, "GRADING_PROPOSAL", "x",
                1, "r", new AiPrompt(null, "u"), "{not json", 10);
        assertThatThrownBy(() -> service.submit(badSchema)).isInstanceOf(ApiException.class);
    }

    @Test
    void cancelReleasesReservationAndTellsTheHandler() {
        AiTaskView view = service.submit(request(teacher, AiSource.SHARED, null, "r1"));

        assertThatThrownBy(() -> service.cancel(student, view.taskId())).isInstanceOf(ApiException.class);
        service.cancel(teacher, view.taskId());

        AiTask task = tasks.rows.get(view.taskId());
        assertThat(task.state()).isEqualTo(AiTaskState.CANCELLED);
        assertThat(task.handlerDone()).isTrue();
        verify(quota).release(teacher.accountId(), task.quotaDay(), task.reservedTokens());
        verify(handler).onFailed(any(AiTaskFailure.class));
    }

    // ---- worker -------------------------------------------------------------------------------------------

    @Test
    void workerMarksDispatchBeforeCallingProviderThenSettlesActualTokens() {
        AiTaskView view = service.submit(request(teacher, AiSource.SHARED, null, "r1"));
        doAnswer(c -> {
            assertThat(tasks.rows.get(view.taskId()).state()).as("đã ghi mốc gửi").isEqualTo(AiTaskState.RUNNING);
            assertThat(tasks.rows.get(view.taskId()).dispatchedAt()).isNotNull();
            return new Response("{\"score\":8}", "req-1", new Usage(300L, 40L, null, null));
        }).when(gateway).call(eq(AiProvider.GEMINI), any());

        worker.handle(message(view.taskId()));

        AiTask task = tasks.rows.get(view.taskId());
        assertThat(task.state()).isEqualTo(AiTaskState.READY);
        assertThat(task.applyResult()).isEqualTo(ApplyOutcome.APPLIED);
        assertThat(task.inputPayload()).as("prompt đã xóa").isNull();
        assertThat(task.resultPayload()).isNull();
        verify(quota).settle(teacher.accountId(), task.quotaDay(), task.reservedTokens(), 340);
        ArgumentCaptor<AiTaskResult> result = ArgumentCaptor.forClass(AiTaskResult.class);
        verify(handler).onReady(result.capture());
        assertThat(result.getValue().output()).isEqualTo("{\"score\":8}");
        assertThat(result.getValue().actor().role()).isEqualTo(Role.TEACHER);
        ArgumentCaptor<AiUsageRecord> usage = ArgumentCaptor.forClass(AiUsageRecord.class);
        verify(usageRepository).insert(usage.capture());
        assertThat(usage.getValue().provenance()).isEqualTo(UsageMeasurement.PROVIDER_MEASURED);
    }

    @Test
    void sharedApiAllowsTwoConcurrentCallsPerProviderAndThirdWaitsStillQueued() throws Exception {
        AiTaskView first = service.submit(request(teacher, AiSource.SHARED, null, "c1"));
        AiTaskView second = service.submit(request(teacher, AiSource.SHARED, null, "c2"));
        AiTaskView third = service.submit(request(teacher, AiSource.SHARED, null, "c3"));
        java.util.concurrent.CountDownLatch inside = new java.util.concurrent.CountDownLatch(2);
        java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        doAnswer(c -> {
            inside.countDown();
            release.await(5, java.util.concurrent.TimeUnit.SECONDS);
            return new Response("{\"score\":8}", "req", new Usage(10L, 5L, null, null));
        }).when(gateway).call(eq(AiProvider.GEMINI), any());
        java.util.List<Throwable> errors = new java.util.concurrent.CopyOnWriteArrayList<>();
        Thread a = new Thread(() -> run(first.taskId(), errors));
        Thread b = new Thread(() -> run(second.taskId(), errors));
        a.start();
        b.start();
        boolean both = inside.await(5, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(errors).as("lỗi của luồng").isEmpty();
        assertThat(both).as("hai lời gọi đang chạy").isTrue();

        assertThatThrownBy(() -> worker.handle(message(third.taskId()))).isInstanceOf(RetryableJobException.class);
        assertThat(tasks.rows.get(third.taskId()).state()).as("chưa ghi mốc gửi").isEqualTo(AiTaskState.QUEUED);

        release.countDown();
        a.join(5000);
        b.join(5000);
        worker.handle(message(third.taskId()));
        assertThat(tasks.rows.get(third.taskId()).state()).isEqualTo(AiTaskState.READY);
    }

    private void run(UUID taskId, List<Throwable> errors) {
        try {
            worker.handle(message(taskId));
        } catch (Throwable t) {
            errors.add(t);
        }
    }

    @Test
    void redeliveredMessageAfterDispatchBecomesUnknownWithoutSecondCall() {
        AiTaskView view = service.submit(request(teacher, AiSource.SHARED, null, "r1"));
        AiTask queued = tasks.rows.get(view.taskId());
        tasks.update(queued.moveTo(AiTaskState.RUNNING, NOW));

        worker.handle(message(view.taskId()));

        verify(gateway, never()).call(any(), any());
        AiTask task = tasks.rows.get(view.taskId());
        assertThat(task.state()).isEqualTo(AiTaskState.UNKNOWN);
        verify(quota).chargeReserved(teacher.accountId(), task.quotaDay(), task.reservedTokens());
        ArgumentCaptor<AiUsageRecord> usage = ArgumentCaptor.forClass(AiUsageRecord.class);
        verify(usageRepository).insert(usage.capture());
        assertThat(usage.getValue().provenance()).isEqualTo(UsageMeasurement.ESTIMATED);
        verify(handler).onFailed(any());
    }

    @Test
    void outputThatBreaksTheSchemaFailsButTokensAreCharged() {
        AiTaskView view = service.submit(request(teacher, AiSource.SHARED, null, "r1"));
        when(gateway.call(any(), any())).thenReturn(new Response("{\"note\":\"thiếu score\"}", "req", new Usage(10L, 5L, null, null)));

        worker.handle(message(view.taskId()));

        AiTask task = tasks.rows.get(view.taskId());
        assertThat(task.state()).isEqualTo(AiTaskState.FAILED);
        assertThat(task.safeError()).contains("định dạng");
        verify(quota).settle(teacher.accountId(), task.quotaDay(), task.reservedTokens(), 15);
        verify(handler, never()).onReady(any());
    }

    @Test
    void rejectedCallReleasesQuotaAndUnknownCallChargesEstimate() {
        AiTaskView rejected = service.submit(request(teacher, AiSource.SHARED, null, "r1"));
        when(gateway.call(any(), any())).thenThrow(new ProviderException(FailureKind.REJECTED, 401, "Key API không hợp lệ", null));
        worker.handle(message(rejected.taskId()));
        AiTask first = tasks.rows.get(rejected.taskId());
        assertThat(first.state()).isEqualTo(AiTaskState.FAILED);
        verify(quota).release(teacher.accountId(), first.quotaDay(), first.reservedTokens());

        AiTaskView unknown = service.submit(request(teacher, AiSource.SHARED, null, "r2"));
        doThrow(new ProviderException(FailureKind.UNKNOWN, 0, "Hết giờ", null)).when(gateway).call(any(), any());
        worker.handle(message(unknown.taskId()));
        AiTask second = tasks.rows.get(unknown.taskId());
        assertThat(second.state()).isEqualTo(AiTaskState.UNKNOWN);
        verify(quota).chargeReserved(teacher.accountId(), second.quotaDay(), second.reservedTokens());
    }

    @Test
    void handlerFailureRetriesOnlyTheHandler() {
        AiTaskView view = service.submit(request(teacher, AiSource.PERSONAL, AiProvider.OPENAI, "r1"));
        when(gateway.call(any(), any())).thenReturn(new Response("{\"score\":6}", "req", new Usage(1L, 1L, null, null)));
        when(handler.onReady(any())).thenThrow(new IllegalStateException("U09 tạm lỗi")).thenReturn(ApplyOutcome.STALE);

        assertThatThrownBy(() -> worker.handle(message(view.taskId()))).isInstanceOf(RetryableJobException.class);
        assertThat(tasks.rows.get(view.taskId()).handlerDone()).isFalse();
        worker.handle(message(view.taskId()));

        verify(gateway, times(1)).call(any(), any());
        verify(usageRepository, times(1)).insert(any());
        assertThat(tasks.rows.get(view.taskId()).applyResult()).isEqualTo(ApplyOutcome.STALE);
        verify(quota, never()).reserve(any(), anyLong());
    }

    @Test
    void transientAccountLookupFailureDuringDeliveryRetriesInsteadOfPermanentlyRejecting() {
        AiTaskView view = service.submit(request(teacher, AiSource.PERSONAL, AiProvider.OPENAI, "r-transient-account"));
        when(gateway.call(any(), any())).thenReturn(new Response("{\"score\":7}", "req", new Usage(2L, 3L, null, null)));
        when(accounts.getContact(teacher.accountId()))
                .thenThrow(new IllegalStateException("DB tạm mất kết nối"))
                .thenReturn(Optional.of(new AccountContact(teacher.accountId(),
                        "gv@fpt.edu.vn", "GV", Role.TEACHER, AccountStatus.ACTIVE)));

        assertThatThrownBy(() -> worker.handle(message(view.taskId()))).isInstanceOf(RetryableJobException.class);
        assertThat(tasks.rows.get(view.taskId()).handlerDone()).isFalse();
        worker.handle(message(view.taskId()));

        verify(gateway, times(1)).call(any(), any());
        verify(usageRepository, times(1)).insert(any());
        assertThat(tasks.rows.get(view.taskId()).handlerDone()).isTrue();
        assertThat(tasks.rows.get(view.taskId()).applyResult()).isEqualTo(ApplyOutcome.APPLIED);
    }

    @Test
    void disabledRequesterAccountRejectsDeliveryAndNotifiesHandlerWithInputVersion() {
        AiTaskView view = service.submit(request(teacher, AiSource.PERSONAL, AiProvider.OPENAI, "r-disabled-account"));
        when(gateway.call(any(), any())).thenReturn(new Response("{\"score\":7}", "req", new Usage(2L, 3L, null, null)));
        when(accounts.getContact(teacher.accountId())).thenReturn(Optional.of(new AccountContact(teacher.accountId(),
                "gv@fpt.edu.vn", "GV", Role.TEACHER, AccountStatus.DISABLED)));

        worker.handle(message(view.taskId()));

        AiTask task = tasks.rows.get(view.taskId());
        assertThat(task.handlerDone()).isTrue();
        assertThat(task.applyResult()).isEqualTo(ApplyOutcome.REJECTED);
        verify(handler, never()).onReady(any());
        ArgumentCaptor<AiTaskFailure> failure = ArgumentCaptor.forClass(AiTaskFailure.class);
        verify(handler).onFailed(failure.capture());
        assertThat(failure.getValue().inputVersion()).isEqualTo(3L);
        assertThat(failure.getValue().state()).isEqualTo(AiTaskState.READY);
    }

    @Test
    void duplicateDeliveryIsIdempotentAndCallsHandlerOnce() {
        AiTaskView view = service.submit(request(teacher, AiSource.PERSONAL, AiProvider.OPENAI, "r-dup-delivery"));
        when(gateway.call(any(), any())).thenReturn(new Response("{\"score\":9}", "req", new Usage(4L, 5L, null, null)));

        worker.handle(message(view.taskId()));
        worker.handle(message(view.taskId()));

        verify(gateway, times(1)).call(any(), any());
        verify(handler, times(1)).onReady(any());
        verify(usageRepository, times(1)).insert(any());
        assertThat(tasks.rows.get(view.taskId()).applyResult()).isEqualTo(ApplyOutcome.APPLIED);
    }

    @Test
    void disabledConnectionFailsBeforeDispatch() {
        AiTaskView view = service.submit(request(teacher, AiSource.SHARED, null, "r1"));
        when(connections.findById(shared.id())).thenReturn(Optional.of(shared.withStatus(AiConnection.Status.DISABLED, NOW)));

        worker.handle(message(view.taskId()));

        verify(gateway, never()).call(any(), any());
        assertThat(tasks.rows.get(view.taskId()).state()).isEqualTo(AiTaskState.FAILED);
        verify(quota).release(any(), any(), anyLong());
    }

    @Test
    void scannerCancelsTasksQueuedTooLong() {
        AiTaskView view = service.submit(request(teacher, AiSource.SHARED, null, "r1"));

        worker.scan(NOW.plus(Duration.ofMinutes(31)));

        AiTask task = tasks.rows.get(view.taskId());
        assertThat(task.state()).isEqualTo(AiTaskState.CANCELLED);
        verify(quota).release(teacher.accountId(), task.quotaDay(), task.reservedTokens());
        assertThat(worker.findStalePending(NOW.plusSeconds(1))).isEmpty();
    }
}
