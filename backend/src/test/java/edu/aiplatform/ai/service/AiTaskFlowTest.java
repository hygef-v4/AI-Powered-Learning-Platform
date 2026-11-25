package edu.aiplatform.ai.service;

import edu.aiplatform.ai.config.AiProperties;

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
import edu.aiplatform.ai.service.IProviderGateway.FailureKind;
import edu.aiplatform.ai.service.IProviderGateway.ProviderException;
import edu.aiplatform.ai.service.IProviderGateway.Response;
import edu.aiplatform.ai.service.IProviderGateway.Usage;
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
import edu.aiplatform.ai.contract.IPromptGuardrail;
import edu.aiplatform.ai.contract.PromptAction;
import edu.aiplatform.ai.contract.PromptFlowState;
import edu.aiplatform.ai.contract.UserPromptInput;
import edu.aiplatform.ai.contract.UsageMeasurement;
import edu.aiplatform.ai.model.AiConnection;
import edu.aiplatform.ai.repository.AiConnectionRepository;
import edu.aiplatform.ai.repository.AiSourcePreferenceRepository;
import edu.aiplatform.ai.model.AiSourcePreference;
import edu.aiplatform.ai.model.AiTask;
import edu.aiplatform.ai.model.AiUsageRecord;
import edu.aiplatform.ai.repository.AiUsageRepository;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
        AiTaskResultHandlerRegistry registry = AiTaskResultHandlerRegistry.createRegistry(List.of(handler));
        AiAccess access = new AiAccess(authorization, mock(IAudit.class), clock);
        AiTaskDelivery delivery = new AiTaskDelivery(tasks, registry, accounts, tx);
        JsonSchemaChecker schemas = new JsonSchemaChecker(json);
        service = new AiTaskService(tasks, connections, usageRepository, registry, quota, new ProviderCatalog(properties),
                schemas, access, new PromptGuardrailService(), delivery, jobs, json, tx, clock);
        worker = new AiTaskJobHandler(tasks, connections, gateway, cipher, schemas, quota,
                new AiUsageService(usageRepository, access, clock), delivery, properties, json, tx, clock);
        worker.transientWaits = new Duration[] {Duration.ZERO, Duration.ZERO};
        when(authorization.authorize(eq(teacher), any(), any())).thenReturn(AuthorizationDecision.allow());
        when(authorization.authorize(eq(student), eq(Actions.AI_PERSONAL), any())).thenReturn(AuthorizationDecision.allow());
        when(authorization.authorize(eq(student), eq(Actions.AI_SHARED_USE), any()))
                .thenReturn(AuthorizationDecision.deny("ROLE"));
        UUID sharedId = UUID.randomUUID();
        shared = AiConnection.createConnection(sharedId, AiConnection.Scope.INSTITUTIONAL, null, AiProvider.GEMINI,
                "gemini-2.5-flash", cipher.encryptConnectionKey(sharedId, AiConnection.Scope.INSTITUTIONAL, null,
                        AiProvider.GEMINI, "school-key-1234"), "1234", NOW);
        UUID personalId = UUID.randomUUID();
        personal = AiConnection.createConnection(personalId, AiConnection.Scope.PERSONAL, teacher.accountId(),
                AiProvider.OPENAI, "gpt-4.1-mini", cipher.encryptConnectionKey(personalId, AiConnection.Scope.PERSONAL,
                        teacher.accountId(), AiProvider.OPENAI, "my-key-abcd"), "abcd", NOW);
        when(connections.findInstitutional(any())).thenReturn(Optional.empty());
        when(connections.findInstitutional(AiProvider.GEMINI)).thenReturn(Optional.of(shared));
        when(connections.findPersonal(any(), any())).thenReturn(Optional.empty());
        when(connections.findPersonal(teacher.accountId(), AiProvider.OPENAI)).thenReturn(Optional.of(personal));
        when(connections.listInstitutional()).thenReturn(List.of(shared));
        when(connections.listPersonal(teacher.accountId())).thenReturn(List.of(personal));
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

    @Test
    void acceptsTwoHundredCharacterReferenceAndRejectsLongerInput() {
        var task = service.submit(request(teacher, AiSource.PERSONAL, AiProvider.OPENAI, "r".repeat(200)));
        assertThat(task.taskId()).isNotNull();
        assertThatThrownBy(() -> service.submit(request(teacher, AiSource.PERSONAL, AiProvider.OPENAI, "r".repeat(201))))
                .isInstanceOf(ApiException.class);
    }

    // ---- nguồn mặc định (FB-U06-01) -----------------------------------------------------------------------

    private AiSourcePreferenceRepository preferSource(Actor actor, AiSource source, AiProvider provider) {
        AiSourcePreferenceRepository preferences = mock(AiSourcePreferenceRepository.class);
        when(preferences.findPreference(any())).thenReturn(Optional.empty());
        when(preferences.findPreference(actor.accountId()))
                .thenReturn(Optional.of(new AiSourcePreference(source, provider)));
        service.setSourcePreferences(preferences);
        worker.setSourcePreferences(preferences);
        return preferences;
    }

    // ---- guardrail tập trung ------------------------------------------------------------------------------

    private AiTaskRequest withInput(AiTaskRequest r, UserPromptInput input) {
        return new AiTaskRequest(r.actor(), r.source(), r.provider(), r.taskType(), r.targetRef(), r.inputVersion(),
                r.requestRef(), r.prompt(), r.outputSchema(), r.maxOutputTokens(), input);
    }

    @Test
    void requireSourceChecksLikeSubmitWithoutCreatingTask() {
        service.requireSource(teacher, AiSource.PERSONAL, AiProvider.OPENAI);
        assertThatThrownBy(() -> service.requireSource(teacher, AiSource.PERSONAL, AiProvider.CLAUDE))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.readCode()).isEqualTo("AI_PERSONAL_NOT_CONFIGURED"));
        // Không chọn nhà cung cấp: tự chọn khóa cá nhân đang bật (yêu cầu 2026-11-05); không có khóa nào thì báo hết nguồn.
        service.requireSource(teacher, AiSource.PERSONAL, null);
        service.requireSource(teacher, null, null);
        assertThatThrownBy(() -> service.requireSource(student, null, null))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.readCode()).isEqualTo("AI_SOURCE_UNAVAILABLE"));
        assertThatThrownBy(() -> service.requireSource(student, AiSource.SHARED, null))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.readCode()).isEqualTo("AI_SHARED_NOT_ALLOWED"));
        assertThat(tasks.rows).isEmpty();
        verify(quota, never()).reserve(any(), anyLong());
    }

    @Test
    void sharedFallbackWaitsForTheNextProviderAndDoesNotReplayThePreviousCall() throws Exception {
        AiTaskView first = service.submit(request(teacher, AiSource.SHARED, null, "busy-1"));
        AiTaskView second = service.submit(request(teacher, AiSource.SHARED, null, "busy-2"));
        UUID initialId = UUID.randomUUID();
        AiConnection initial = AiConnection.createConnection(initialId, AiConnection.Scope.INSTITUTIONAL, null,
                AiProvider.OPENAI, "gpt-4.1-mini", cipher.encryptConnectionKey(initialId,
                        AiConnection.Scope.INSTITUTIONAL, null, AiProvider.OPENAI, "synthetic-initial-key"), "test", NOW);
        when(connections.listInstitutional()).thenReturn(List.of(initial, shared));
        when(connections.findById(initialId)).thenReturn(Optional.of(initial));
        AiTaskView fallback = service.submit(request(teacher, AiSource.SHARED, null, "fallback"));
        CountDownLatch inside = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        Response response = new Response("{\"score\":8}", "req", new Usage(10L, 5L, null, null));
        doAnswer(call -> {
            if (inside.getCount() > 0) {
                inside.countDown();
                assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
            }
            return response;
        }).when(gateway).call(eq(AiProvider.GEMINI), any());
        when(gateway.call(eq(AiProvider.OPENAI), any()))
                .thenThrow(new ProviderException(FailureKind.REJECTED, 401, "key rejected", null));

        List<Throwable> errors = new CopyOnWriteArrayList<>();
        Thread a = new Thread(() -> run(first.taskId(), errors));
        Thread b = new Thread(() -> run(second.taskId(), errors));
        a.start();
        b.start();
        try {
            assertThat(inside.await(5, TimeUnit.SECONDS)).isTrue();

            assertThatThrownBy(() -> worker.handle(message(fallback.taskId())))
                    .isInstanceOf(RetryableJobException.class);

            AiTask queued = tasks.rows.get(fallback.taskId());
            assertThat(queued.state()).isEqualTo(AiTaskState.QUEUED);
            assertThat(queued.connectionId()).isEqualTo(shared.id());
            assertThat(queued.dispatchedAt()).isNull();
            verify(gateway, times(2)).call(eq(AiProvider.GEMINI), any());
        } finally {
            release.countDown();
            a.join(5000);
            b.join(5000);
        }

        assertThat(errors).isEmpty();
        worker.handle(message(fallback.taskId()));

        assertThat(tasks.rows.get(fallback.taskId()).state()).isEqualTo(AiTaskState.READY);
        verify(gateway).call(eq(AiProvider.OPENAI), any());
        verify(gateway, times(3)).call(eq(AiProvider.GEMINI), any());
    }

    private void run(UUID taskId, List<Throwable> errors) {
        try {
            worker.handle(message(taskId));
        } catch (Throwable t) {
            errors.add(t);
        }
    }

    @Test
    void scannerRetriesCancelledCallbackAfterFailure() {
        AiTaskView view = service.submit(request(teacher, AiSource.SHARED, null, "cancel-retry"));
        doThrow(new IllegalStateException("temporary failure")).doNothing()
                .when(handler).onFailed(any(AiTaskFailure.class));
        AiTask queued = tasks.rows.get(view.taskId());
        tasks.rows.put(view.taskId(), queued.moveTo(AiTaskState.CANCELLED, NOW));

        worker.scan(NOW.plusSeconds(120));
        assertThat(tasks.rows.get(view.taskId()).handlerDone()).isFalse();
        assertThat(tasks.rows.get(view.taskId()).inputPayload()).isNotNull();

        worker.scan(NOW.plusSeconds(180));
        assertThat(tasks.rows.get(view.taskId()).handlerDone()).isTrue();
        assertThat(tasks.rows.get(view.taskId()).inputPayload()).isNull();
        verify(handler, times(2)).onFailed(any(AiTaskFailure.class));
    }

    // ---- ảnh trong prompt (BR-U06-54…59) -------------------------------------------------------------------

    private final Map<String, byte[]> pages = new ConcurrentHashMap<>();

    private void registerPages() {
        edu.aiplatform.ai.contract.IAiImageSource source = new edu.aiplatform.ai.contract.IAiImageSource() {
            @Override
            public String source() {
                return "TEST_PAGES";
            }

            @Override
            public Optional<edu.aiplatform.ai.contract.AiImage> load(String ref) {
                return Optional.ofNullable(pages.get(ref))
                        .map(bytes -> new edu.aiplatform.ai.contract.AiImage("image/jpeg", bytes));
            }
        };
        AiImageSourceRegistry registry = AiImageSourceRegistry.createRegistry(List.of(source));
        service.setImageSources(registry);
        worker.setImageSources(registry);
    }

    private AiTaskRequest imageRequest(String ref, List<edu.aiplatform.ai.contract.AiImageRef> images) {
        return new AiTaskRequest(teacher, AiSource.PERSONAL, AiProvider.OPENAI, "GRADING_PROPOSAL", "evaluation:1", 3,
                ref, new AiPrompt("Chép đề", "Trang 1", images), SCHEMA, 500);
    }

    private static edu.aiplatform.ai.contract.AiImageRef page(String ref) {
        return new edu.aiplatform.ai.contract.AiImageRef("TEST_PAGES", ref, "image/jpeg");
    }
}
