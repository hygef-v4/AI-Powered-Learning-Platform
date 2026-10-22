package edu.aiplatform.ai.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.application.IProviderGateway.FailureKind;
import edu.aiplatform.ai.application.IProviderGateway.ProviderException;
import edu.aiplatform.ai.application.IProviderGateway.Request;
import edu.aiplatform.ai.application.IProviderGateway.Response;
import edu.aiplatform.ai.application.IProviderGateway.Usage;
import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.ai.contract.AiTaskState;
import edu.aiplatform.ai.domain.AiConnection;
import edu.aiplatform.ai.domain.AiConnection.Status;
import edu.aiplatform.ai.domain.AiConnectionRepository;
import edu.aiplatform.ai.domain.AiTask;
import edu.aiplatform.ai.domain.AiTaskRepository;
import edu.aiplatform.ai.domain.AiUsageRecord.Source;
import edu.aiplatform.jobs.contract.IJobHandler;
import edu.aiplatform.jobs.contract.IPendingSweeper;
import edu.aiplatform.jobs.contract.IScheduledScanner;
import edu.aiplatform.jobs.contract.JobMessage;
import edu.aiplatform.jobs.contract.JobQueue;
import edu.aiplatform.jobs.contract.JobTypes;
import edu.aiplatform.jobs.contract.PendingJob;
import edu.aiplatform.jobs.contract.RetryableJobException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Chạy task AI trong worker (F12, F13). Ghi mốc gửi và commit {@code RUNNING} trước khi gọi provider; message đến lại
 * thấy mốc gửi mà chưa có kết quả thì {@code UNKNOWN}, không gọi lần hai (BR-U06-22, P2). Kết quả kiểm JSON schema
 * (BR-U06-23); quota quyết toán theo token thật, giải phóng khi provider từ chối, tính phần giữ chỗ khi không rõ
 * (BR-U06-14). Kết quả chuyển cho handler qua {@link AiTaskDelivery} (P3).
 */
@Component
public class AiTaskJobHandler implements IJobHandler, IPendingSweeper, IScheduledScanner {

    static final Duration QUEUE_TIMEOUT = Duration.ofMinutes(30);
    static final Duration STALE_PENDING = Duration.ofMinutes(5);
    static final int MAX_RESULT_CHARS = 1_000_000;
    static final int SHARED_CONCURRENCY = 2;

    private final AiTaskRepository tasks;
    private final AiConnectionRepository connections;
    private final IProviderGateway gateway;
    private final KeyCipher cipher;
    private final JsonSchemaChecker schemas;
    private final QuotaReservationService quota;
    private final AiUsageService usage;
    private final AiTaskDelivery delivery;
    private final AiProperties properties;
    private final ObjectMapper json;
    private final TransactionTemplate transactions;
    private final Clock clock;
    /** API chung: tối đa 2 lời gọi đồng thời mỗi provider trong một worker (NFR-U06-03). */
    private final Map<AiProvider, Semaphore> sharedSlots = new EnumMap<>(AiProvider.class);

    public AiTaskJobHandler(AiTaskRepository tasks, AiConnectionRepository connections, IProviderGateway gateway,
                            KeyCipher cipher, JsonSchemaChecker schemas, QuotaReservationService quota,
                            AiUsageService usage, AiTaskDelivery delivery, AiProperties properties, ObjectMapper json,
                            PlatformTransactionManager transactionManager, Clock clock) {
        this.tasks = tasks;
        this.connections = connections;
        this.gateway = gateway;
        this.cipher = cipher;
        this.schemas = schemas;
        this.quota = quota;
        this.usage = usage;
        this.delivery = delivery;
        this.properties = properties;
        this.json = json;
        this.transactions = new TransactionTemplate(transactionManager);
        this.clock = clock;
        for (AiProvider provider : AiProvider.values()) {
            sharedSlots.put(provider, new Semaphore(SHARED_CONCURRENCY));
        }
    }

    // ---- IJobHandler ----------------------------------------------------------------------------------------

    @Override
    public String jobType() {
        return JobTypes.AI_TASK;
    }

    @Override
    public JobQueue queue() {
        return JobQueue.AI;
    }

    @Override
    public Duration timeout() {
        return properties.requestTimeout().plusSeconds(30);
    }

    @Override
    public void handle(JobMessage message) {
        UUID taskId = UUID.fromString(String.valueOf(message.payload().get("taskId")));
        // Lấy chỗ trước mốc gửi: hết chỗ thì U03 thử lại sau, task vẫn QUEUED nên không bị coi là đã gọi (P2).
        Semaphore slot = sharedSlot(taskId);
        if (slot != null && !slot.tryAcquire()) {
            throw new RetryableJobException("API chung của nhà cung cấp đang đủ lời gọi đồng thời");
        }
        try {
            Dispatch dispatch;
            try {
                dispatch = transactions.execute(status -> start(taskId));
            } catch (DataAccessException e) {
                // Lỗi trước mốc gửi (database tạm lỗi): cho U03 thử lại (P2).
                throw new RetryableJobException("Không bắt đầu được task AI", e);
            }
            if (dispatch != null) {
                Outcome outcome = callProvider(dispatch);
                transactions.executeWithoutResult(status -> finish(taskId, outcome));
            }
        } finally {
            if (slot != null) {
                slot.release();
            }
        }
        deliverOrRetry(taskId);
    }

