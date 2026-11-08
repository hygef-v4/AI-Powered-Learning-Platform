package edu.aiplatform.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.contract.AiPrompt;
import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.ai.contract.AiSourceChoice;
import edu.aiplatform.ai.contract.AiTaskRequest;
import edu.aiplatform.ai.contract.AiTaskState;
import edu.aiplatform.ai.contract.AiTaskView;
import edu.aiplatform.ai.contract.AiUsageSummary;
import edu.aiplatform.ai.contract.IAiTask;
import edu.aiplatform.ai.model.AiConnection;
import edu.aiplatform.ai.model.AiConnection.Status;
import edu.aiplatform.ai.repository.AiConnectionRepository;
import edu.aiplatform.ai.model.AiTask;
import edu.aiplatform.ai.repository.AiTaskRepository;
import edu.aiplatform.ai.repository.AiUsageRepository;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.jobs.contract.IJob;
import edu.aiplatform.jobs.contract.JobTypes;
import edu.aiplatform.shared.error.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Cài {@link IAiTask} (F11): nguồn do người dùng chọn, không tự đổi nguồn (BR-U06-10); {@code SHARED} chỉ giảng viên
 * (BR-U06-11); tối đa 3 task đang chờ hoặc chạy (BR-U06-17); {@code requestRef} chống gửi trùng (BR-U06-21); giữ chỗ
 * quota rồi gửi job {@code AI_TASK} sau commit.
 */
@Service
public class AiTaskService implements IAiTask {

    public static final int MAX_ACTIVE = 3;
    static final int MAX_REQUEST_REF = 100;
    static final int MAX_PROMPT_CHARS = 400_000;

    private final AiTaskRepository tasks;
    private final AiConnectionRepository connections;
    private final AiUsageRepository usage;
    private final AiTaskResultHandlerRegistry handlers;
    private final QuotaReservationService quota;
    private final ProviderCatalog catalog;
    private final JsonSchemaChecker schemas;
    private final AiAccess access;
    private final AiTaskDelivery delivery;
    private final IJob jobs;
    private final ObjectMapper json;
    private final TransactionTemplate transactions;
    private final Clock clock;

    public AiTaskService(AiTaskRepository tasks, AiConnectionRepository connections, AiUsageRepository usage,
                         AiTaskResultHandlerRegistry handlers, QuotaReservationService quota, ProviderCatalog catalog,
                         JsonSchemaChecker schemas, AiAccess access, AiTaskDelivery delivery, IJob jobs, ObjectMapper json,
                         PlatformTransactionManager transactionManager, Clock clock) {
        this.tasks = tasks;
        this.connections = connections;
        this.usage = usage;
        this.handlers = handlers;
        this.quota = quota;
        this.catalog = catalog;
        this.schemas = schemas;
        this.access = access;
        this.delivery = delivery;
        this.jobs = jobs;
        this.json = json;
        this.transactions = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    @Override
    @Transactional
    public AiTaskView submit(AiTaskRequest requested) {
        validate(requested);
        String fingerprint = fingerprint(requested);
        Optional<AiTask> existing = tasks.findByRequestRef(requested.requestRef());
        if (existing.isPresent()) {
            return sameOrConflict(existing.get(), fingerprint);
        }
        AiTaskRequest request = resolveSource(requested);
        Actor actor = request.actor();
        AiConnection connection = connectionFor(actor, request.source(), request.provider());
        int max = Math.min(request.maxOutputTokens(), catalog.maxOutputTokens(connection.provider()));
        tasks.lockActor(actor.accountId());
        if (tasks.countActive(actor.accountId()) >= MAX_ACTIVE) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "AI_TOO_MANY_TASKS",
                    "Bạn đang có 3 yêu cầu AI chưa xong; chờ hoặc hủy bớt rồi thử lại");
        }
        String input = prompt(request.prompt());
        Long reserved = null;
        LocalDate day = null;
        if (request.source() == AiSource.SHARED) {
            QuotaReservationService.Reservation reservation = quota.reserve(actor.accountId(),
                    QuotaReservationService.estimate(promptChars(request.prompt()), max));
            reserved = reservation.tokens();
            day = reservation.day();
        }
        AiTask task = new AiTask(UUID.randomUUID(), request.requestRef(), fingerprint, actor.accountId(), request.source(),
                connection.id(), connection.provider(), connection.model(), request.taskType(), request.targetRef(),
                request.inputVersion(), AiTaskState.QUEUED, input, request.outputSchema(), null, max, reserved, day, null,
                null, null, false, null, clock.instant(), null, 0);
        try {
            tasks.insert(task);
        } catch (DuplicateKeyException e) {
            throw ApiException.conflict("AI_REQUEST_REF_CONFLICT", "Yêu cầu này đang được xử lý, hãy tải lại");
        }
        jobs.enqueue(JobTypes.AI_TASK, Map.of("taskId", task.id().toString()), task.id().toString());
        return view(task);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiTaskView> get(UUID taskId) {
        return tasks.findById(taskId).map(this::view);
    }

