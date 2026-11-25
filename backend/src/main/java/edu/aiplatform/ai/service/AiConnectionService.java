package edu.aiplatform.ai.service;

import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.model.AiConnection;
import edu.aiplatform.ai.model.AiConnection.Scope;
import edu.aiplatform.ai.model.AiConnection.Status;
import edu.aiplatform.ai.repository.AiConnectionRepository;
import edu.aiplatform.ai.model.AiUsageRecord.Source;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kết nối API cá nhân (UC 31) và API chung của trường (UC 14, 15) (F1–F3, F8). Key mã hóa trước khi lưu, chỉ hiện 4
 * ký tự cuối, không ra API, log hay audit (BR-U06-03); thay key hoặc model kèm {@code version} (BR-U06-06).
 */
@Service
public class AiConnectionService {

    static final int MIN_KEY = 8;
    static final int MAX_KEY = 500;

    private final AiConnectionRepository repository;
    private final KeyCipher cipher;
    private final ProviderCatalog catalog;
    private final ConnectionTestService connectionTestService;
    private final AiUsageService aiUsageService;
    private final AiAccess access;
    private final IAudit audit;
    private final Clock clock;

    public AiConnectionService(AiConnectionRepository repository, KeyCipher cipher, ProviderCatalog catalog,
                               ConnectionTestService connectionTestService, AiUsageService aiUsageService, AiAccess access, IAudit audit,
                               Clock clock) {
        this.repository = repository;
        this.cipher = cipher;
        this.catalog = catalog;
        this.connectionTestService = connectionTestService;
        this.aiUsageService = aiUsageService;
        this.access = access;
        this.audit = audit;
        this.clock = clock;
    }

    // ---- API cá nhân ----------------------------------------------------------------------------------------
    // Như khóa của trường (yêu cầu 2026-11-05): nhiều khóa cho một nhà cung cấp, mỗi khóa có công tắc, thay, kiểm tra,
    // thu hồi theo id; chỉ chủ khóa làm được. Khóa chỉ ghi: đã lưu thì không đọc lại được qua API (2026-11-11).

    /** Mọi khóa cá nhân chưa thu hồi của người dùng, theo nhà cung cấp rồi thời điểm thêm. */
    @Transactional(readOnly = true)
    public List<ConnectionView> listPersonal(Actor actor) {
        access.require(actor, Actions.AI_PERSONAL);
        return repository.listPersonal(actor.accountId()).stream().map(c -> view(c.provider(), c)).toList();
    }

    @Transactional
    public ConnectionView addPersonal(Actor actor, AiProvider provider, SaveCommand command) {
        access.require(actor, Actions.AI_PERSONAL);
        return saveConnection(actor, Scope.PERSONAL, actor.accountId(), provider, command, Optional.empty());
    }

    @Transactional
    public ConnectionView replacePersonal(Actor actor, UUID id, SaveCommand command) {
        access.require(actor, Actions.AI_PERSONAL);
        AiConnection connection = loadPersonalConnection(actor, id);
        return saveConnection(actor, Scope.PERSONAL, actor.accountId(), connection.provider(), command, Optional.of(connection));
    }

    /** Công tắc khóa cá nhân: tắt vẫn giữ key, không dùng khi gọi AI. */
    @Transactional
    public ConnectionView setPersonalEnabled(Actor actor, UUID id, boolean enabled, Long version) {
        access.require(actor, Actions.AI_PERSONAL);
        AiConnection connection = loadPersonalConnection(actor, id);
        checkVersion(connection, version);
        AiConnection next = connection.changeStatus(enabled ? Status.ACTIVE : Status.DISABLED, clock.instant());
        store(next);
        auditChange(actor, connection, enabled ? "ENABLE" : "DISABLE");
        return view(connection.provider(), repository.findById(connection.id()).orElse(next));
    }

    @Transactional
    public void revokePersonal(Actor actor, UUID id) {
        access.require(actor, Actions.AI_PERSONAL);
        AiConnection connection = loadPersonalConnection(actor, id);
        store(connection.markRevoked(clock.instant()));
        auditChange(actor, connection, "REVOKE");
    }