    /** Semaphore của provider khi task dùng API chung và còn chờ gọi; task cá nhân hay đã xong thì không giới hạn. */
    private Semaphore sharedSlot(UUID taskId) {
        try {
            return tasks.findById(taskId)
                    .filter(t -> t.source() == AiSource.SHARED && t.state() == AiTaskState.QUEUED)
                    .flatMap(t -> t.provider() != null ? Optional.of(t.provider())
                            : Optional.ofNullable(t.connectionId()).flatMap(connections::findById).map(AiConnection::provider))
                    .map(sharedSlots::get).orElse(null);
        } catch (DataAccessException e) {
            throw new RetryableJobException("Không đọc được task AI", e);
        }
    }

    @Override
    public void onFailed(JobMessage message, String error) {
        UUID taskId = UUID.fromString(String.valueOf(message.payload().get("taskId")));
        transactions.executeWithoutResult(status -> tasks.lock(taskId).filter(t -> t.state() == AiTaskState.QUEUED)
                .ifPresent(t -> fail(t, "Không chạy được yêu cầu AI, hãy thử lại", false)));
        delivery.deliver(taskId);
    }

    /** Khóa task; trả dữ liệu để gọi provider khi vừa chuyển {@code RUNNING}, rỗng khi không cần gọi. */
    private Dispatch start(UUID taskId) {
        AiTask task = tasks.lock(taskId).orElse(null);
        if (task == null || task.finished()) {
            return null;
        }
        Instant now = clock.instant();
        if (task.state() == AiTaskState.RUNNING) {
            // Message đến lại sau mốc gửi: không gọi provider lần hai (BR-U06-22).
            unknown(task, "Không rõ kết quả của lần gọi trước");
            return null;
        }
        AiConnection connection = task.connectionId() == null ? null
                : connections.findById(task.connectionId()).orElse(null);
        if (connection == null || connection.status() != Status.ACTIVE) {
            fail(task, "Kết nối AI đã bị tắt hoặc thu hồi", false);
            return null;
        }
        String key = cipher.decrypt(connection.encryptedKey());
        JsonNode prompt = readPrompt(task.inputPayload());
        save(task.moveTo(AiTaskState.RUNNING, now));
        return new Dispatch(task, connection, new Request(task.model(), key, text(prompt, "system"), text(prompt, "user"),
                task.outputSchema(), task.maxOutputTokens(), properties.requestTimeout()));
    }

    private Outcome callProvider(Dispatch dispatch) {
        try {
            Response response = gateway.call(dispatch.connection().provider(), dispatch.request());
            return new Outcome(response, null);
        } catch (ProviderException e) {
            return new Outcome(null, e);
        } catch (RuntimeException e) {
            return new Outcome(null, new ProviderException(FailureKind.UNKNOWN, 0, "Lỗi khi gọi nhà cung cấp AI", null));
        }
    }

    private void finish(UUID taskId, Outcome outcome) {
        AiTask task = tasks.lock(taskId).orElse(null);
        if (task == null || task.state() != AiTaskState.RUNNING) {
            return;
        }
        if (outcome.error() == null) {
            Response response = outcome.response();
            Optional<String> invalid = response.output().length() > MAX_RESULT_CHARS ? Optional.of("quá dài")
                    : schemas.check(task.outputSchema(), response.output());
            settle(task, response.usage(), invalid.isEmpty() ? "READY" : "INVALID_OUTPUT");
            if (invalid.isPresent()) {
                save(task.moveTo(AiTaskState.FAILED, clock.instant()).withResult(null, response.requestId())
                        .withError("Kết quả AI không đúng định dạng yêu cầu"));
            } else {
                save(task.withResult(response.output(), response.requestId()).moveTo(AiTaskState.READY, clock.instant()));
            }
            return;
        }
        ProviderException e = outcome.error();
        switch (e.kind()) {
            case REJECTED -> fail(task, e.getMessage(), true);
            case CHARGED -> {
                settle(task, e.usage(), "FAILED");
                save(task.moveTo(AiTaskState.FAILED, clock.instant()).withError(e.getMessage()));
            }
            case UNKNOWN -> unknown(task, e.getMessage());
        }
    }

    /** Thất bại không tính token: giải phóng giữ chỗ (BR-U06-14). */
    private void fail(AiTask task, String error, boolean recordUsage) {
        if (task.reservedTokens() != null) {
            quota.release(task.actorId(), task.quotaDay(), task.reservedTokens());
        }
        if (recordUsage) {
            usage.recordProvider(task.actorId(), source(task), task.id(), task.provider(), task.model(), task.taskType(),
                    "REJECTED", null, null);
        }
        save(task.moveTo(AiTaskState.FAILED, clock.instant()).withError(error));
    }