    /** Xem task của chính mình (API {@code GET /api/v1/ai/tasks/{id}}). */
    @Transactional(readOnly = true)
    public AiTaskView getOwn(Actor actor, UUID taskId) {
        return tasks.findById(taskId).filter(t -> actor != null && t.actorId().equals(actor.accountId()))
                .map(this::view).orElseThrow(AiAccess::notFound);
    }

    /** Người gửi hủy task {@code QUEUED}; giữ chỗ được giải phóng (BR-U06-26). */
    @Override
    public void cancel(Actor actor, UUID taskId) {
        transactions.executeWithoutResult(status -> cancelQueued(actor, taskId));
        delivery.deliver(taskId);
    }

    private void cancelQueued(Actor actor, UUID taskId) {
        AiTask task = tasks.lock(taskId).filter(t -> actor != null && t.actorId().equals(actor.accountId()))
                .orElseThrow(AiAccess::notFound);
        if (task.state() != AiTaskState.QUEUED) {
            throw ApiException.conflict("AI_TASK_NOT_CANCELLABLE", "Yêu cầu đã được gửi tới nhà cung cấp, không hủy được");
        }
        if (task.reservedTokens() != null) {
            quota.release(task.actorId(), task.quotaDay(), task.reservedTokens());
        }
        if (!tasks.update(task.moveTo(AiTaskState.CANCELLED, clock.instant()).withError("Người dùng đã hủy"))) {
            throw ApiException.conflict("AI_TASK_VERSION_CONFLICT", "Yêu cầu vừa thay đổi, hãy tải lại");
        }
    }

    private AiTaskView sameOrConflict(AiTask task, String fingerprint) {
        if (!task.fingerprint().equals(fingerprint)) {
            throw ApiException.conflict("AI_REQUEST_REF_CONFLICT", "Mã yêu cầu đã dùng cho một yêu cầu khác");
        }
        return view(task);
    }

    @Override
    public void requireSource(Actor actor, AiSource source, AiProvider provider) {
        if (actor == null) {
            throw ApiException.badRequest("AI_TASK_INVALID", "Yêu cầu AI thiếu thông tin");
        }
        if (source == null || source == AiSource.PERSONAL && provider == null) {
            if (candidates(actor, source, new AiPrompt(null, "x"), 1).isEmpty()) {
                throw unavailable();
            }
            return;
        }
        connectionFor(actor, source, provider);
    }

    /**
     * Nguồn tự chọn (yêu cầu 2026-11-05) khi bên gọi để trống nguồn ({@code PERSONAL} không kèm provider là chỉ khóa cá
     * nhân): API chung nếu người gọi được dùng và hạn mức còn đủ cho ước tính token của lượt này, không thì khóa cá nhân
     * tốt nhất; lỗi lúc chạy thì worker tự thử các khóa kế tiếp.
     */
    private AiTaskRequest resolveSource(AiTaskRequest request) {
        if (request.source() == AiSource.SHARED || request.provider() != null) {
            return request;
        }
        List<AiSourceChoice> order = candidates(request.actor(), request.source(), request.prompt(), request.maxOutputTokens());
        if (order.isEmpty()) {
            throw unavailable();
        }
        AiSourceChoice choice = order.get(0);
        return new AiTaskRequest(request.actor(), choice.source(), choice.provider(), request.taskType(),
                request.targetRef(), request.inputVersion(), request.requestRef(), request.prompt(), request.outputSchema(),
                request.maxOutputTokens());
    }

    private List<AiSourceChoice> candidates(Actor actor, AiSource only, AiPrompt prompt, int maxOutputTokens) {
        List<AiSourceChoice> order = sourceOrder(actor, prompt, maxOutputTokens);
        return only == null ? order : order.stream().filter(choice -> choice.source() == only).toList();
    }

