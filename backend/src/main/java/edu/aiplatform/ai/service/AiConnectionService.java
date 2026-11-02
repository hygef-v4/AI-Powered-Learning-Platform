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
import java.util.Arrays;
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
    private final ConnectionTestService tester;
    private final AiUsageService usage;
    private final AiAccess access;
    private final IAudit audit;
    private final Clock clock;

    public AiConnectionService(AiConnectionRepository repository, KeyCipher cipher, ProviderCatalog catalog,
                               ConnectionTestService tester, AiUsageService usage, AiAccess access, IAudit audit,
                               Clock clock) {
        this.repository = repository;
        this.cipher = cipher;
        this.catalog = catalog;
        this.tester = tester;
        this.usage = usage;
        this.access = access;
        this.audit = audit;
        this.clock = clock;
    }

    // ---- API cá nhân ----------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ConnectionView> listPersonal(Actor actor) {
        access.require(actor, Actions.AI_PERSONAL);
        Map<AiProvider, AiConnection> saved = new LinkedHashMap<>();
        repository.listPersonal(actor.accountId()).forEach(c -> saved.put(c.provider(), c));
        return Arrays.stream(AiProvider.values()).map(p -> view(p, saved.get(p))).toList();
    }

    @Transactional
    public ConnectionView savePersonal(Actor actor, AiProvider provider, SaveCommand command) {
        access.require(actor, Actions.AI_PERSONAL);
        return save(actor, Scope.PERSONAL, actor.accountId(), provider, command,
                repository.findPersonal(actor.accountId(), provider));
    }

    @Transactional
    public void revokePersonal(Actor actor, AiProvider provider) {
        access.require(actor, Actions.AI_PERSONAL);
        AiConnection connection = repository.findPersonal(actor.accountId(), provider)
                .orElseThrow(AiAccess::notFound);
        store(connection.revoked(clock.instant()));
        auditChange(actor, connection, "REVOKE");
    }

    @Transactional
    public ConnectionView testPersonal(Actor actor, AiProvider provider) {
        access.require(actor, Actions.AI_PERSONAL);
        AiConnection connection = repository.findPersonal(actor.accountId(), provider).orElseThrow(AiAccess::notFound);
        return test(actor, connection, Source.PERSONAL);
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
        return save(actor, Scope.INSTITUTIONAL, null, provider, command, Optional.empty());
    }

    /** Thay khóa hoặc model của một khóa chung, kèm {@code version}. */
    @Transactional
    public ConnectionView replaceInstitutional(Actor actor, UUID id, SaveCommand command) {
        access.require(actor, Actions.AI_ADMIN);
        AiConnection connection = institutional(id);
        return save(actor, Scope.INSTITUTIONAL, null, connection.provider(), command, Optional.of(connection));
    }

    /** Công tắc API chung: tắt vẫn giữ key, không dùng cho yêu cầu mới (BR-U06-05). */
    @Transactional
    public ConnectionView setInstitutionalEnabled(Actor actor, UUID id, boolean enabled, Long version) {
        access.require(actor, Actions.AI_ADMIN);
        AiConnection connection = institutional(id);
        checkVersion(connection, version);
        AiConnection next = connection.withStatus(enabled ? Status.ACTIVE : Status.DISABLED, clock.instant());
        store(next);
        auditChange(actor, connection, enabled ? "ENABLE" : "DISABLE");
        return view(connection.provider(), repository.findById(connection.id()).orElse(next));
    }

    @Transactional
    public void revokeInstitutional(Actor actor, UUID id) {
        access.require(actor, Actions.AI_ADMIN);
        AiConnection connection = institutional(id);
        store(connection.revoked(clock.instant()));
        auditChange(actor, connection, "REVOKE");
    }

    @Transactional
    public ConnectionView testInstitutional(Actor actor, UUID id) {
        access.require(actor, Actions.AI_ADMIN);
        return test(actor, institutional(id), Source.SHARED);
    }

    /**
     * Kiểm tra trong popup trước khi lưu: khóa vừa nhập, hoặc khóa đã lưu của {@code id} khi để trống ô khóa, với mô
     * hình đang chọn. Không lưu kết quả vào khóa; vẫn ghi usage (có thể tính phí) và audit như kiểm tra thường.
     */
    @Transactional
    public TestResult testInstitutionalDraft(Actor actor, AiProvider provider, UUID id, SaveCommand command) {
        access.require(actor, Actions.AI_ADMIN);
        String model = allowedModel(provider, command.model());
        String key = validKey(command.apiKey());
        Instant now = clock.instant();
        AiConnection probe;
        if (key != null) {
            probe = AiConnection.create(id == null ? UUID.randomUUID() : id, Scope.INSTITUTIONAL, null, provider, model,
                    cipher.encrypt(key), KeyCipher.hint(key), now);
        } else if (id != null) {
            AiConnection saved = institutional(id);
            if (saved.provider() != provider) {
                throw ApiException.badRequest("AI_KEY_REQUIRED", "Cần nhập khóa API");
            }
            probe = saved.withModel(model, now);
        } else {
            throw ApiException.badRequest("AI_KEY_REQUIRED", "Cần nhập khóa API");
        }
        ConnectionTestService.Result result = tester.test(probe);
        usage.recordProvider(actor.accountId(), Source.SHARED, null, provider, model, "CONNECTION_TEST",
                result.ok() ? "OK" : "FAILED", result.usage(), null);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("provider", provider.name());
        details.put("scope", Scope.INSTITUTIONAL.name());
        details.put("ok", result.ok());
        details.put("draft", true);
        audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(), "AI_CONNECTION_TESTED", "AI_CONNECTION",
                probe.id().toString(), result.ok() ? AuditResult.SUCCESS : AuditResult.FAILURE,
                result.ok() ? null : result.message(), details, now));
        return new TestResult(result.ok(), result.message());
    }

    /** Khóa rõ của một khóa chung, cho Admin xem lại hoặc sao chép trong popup Sửa; mỗi lần xem ghi audit. */
    @Transactional
    public String revealInstitutional(Actor actor, UUID id) {
        access.require(actor, Actions.AI_ADMIN);
        AiConnection connection = institutional(id);
        String key = cipher.decrypt(connection.encryptedKey());
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("provider", connection.provider().name());
        details.put("keyHint", connection.keyHint());
        audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(), "AI_CONNECTION_KEY_REVEALED", "AI_CONNECTION",
                connection.id().toString(), AuditResult.SUCCESS, null, details, clock.instant()));
        return key;
    }

    private AiConnection institutional(UUID id) {
        return repository.findById(id)
                .filter(c -> c.scope() == Scope.INSTITUTIONAL && c.status() != Status.REVOKED)
                .orElseThrow(AiAccess::notFound);
    }

    // ---- chung ---------------------------------------------------------------------------------------------

    private ConnectionView save(Actor actor, Scope scope, UUID ownerId, AiProvider provider, SaveCommand command,
                                Optional<AiConnection> existing) {
        String model = allowedModel(provider, command.model());
        String key = validKey(command.apiKey());
        Instant now = clock.instant();
        if (existing.isEmpty()) {
            if (key == null) {
                throw ApiException.badRequest("AI_KEY_REQUIRED", "Cần nhập khóa API");
            }
            AiConnection created = AiConnection.create(UUID.randomUUID(), scope, ownerId, provider, model,
                    cipher.encrypt(key), KeyCipher.hint(key), now);
            repository.insert(created);
            auditChange(actor, created, "ADD");
            return view(provider, created);
        }
        AiConnection connection = existing.get();
        checkVersion(connection, command.version());
        AiConnection next = key == null ? connection.withModel(model, now)
                : connection.withKey(model, cipher.encrypt(key), KeyCipher.hint(key), now);
        store(next);
        auditChange(actor, next, key == null ? "MODEL" : "REPLACE");
        return view(provider, repository.findById(connection.id()).orElse(next));
    }

    private String allowedModel(AiProvider provider, String raw) {
        String model = raw == null ? null : raw.trim();
        if (!catalog.allowed(provider, model)) {
            throw ApiException.badRequest("AI_MODEL_NOT_ALLOWED", "Mô hình không nằm trong danh sách cho phép");
        }
        return model;
    }

    /** Khóa đã bỏ khoảng trắng hai đầu; rỗng là {@code null} (giữ khóa cũ). */
    private static String validKey(String raw) {
        String key = raw == null || raw.isBlank() ? null : raw.trim();
        if (key != null && (key.length() < MIN_KEY || key.length() > MAX_KEY || key.chars().anyMatch(Character::isWhitespace))) {
            throw ApiException.badRequest("AI_KEY_INVALID", "Khóa API không hợp lệ");
        }
        return key;
    }

    private ConnectionView test(Actor actor, AiConnection connection, Source source) {
        if (connection.status() == Status.REVOKED) {
            throw AiAccess.notFound();
        }
        ConnectionTestService.Result result = tester.test(connection);
        Instant now = clock.instant();
        repository.saveTest(connection.id(), result.message(), now);
        usage.recordProvider(actor.accountId(), source, null, connection.provider(), connection.model(), "CONNECTION_TEST",
                result.ok() ? "OK" : "FAILED", result.usage(), null);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("provider", connection.provider().name());
        details.put("scope", connection.scope().name());
        details.put("ok", result.ok());
        audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(), "AI_CONNECTION_TESTED", "AI_CONNECTION",
                connection.id().toString(), result.ok() ? AuditResult.SUCCESS : AuditResult.FAILURE,
                result.ok() ? null : result.message(), details, now));
        return view(connection.provider(), connection.withTest(result.message(), now));
    }

    private void auditChange(Actor actor, AiConnection connection, String change) {
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
            throw conflict();
        }
    }

    private void store(AiConnection connection) {
        if (!repository.update(connection)) {
            throw conflict();
        }
    }

    private static ApiException conflict() {
        return ApiException.conflict("AI_CONNECTION_VERSION_CONFLICT", "Kết nối vừa được thay đổi, hãy tải lại");
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