    /** Không rõ kết quả: tính phần giữ chỗ là đã dùng với nhãn {@code ESTIMATED} (BR-U06-14). */
    private void unknown(AiTask task, String error) {
        if (task.reservedTokens() != null) {
            quota.chargeReserved(task.actorId(), task.quotaDay(), task.reservedTokens());
        }
        usage.recordProvider(task.actorId(), source(task), task.id(), task.provider(), task.model(), task.taskType(),
                "UNKNOWN", null, task.reservedTokens());
        save(task.moveTo(AiTaskState.UNKNOWN, clock.instant()).withError(error));
    }

    private void settle(AiTask task, Usage measured, String outcome) {
        Long actual = measured == null || (measured.inputTokens() == null && measured.outputTokens() == null) ? null
                : (measured.inputTokens() == null ? 0 : measured.inputTokens())
                        + (measured.outputTokens() == null ? 0 : measured.outputTokens());
        if (task.reservedTokens() != null) {
            if (actual == null) {
                quota.chargeReserved(task.actorId(), task.quotaDay(), task.reservedTokens());
            } else {
                quota.settle(task.actorId(), task.quotaDay(), task.reservedTokens(), actual);
            }
        }
        usage.recordProvider(task.actorId(), source(task), task.id(), task.provider(), task.model(), task.taskType(),
                outcome, measured, actual == null ? task.reservedTokens() : null);
    }

    private void deliverOrRetry(UUID taskId) {
        try {
            delivery.deliver(taskId);
        } catch (RuntimeException e) {
            // Handler lỗi: lượt sau chỉ gọi lại handler (P3).
            throw new RetryableJobException("Handler kết quả AI lỗi", e);
        }
    }

    // ---- IPendingSweeper -------------------------------------------------------------------------------------

    @Override
    public List<PendingJob> findStalePending(Instant olderThan) {
        return tasks.listByStateBefore(AiTaskState.QUEUED, olderThan).stream()
                .map(t -> new PendingJob(t.id().toString(), Map.of("taskId", t.id().toString())))
                .toList();
    }

    // ---- IScheduledScanner -----------------------------------------------------------------------------------

    @Override
    public String name() {
        return "ai-task-timeout";
    }

    /** Chờ quá 30 phút thì hủy; chạy quá hạn gọi thì {@code UNKNOWN}; task kết thúc mà handler chưa nhận thì giao lại. */
    @Override
    public void scan(Instant now) {
        for (AiTask queued : tasks.listByStateBefore(AiTaskState.QUEUED, now.minus(QUEUE_TIMEOUT))) {
            transactions.executeWithoutResult(status -> tasks.lock(queued.id())
                    .filter(t -> t.state() == AiTaskState.QUEUED).ifPresent(t -> {
                        if (t.reservedTokens() != null) {
                            quota.release(t.actorId(), t.quotaDay(), t.reservedTokens());
                        }
                        save(t.moveTo(AiTaskState.CANCELLED, clock.instant()).withError("Chờ quá 30 phút, đã hủy"));
                    }));
            quietDeliver(queued.id());
        }
        Instant runningDeadline = now.minus(properties.requestTimeout()).minus(Duration.ofMinutes(5));
        for (AiTask running : tasks.listByStateBefore(AiTaskState.RUNNING, runningDeadline)) {
            transactions.executeWithoutResult(status -> tasks.lock(running.id())
                    .filter(t -> t.state() == AiTaskState.RUNNING && t.dispatchedAt() != null
                            && t.dispatchedAt().isBefore(runningDeadline))
                    .ifPresent(t -> unknown(t, "Nhà cung cấp AI không trả lời kịp")));
            quietDeliver(running.id());
        }
        for (AiTask finished : tasks.listUnhandled(now.minus(Duration.ofMinutes(1)))) {
            quietDeliver(finished.id());
        }
    }

    /** Handler lỗi thì thử lại ở lượt quét sau, không chặn các task khác. */
    private void quietDeliver(UUID taskId) {
        try {
            delivery.deliver(taskId);
        } catch (RuntimeException ignored) {
            // Thử lại ở lượt quét sau.
        }
    }

    // ---- tiện ích ---------------------------------------------------------------------------------------------

    private void save(AiTask task) {
        if (!tasks.update(task)) {
            throw new IllegalStateException("Task " + task.id() + " vừa bị thay đổi");
        }
    }

    private static Source source(AiTask task) {
        return task.source() == AiSource.SHARED ? Source.SHARED : Source.PERSONAL;
    }

    private JsonNode readPrompt(String payload) {
        try {
            return json.readTree(payload == null ? "{}" : payload);
        } catch (JsonProcessingException e) {
            return json.createObjectNode();
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() ? value.asText() : null;
    }

    private record Dispatch(AiTask task, AiConnection connection, Request request) {
    }

    private record Outcome(Response response, ProviderException error) {
    }
}