    private static ApiException unavailable() {
        return ApiException.badRequest("AI_SOURCE_UNAVAILABLE",
                "Không có nguồn AI dùng được: API chung không đủ hạn mức cho lượt này và chưa có khóa API cá nhân đang bật");
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiSourceChoice> sourceOrder(Actor actor, AiPrompt prompt, int maxOutputTokens) {
        List<AiSourceChoice> order = new ArrayList<>();
        if (actor == null || prompt == null || prompt.user() == null) {
            return order;
        }
        if (access.allowed(actor, Actions.AI_SHARED_USE)) {
            Optional<AiConnection> shared = ConnectionOrder.active(connections.listInstitutional(), catalog).stream()
                    .findFirst();
            if (shared.isPresent()) {
                int max = Math.min(maxOutputTokens, catalog.maxOutputTokens(shared.get().provider()));
                QuotaReservationService.Remaining left = quota.remaining(actor.accountId());
                if (left != null && left.limit() > 0
                        && left.left() >= QuotaReservationService.estimate(promptChars(prompt), max)) {
                    order.add(new AiSourceChoice(AiSource.SHARED, null));
                }
            }
        }
        if (access.allowed(actor, Actions.AI_PERSONAL)) {
            ConnectionOrder.active(connections.listPersonal(actor.accountId()), catalog)
                    .forEach(c -> order.add(new AiSourceChoice(AiSource.PERSONAL, c.provider())));
        }
        return order;
    }

    /** Nguồn đúng như người dùng chọn; không có hoặc không dùng được thì báo lỗi, không đổi nguồn (BR-U06-10). */
    private AiConnection connectionFor(Actor actor, AiSource source, AiProvider provider) {
        if (source == AiSource.SHARED) {
            if (!access.allowed(actor, Actions.AI_SHARED_USE)) {
                throw ApiException.forbidden("AI_SHARED_NOT_ALLOWED");
            }
            // Khóa chung tốt nhất theo thứ tự thử: Claude, GPT, Gemini; mô hình tốt trước (yêu cầu 2026-11-05).
            return ConnectionOrder.active(connections.listInstitutional(), catalog).stream().findFirst()
                    .orElseThrow(() -> ApiException.badRequest("AI_SHARED_NOT_CONFIGURED",
                            "API của trường chưa được bật; hãy chọn API cá nhân"));
        }
        if (!access.allowed(actor, Actions.AI_PERSONAL)) {
            throw ApiException.forbidden("AI_PERSONAL_NOT_ALLOWED");
        }
        if (provider == null) {
            throw ApiException.badRequest("AI_PROVIDER_REQUIRED", "Cần chọn nhà cung cấp cho API cá nhân");
        }
        // Khóa cá nhân tốt nhất của provider (một provider có thể có nhiều khóa).
        return ConnectionOrder.active(connections.listPersonal(actor.accountId()), catalog).stream()
                .filter(c -> c.provider() == provider).findFirst()
                .filter(c -> c.status() == Status.ACTIVE)
                .orElseThrow(() -> ApiException.badRequest("AI_PERSONAL_NOT_CONFIGURED",
                        "Bạn chưa lưu khóa API cho nhà cung cấp này"));
    }

    private void validate(AiTaskRequest request) {
        if (request == null || request.actor() == null || request.prompt() == null
                || request.prompt().user() == null || request.taskType() == null || request.targetRef() == null) {
            throw ApiException.badRequest("AI_TASK_INVALID", "Yêu cầu AI thiếu thông tin");
        }
        if (request.requestRef() == null || request.requestRef().isBlank() || request.requestRef().length() > MAX_REQUEST_REF) {
            throw ApiException.badRequest("AI_REQUEST_REF_INVALID", "Mã yêu cầu không hợp lệ");
        }
        if (handlers.find(request.taskType()).isEmpty()) {
            throw ApiException.badRequest("AI_TASK_TYPE_UNKNOWN", "Loại yêu cầu AI chưa được hỗ trợ");
        }
        if (request.maxOutputTokens() <= 0) {
            throw ApiException.badRequest("AI_TASK_INVALID", "Số token ra phải lớn hơn 0");
        }
        if (request.outputSchema() == null || !schemas.validSchema(request.outputSchema())) {
            throw ApiException.badRequest("AI_TASK_INVALID", "Schema kết quả không hợp lệ");
        }
        if (promptChars(request.prompt()) > MAX_PROMPT_CHARS) {
            throw ApiException.badRequest("AI_PROMPT_TOO_LARGE", "Nội dung gửi AI quá dài");
        }
    }

    private static int promptChars(AiPrompt prompt) {
        return (prompt.system() == null ? 0 : prompt.system().length()) + prompt.user().length();
    }

    private String prompt(AiPrompt prompt) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("system", prompt.system());
        body.put("user", prompt.user());
        try {
            return json.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không ghi được prompt", e);
        }
    }

    /** Băm của người gọi, loại task, đích, {@code inputVersion}, nguồn (BR-U06-21). */
    static String fingerprint(AiTaskRequest r) {
        String raw = r.actor().accountId() + "|" + r.taskType() + "|" + r.targetRef() + "|" + r.inputVersion() + "|"
                + r.source() + "|" + (r.provider() == null ? "" : r.provider());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    AiTaskView view(AiTask task) {
        AiUsageSummary summary = usage.summaryOfTask(task.id()).orElse(null);
        return new AiTaskView(task.id(), task.state(), task.source(), task.provider(), task.model(), task.applyResult(),
                task.safeError(), summary);
    }
}
