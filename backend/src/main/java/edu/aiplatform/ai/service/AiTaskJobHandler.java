package edu.aiplatform.ai.service;

import edu.aiplatform.ai.config.AiProperties;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.service.IProviderGateway.FailureKind;
import edu.aiplatform.ai.service.IProviderGateway.ProviderException;
import edu.aiplatform.ai.service.IProviderGateway.Request;
import edu.aiplatform.ai.service.IProviderGateway.Response;
import edu.aiplatform.ai.service.IProviderGateway.Usage;
import edu.aiplatform.ai.contract.AiImage;
import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.ai.contract.AiTaskState;
import edu.aiplatform.ai.contract.AiTokenEstimate;
import edu.aiplatform.ai.model.AiConnection;
import edu.aiplatform.ai.model.AiConnection.Status;
import edu.aiplatform.ai.repository.AiConnectionRepository;
import edu.aiplatform.ai.model.AiTask;
import edu.aiplatform.ai.repository.AiTaskRepository;
import edu.aiplatform.ai.model.AiUsageRecord.Source;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final Logger log = LoggerFactory.getLogger(AiTaskJobHandler.class);
    static final Duration QUEUE_TIMEOUT = Duration.ofMinutes(30);
    static final Duration STALE_PENDING = Duration.ofMinutes(5);
    static final int MAX_RESULT_CHARS = 1_000_000;
    static final int SHARED_CONCURRENCY = 2;
    static final String MISSING_IMAGES = "Ảnh gửi AI không còn";

    private final AiTaskRepository tasks;
    private final AiConnectionRepository connections;
    private final IProviderGateway gateway;
    private final KeyCipher cipher;
    private final JsonSchemaChecker schemas;
    private final QuotaReservationService quota;
    private final AiUsageService usage;
    private final AiTaskDelivery delivery;
    private final AiProperties properties;
    private final ProviderCatalog catalog;
    private final ObjectMapper json;
    private final TransactionTemplate transactions;
    private final Clock clock;
    /** API chung: tối đa 2 lời gọi đồng thời mỗi provider trong một worker (NFR-U06-03). */
    private final Map<AiProvider, Semaphore> sharedSlots = new EnumMap<>(AiProvider.class);
    private AiImageSourceRegistry imageSources = AiImageSourceRegistry.of(List.of());

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
        this.catalog = new ProviderCatalog(properties);
        this.json = json;
        this.transactions = new TransactionTemplate(transactionManager);
        this.clock = clock;
        for (AiProvider provider : AiProvider.values()) {
            sharedSlots.put(provider, new Semaphore(SHARED_CONCURRENCY));
        }
    }

    @Autowired(required = false)
    void setImageSources(AiImageSourceRegistry imageSources) {
        this.imageSources = imageSources;
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
        return callWindow();
    }

    /** Thời gian tối đa một lần gọi provider có thể còn chạy sau mốc gửi. */
    private Duration callWindow() {
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
            // Khóa lỗi thì finish trả lượt gọi bằng khóa kế tiếp (hệ thống rồi cá nhân; Claude, GPT, Gemini; mô hình tốt
            // trước), gọi tiếp ngay tới khi có kết quả hoặc hết khóa.
            while (dispatch != null) {
                long callStartNanos = System.nanoTime();
                Outcome outcome = callProvider(dispatch);
                // Quá tải hoặc vượt tần suất (429, 503, 529) thường chỉ thoáng qua: đợi rồi gọi lại cùng khóa vài lần
                // trước khi chuyển khóa (lỗi 2026-11-05: Gemini 503 dù kiểm tra khóa đạt).
                for (int retry = 0; retry < transientWaits.length && transientFailure(outcome); retry++) {
                    pause(transientWaits[retry]);
                    outcome = callProvider(dispatch);
                }
                long callDurationMs = Duration.ofNanos(System.nanoTime() - callStartNanos).toMillis();
                Dispatch current = dispatch;
                Outcome result = outcome;
                dispatch = transactions.execute(status -> finish(taskId, result));
                String outcomeKind = result.error() == null ? "READY" : result.error().kind().name() + "/" + result.error().status();
                log.info("AI task provider finished taskId={} requestRef={} taskType={} targetRef={} provider={} outcome={} "
                                + "durationMs={} fallback={}", current.task().id(), current.task().requestRef(),
                        current.task().taskType(), current.task().targetRef(), current.connection().provider(), outcomeKind,
                        callDurationMs, dispatch != null);
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
            // Message đến lại sau mốc gửi: không gọi provider lần hai (BR-U06-22). Lần gọi trước có thể vẫn đang chạy
            // (bản trùng do sweeper hoặc retry): chỉ coi là không rõ khi đã quá hạn gọi, không thì để lần gọi đó xong.
            if (task.dispatchedAt() != null && !task.dispatchedAt().plus(callWindow()).isBefore(now)) {
                return null;
            }
            unknown(task, "Không rõ kết quả của lần gọi trước");
            return null;
        }
        AiConnection connection = task.connectionId() == null ? null
                : connections.findById(task.connectionId()).orElse(null);
        AiTask running = task.moveTo(AiTaskState.RUNNING, now);
        if (connection == null || connection.status() != Status.ACTIVE) {
            // Khóa đã tắt hoặc thu hồi sau khi gửi: dùng khóa kế tiếp nếu còn.
            if (running.reservedTokens() != null) {
                quota.release(running.actorId(), running.quotaDay(), running.reservedTokens());
            }
            Dispatch next = next(running.retryWith(running.source(), running.connectionId(), running.provider(),
                    running.model(), running.maxOutputTokens(), null, null, now));
            if (next == null) {
                save(running.retryWith(running.source(), running.connectionId(), running.provider(), running.model(),
                        running.maxOutputTokens(), null, null, now).moveTo(AiTaskState.FAILED, now)
                        .withError("Kết nối AI đã bị tắt hoặc thu hồi"));
            }
            return next;
        }
        List<AiImage> images = loadImages(readPrompt(task.inputPayload()));
        if (images == null) {
            // Ảnh đã bị dọn (bản nháp bị bỏ hoặc quá hạn): không gọi provider, hoàn chỗ quota (BR-U06-55).
            fail(task, MISSING_IMAGES, false);
            return null;
        }
        save(running);
        return dispatch(running, connection, images);
    }

    private Dispatch dispatch(AiTask task, AiConnection connection, List<AiImage> images) {
        String key = cipher.decryptConnectionKey(connection);
        JsonNode prompt = readPrompt(task.inputPayload());
        return new Dispatch(task, connection, new Request(task.model(), key, text(prompt, "system"), text(prompt, "user"),
                task.outputSchema(), task.maxOutputTokens(), properties.requestTimeout(), images));
    }

    /** Nạp bytes ảnh theo tham chiếu trong prompt; thiếu ảnh nào thì trả {@code null} (BR-U06-55). */
    private List<AiImage> loadImages(JsonNode prompt) {
        List<AiImage> images = new ArrayList<>();
        for (JsonNode ref : prompt.path("images")) {
            Optional<AiImage> image = imageSources.find(ref.path("source").asText(""))
                    .flatMap(source -> source.load(ref.path("ref").asText("")));
            if (image.isEmpty() || image.get().bytes() == null || image.get().bytes().length == 0) {
                return null;
            }
            images.add(new AiImage(ref.path("mediaType").asText(image.get().mediaType()), image.get().bytes()));
        }
        return images;
    }

    /**
     * Khóa kế tiếp sau khóa của task (đã quyết toán quota lần trước): API chung theo thứ tự rồi API cá nhân của người
     * gửi; khóa chung mà hết hạn mức thì bỏ qua. Lưu task với khóa mới; hết khóa thì trả rỗng, task chưa đổi.
     */
    private Dispatch next(AiTask task) {
        List<AiConnection> order = new ArrayList<>();
        if (task.source() == AiSource.SHARED) {
            order.addAll(ConnectionOrder.active(connections.listInstitutional(), catalog));
        }
        order.addAll(ConnectionOrder.active(connections.listPersonal(task.actorId()), catalog));
        int at = -1;
        for (int i = 0; i < order.size(); i++) {
            if (order.get(i).id().equals(task.connectionId())) {
                at = i;
            }
        }
        if (at < 0 && task.source() == AiSource.PERSONAL) {
            // Khóa cá nhân đang dùng đã tắt: thử các khóa cá nhân xếp sau nó theo nhà cung cấp.
            int rank = ConnectionOrder.providerRank(task.provider());
            order.removeIf(c -> ConnectionOrder.providerRank(c.provider()) <= rank);
        }
        JsonNode prompt = readPrompt(task.inputPayload());
        int imageCount = prompt.path("images").size();
        int promptChars = text(prompt, "system").length() + text(prompt, "user").length()
                + imageCount * AiTokenEstimate.IMAGE_PROMPT_CHARS;
        List<AiImage> images = imageCount == 0 ? List.of() : loadImages(prompt);
        if (images == null) {
            return null;
        }
        for (AiConnection candidate : order.subList(at + 1, order.size())) {
            if (candidate.id().equals(task.connectionId())) {
                continue;
            }
            // Prompt có ảnh: bỏ qua model không đọc được ảnh (BR-U06-58).
            if (imageCount > 0 && !catalog.supportsImages(candidate.provider(), candidate.model())) {
                continue;
            }
            int max = Math.min(task.maxOutputTokens(), catalog.maxOutputTokens(candidate.provider()));
            Long reserved = null;
            LocalDate day = null;
            AiSource source = candidate.scope() == AiConnection.Scope.INSTITUTIONAL ? AiSource.SHARED : AiSource.PERSONAL;
            if (source == AiSource.SHARED) {
                try {
                    QuotaReservationService.Reservation reservation = quota.reserve(task.actorId(),
                            QuotaReservationService.estimate(promptChars, max));
                    reserved = reservation.tokens();
                    day = reservation.day();
                } catch (RuntimeException e) {
                    continue;
                }
            }
            AiTask moved = task.retryWith(source, candidate.id(), candidate.provider(), candidate.model(), max, reserved,
                    day, clock.instant());
            save(moved);
            return dispatch(tasks.findById(moved.id()).orElse(moved), candidate, images);
        }
        return null;
    }

    /** Thời gian đợi trước mỗi lần gọi lại khi nhà cung cấp quá tải; test đặt rỗng để không đợi. */
    Duration[] transientWaits = {Duration.ofSeconds(2), Duration.ofSeconds(5)};

    private static boolean transientFailure(Outcome outcome) {
        ProviderException e = outcome.error();
        return e != null && e.kind() == FailureKind.REJECTED
                && (e.status() == 429 || e.status() == 503 || e.status() == 529);
    }

    private static void pause(Duration wait) {
        try {
            Thread.sleep(wait.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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

    private Dispatch finish(UUID taskId, Outcome outcome) {
        AiTask task = tasks.lock(taskId).orElse(null);
        if (task == null || task.state() != AiTaskState.RUNNING) {
            return null;
        }
        if (outcome.error() == null) {
            Response response = outcome.response();
            Optional<String> invalid = response.output().length() > MAX_RESULT_CHARS ? Optional.of("quá dài")
                    : schemas.check(task.outputSchema(), response.output());
            settle(task, response.usage(), invalid.isEmpty() ? "READY" : "INVALID_OUTPUT");
            if (invalid.isPresent()) {
                // Chỉ ghi vị trí lệch schema để chẩn đoán, không ghi nội dung kết quả.
                log.warn("AI task output rejected taskId={} taskType={} reason={}", task.id(), task.taskType(),
                        invalid.get().length() > 300 ? invalid.get().substring(0, 300) : invalid.get());                save(task.moveTo(AiTaskState.FAILED, clock.instant()).withResult(null, response.requestId())
                        .withError("Kết quả AI không đúng định dạng yêu cầu"));
            } else {
                save(task.withResult(response.output(), response.requestId()).moveTo(AiTaskState.READY, clock.instant()));
            }
            return null;
        }
        ProviderException e = outcome.error();
        // Quyết toán lần gọi lỗi như trước, rồi thử khóa kế tiếp; hết khóa mới kết thúc task.
        switch (e.kind()) {
            case REJECTED -> {
                if (task.reservedTokens() != null) {
                    quota.release(task.actorId(), task.quotaDay(), task.reservedTokens());
                }
                usage.recordProvider(task.actorId(), source(task), task.id(), task.provider(), task.model(), task.taskType(),
                        "REJECTED", null, null);
            }
            case CHARGED -> settle(task, e.usage(), "FAILED");
            case UNKNOWN -> {
                if (task.reservedTokens() != null) {
                    quota.chargeReserved(task.actorId(), task.quotaDay(), task.reservedTokens());
                }
                usage.recordProvider(task.actorId(), source(task), task.id(), task.provider(), task.model(), task.taskType(),
                        "UNKNOWN", null, task.reservedTokens());
            }
        }
        AiTask settled = task.retryWith(task.source(), task.connectionId(), task.provider(), task.model(),
                task.maxOutputTokens(), null, null, task.dispatchedAt());
        Dispatch next = next(settled);
        if (next != null) {
            return next;
        }
        AiTaskState end = e.kind() == FailureKind.UNKNOWN ? AiTaskState.UNKNOWN : AiTaskState.FAILED;
        save(settled.moveTo(end, clock.instant()).withError(e.getMessage()));
        return null;
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
        long startNanos = System.nanoTime();
        try {
            delivery.deliver(taskId);
        } catch (RuntimeException e) {
            long durationMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
            String causeClass = e.getCause() != null ? e.getCause().getClass().getSimpleName() : "none";
            log.warn("AI task delivery failed taskId={} durationMs={} errorClass={} causeClass={}",
                    taskId, durationMs, e.getClass().getSimpleName(), causeClass);
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
        long startNanos = System.nanoTime();
        try {
            log.info("AI task scanner recovery started taskId={}", taskId);
            delivery.deliver(taskId);
        } catch (RuntimeException e) {
            long durationMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
            String causeClass = e.getCause() != null ? e.getCause().getClass().getSimpleName() : "none";
            log.warn("AI task scanner recovery failed taskId={} durationMs={} errorClass={} causeClass={}",
                    taskId, durationMs, e.getClass().getSimpleName(), causeClass);
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