    @Transactional
    public ConnectionView testPersonal(Actor actor, UUID id) {
        access.require(actor, Actions.AI_PERSONAL);
        return test(actor, loadPersonalConnection(actor, id), Source.PERSONAL);
    }

    @Transactional
    public TestResult testPersonalDraft(Actor actor, AiProvider provider, UUID id, SaveCommand command) {
        access.require(actor, Actions.AI_PERSONAL);
        return testDraft(actor, Scope.PERSONAL, actor.accountId(), provider,
                id == null ? null : loadPersonalConnection(actor, id), command, Source.PERSONAL);
    }

    private AiConnection loadPersonalConnection(Actor actor, UUID id) {
        return repository.findById(id)
                .filter(c -> c.scope() == Scope.PERSONAL && actor.accountId().equals(c.ownerId())
                        && c.status() != Status.REVOKED)
                .orElseThrow(AiAccess::createNotFoundError);
    }

    // ---- API chung của trường -------------------------------------------------------------------------------

    /** Mọi khóa chung chưa thu hồi, theo provider rồi thời điểm thêm; một provider có thể có nhiều khóa. */
    @Transactional(readOnly = true)
    public List<ConnectionView> listInstitutional(Actor actor) {
        access.require(actor, Actions.AI_ADMIN);
        return repository.listInstitutional().stream().map(c -> view(c.provider(), c)).toList();
    }

    /** Thêm một khóa chung mới cho provider, không thay khóa đang có. */
    @Transactional
    public ConnectionView addInstitutional(Actor actor, AiProvider provider, SaveCommand command) {
        access.require(actor, Actions.AI_ADMIN);
        return saveConnection(actor, Scope.INSTITUTIONAL, null, provider, command, Optional.empty());
    }

    /** Thay khóa hoặc model của một khóa chung, kèm {@code version}. */
    @Transactional
    public ConnectionView replaceInstitutional(Actor actor, UUID id, SaveCommand command) {
        access.require(actor, Actions.AI_ADMIN);
        AiConnection connection = loadInstitutionalConnection(id);
        return saveConnection(actor, Scope.INSTITUTIONAL, null, connection.provider(), command, Optional.of(connection));
    }

    /** Công tắc API chung: tắt vẫn giữ key, không dùng cho yêu cầu mới (BR-U06-05). */
    @Transactional
    public ConnectionView setInstitutionalEnabled(Actor actor, UUID id, boolean enabled, Long version) {
        access.require(actor, Actions.AI_ADMIN);
        AiConnection connection = loadInstitutionalConnection(id);
        checkVersion(connection, version);
        AiConnection next = connection.changeStatus(enabled ? Status.ACTIVE : Status.DISABLED, clock.instant());
        store(next);
        auditChange(actor, connection, enabled ? "ENABLE" : "DISABLE");
        return view(connection.provider(), repository.findById(connection.id()).orElse(next));
    }

    @Transactional
    public void revokeInstitutional(Actor actor, UUID id) {
        access.require(actor, Actions.AI_ADMIN);
        AiConnection connection = loadInstitutionalConnection(id);
        store(connection.markRevoked(clock.instant()));
        auditChange(actor, connection, "REVOKE");
    }

    @Transactional
    public ConnectionView testInstitutional(Actor actor, UUID id) {
        access.require(actor, Actions.AI_ADMIN);
        return test(actor, loadInstitutionalConnection(id), Source.SHARED);
    }

    /**
     * Kiểm tra trong popup trước khi lưu: khóa vừa nhập, hoặc khóa đã lưu của {@code id} khi để trống ô khóa, với mô
     * hình đang chọn. Không lưu kết quả vào khóa; vẫn ghi usage (có thể tính phí) và audit như kiểm tra thường.
     */
    @Transactional
    public TestResult testInstitutionalDraft(Actor actor, AiProvider provider, UUID id, SaveCommand command) {
        access.require(actor, Actions.AI_ADMIN);
        return testDraft(actor, Scope.INSTITUTIONAL, null, provider, id == null ? null : loadInstitutionalConnection(id), command,
                Source.SHARED);
    }

    private TestResult testDraft(Actor actor, Scope scope, UUID ownerId, AiProvider provider, AiConnection saved,
                                 SaveCommand command, Source source) {
        String model = requireAllowedModel(provider, command.model());
        String key = requireValidKey(command.apiKey());
        Instant now = clock.instant();
        AiConnection probe;
        if (key != null) {
            UUID probeId = saved == null ? UUID.randomUUID() : saved.id();
            probe = AiConnection.createConnection(probeId, scope, ownerId, provider, model,
                    cipher.encryptConnectionKey(probeId, scope, ownerId, provider, key), KeyCipher.buildHint(key), now);
        } else if (saved != null) {
            if (saved.provider() != provider) {
                throw ApiException.createBadRequest("AI_KEY_REQUIRED", "Cần nhập khóa API");
            }
            probe = saved.replaceModel(model, now);
        } else {
            throw ApiException.createBadRequest("AI_KEY_REQUIRED", "Cần nhập khóa API");
        }
        ConnectionTestService.Result result = connectionTestService.test(probe);
        recordTestUsage(actor, source, provider, model, result);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("provider", provider.name());
        details.put("scope", scope.name());
        details.put("ok", result.ok());
        details.put("draft", true);
        // Chỉ ghi audit cho khóa AI của hệ thống (yêu cầu 2026-11-25); khóa cá nhân của giảng viên thì không.
        if (scope == Scope.INSTITUTIONAL) {
            audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(), "AI_CONNECTION_TESTED", "AI_CONNECTION",
                    probe.id().toString(), result.ok() ? AuditResult.SUCCESS : AuditResult.FAILURE,
                    result.ok() ? null : result.message(), details, now));
        }
        return new TestResult(result.ok(), result.message());
    }

    private AiConnection loadInstitutionalConnection(UUID id) {
        return repository.findById(id)
                .filter(c -> c.scope() == Scope.INSTITUTIONAL && c.status() != Status.REVOKED)
                .orElseThrow(AiAccess::createNotFoundError);
    }

    // ---- chung ---------------------------------------------------------------------------------------------

    private ConnectionView saveConnection(Actor actor, Scope scope, UUID ownerId, AiProvider provider, SaveCommand command,
                                Optional<AiConnection> existing) {
        String model = requireAllowedModel(provider, command.model());
        String key = requireValidKey(command.apiKey());
        Instant now = clock.instant();
        if (existing.isEmpty()) {
            if (key == null) {
                throw ApiException.createBadRequest("AI_KEY_REQUIRED", "Cần nhập khóa API");
            }
            UUID id = UUID.randomUUID();
            AiConnection created = AiConnection.createConnection(id, scope, ownerId, provider, model,
                    cipher.encryptConnectionKey(id, scope, ownerId, provider, key), KeyCipher.buildHint(key), now);
            repository.insert(created);
            auditChange(actor, created, "ADD");
            return view(provider, created);
        }
        AiConnection connection = existing.get();
        checkVersion(connection, command.version());
        AiConnection next = key == null ? connection.replaceModel(model, now)
                : connection.replaceKey(model, cipher.encryptConnectionKey(connection.id(), connection.scope(),
                        connection.ownerId(), connection.provider(), key), KeyCipher.buildHint(key), now);
        store(next);
        auditChange(actor, next, key == null ? "MODEL" : "REPLACE");
        return view(provider, repository.findById(connection.id()).orElse(next));
    }

    private String requireAllowedModel(AiProvider provider, String raw) {
        String model = raw == null ? null : raw.trim();
        if (!catalog.isAllowed(provider, model)) {
            throw ApiException.createBadRequest("AI_MODEL_NOT_ALLOWED", "Mô hình không nằm trong danh sách cho phép");
        }
        return model;
    }

    /** Khóa đã bỏ khoảng trắng hai đầu; rỗng là {@code null} (giữ khóa cũ). */
    private static String requireValidKey(String raw) {
        String key = raw == null || raw.isBlank() ? null : raw.trim();
        if (key != null && (key.length() < MIN_KEY || key.length() > MAX_KEY || key.chars().anyMatch(Character::isWhitespace))) {
            throw ApiException.createBadRequest("AI_KEY_INVALID", "Khóa API không hợp lệ");
        }
        return key;
    }

    private ConnectionView test(Actor actor, AiConnection connection, Source source) {
        if (connection.status() == Status.REVOKED) {
            throw AiAccess.createNotFoundError();
        }
        ConnectionTestService.Result result = connectionTestService.test(connection);
        Instant now = clock.instant();
        repository.saveTest(connection.id(), result.message(), now);
        recordTestUsage(actor, source, connection.provider(), connection.model(), result);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("provider", connection.provider().name());
        details.put("scope", connection.scope().name());
        details.put("ok", result.ok());
        if (connection.scope() == Scope.INSTITUTIONAL) {
            audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(), "AI_CONNECTION_TESTED", "AI_CONNECTION",
                    connection.id().toString(), result.ok() ? AuditResult.SUCCESS : AuditResult.FAILURE,
                    result.ok() ? null : result.message(), details, now));
        }
        return view(connection.provider(), connection.recordTest(result.message(), now));
    }

    /**
     * Chỉ ghi usage {@code CONNECTION_TEST} khi nhà cung cấp báo token đã dùng; khóa sai hay bị từ chối
     * không tốn token nên không ghi (BR-U06-14). Kết quả kiểm tra vẫn nằm trong audit.
     */
    private void recordTestUsage(Actor actor, Source source, AiProvider provider, String model,
                                 ConnectionTestService.Result result) {
        IProviderGateway.Usage usage = result.usage();
        if (usage == null || (usage.inputTokens() == null && usage.outputTokens() == null)) {
            return;
        }
        aiUsageService.recordProvider(actor.accountId(), source, null, provider, model, "CONNECTION_TEST",
                result.ok() ? "OK" : "FAILED", usage, null);
    }

    /** Chỉ khóa AI của hệ thống vào audit (yêu cầu 2026-11-25). */
    private void auditChange(Actor actor, AiConnection connection, String change) {
        if (connection.scope() != Scope.INSTITUTIONAL) {
            return;
        }
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("change", change);
        details.put("provider", connection.provider().name());
        details.put("scope", connection.scope().name());
        details.put("model", connection.model());
        details.put("keyHint", connection.keyHint());
        audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(), "AI_CONNECTION_CHANGED", "AI_CONNECTION",
                connection.id().toString(), AuditResult.SUCCESS, null, details, clock.instant()));
    }

    private static void checkVersion(AiConnection connection, Long version) {
        if (version == null || connection.version() != version) {
            throw createConflictError();
        }
    }

    private void store(AiConnection connection) {
        if (!repository.updateConnection(connection)) {
            throw createConflictError();
        }
    }

    private static ApiException createConflictError() {
        return ApiException.createConflict("AI_CONNECTION_VERSION_CONFLICT", "Kết nối vừa được thay đổi, hãy tải lại");
    }

    static ConnectionView view(AiProvider provider, AiConnection c) {
        if (c == null || c.status() == Status.REVOKED) {
            return new ConnectionView(provider, false, null, null, null, null, null, 0, null);
        }
        return new ConnectionView(provider, true, c.model(), c.keyHint(), c.status(), c.lastTestAt(), c.lastTestResult(),
                c.version(), c.id());
    }

    /** Kết quả kiểm tra trong popup: {@code message} đã làm sạch, không có khóa. */
    public record TestResult(boolean ok, String message) {
    }

    /** {@code apiKey} rỗng là giữ key cũ, chỉ đổi model. */
    public record SaveCommand(String apiKey, String model, Long version) {

        @Override
        public String toString() {
            return "SaveCommand[model=" + model + ", version=" + version + "]";
        }
    }

    /** Thông tin kết nối trả ra ngoài: chỉ 4 ký tự cuối, không có key hay bản mã (P4). */
    public record ConnectionView(AiProvider provider, boolean configured, String model, String keyHint, Status status,
                                 Instant lastTestAt, String lastTestResult, long version, UUID id) {
    }
}
