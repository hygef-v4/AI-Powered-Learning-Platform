package edu.aiplatform.audit.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditFilter;
import edu.aiplatform.audit.contract.AuditLogView;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.audit.contract.IAuditQuery;
import edu.aiplatform.audit.contract.IAuditStats;
import edu.aiplatform.audit.domain.AuditLogRepository;
import edu.aiplatform.audit.domain.AuditRow;
import edu.aiplatform.identity.contract.AccountContact;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.subjects.contract.ISubjectQuery;
import edu.aiplatform.subjects.contract.SubjectRef;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cài {@link IAuditQuery} (A2, UC 13): chỉ {@code ADMIN}, kiểm quyền lỗi thì từ chối (BR-U02-07, 50); lọc, mới nhất
 * trước, tối đa 100 dòng mỗi trang (BR-U02-08); email người thực hiện lấy lúc đọc (BR-U02-09); mọi lần tra cứu được
 * audit {@code AUDIT_QUERIED}.
 */
@Service
public class AuditQueryService implements IAuditQuery, IAuditStats {

    /** Ngoài mọi lần bị từ chối: các việc liên quan đăng nhập, khóa, vô hiệu hóa tài khoản, mật khẩu và token MCP. */
    static final List<String> SECURITY_ACTIONS = List.of("LOGIN_FAILED", "LOGIN_BLOCKED_LOCKED", "ACCOUNT_TEMP_LOCKED",
            "AUTH_FAILED", "ACCOUNT_DISABLED", "PASSWORD_RESET", "MCP_TOKEN_REVEALED");

    public static final String READ_ACTION = "AUDIT_READ";
    public static final String OBJECT_TYPE = "AUDIT_LOG";
    public static final int MAX_PAGE_SIZE = 100;
    static final int MAX_ACTION = 100;
    static final int MAX_OBJECT_TYPE = 50;
    static final int MAX_OBJECT_ID = 200;
    static final int MAX_ACTOR_QUERY = 254;
    /** Số tài khoản tối đa khớp phần email đã gõ; U01 trả tối đa 20. */
    static final int ACTOR_MATCH_LIMIT = 20;
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {
    };

    private final AuditLogRepository repository;
    private final IAuthorization authorization;
    private final IAccountLookup accounts;
    private final IAudit audit;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final ObjectProvider<ISubjectQuery> subjects;
    private final ObjectProvider<IClassAccess> classes;

    public AuditQueryService(AuditLogRepository repository, IAuthorization authorization, IAccountLookup accounts,
                             IAudit audit, ObjectMapper objectMapper, Clock clock) {
        this(repository, authorization, accounts, audit, objectMapper, clock, null, null);
    }

    /** Môn và lớp chỉ dùng để hiện tên đối tượng; thiếu bean hay lỗi thì dòng vẫn hiện, chỉ không có tên. */
    @Autowired
    public AuditQueryService(AuditLogRepository repository, IAuthorization authorization, IAccountLookup accounts,
                             IAudit audit, ObjectMapper objectMapper, Clock clock,
                             ObjectProvider<ISubjectQuery> subjects, ObjectProvider<IClassAccess> classes) {
        this.subjects = subjects;
        this.classes = classes;
        this.repository = repository;
        this.authorization = authorization;
        this.accounts = accounts;
        this.audit = audit;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Page<AuditLogView> query(Actor actor, AuditFilter filter, Pageable page, boolean recordView) {
        requireAdmin(actor);
        AuditFilter normalized = withActorMatches(normalize(filter));
        validatePage(page);
        long total = repository.count(normalized);
        List<AuditRow> rows = total == 0 ? List.of()
                : repository.find(normalized, page.getOffset(), page.getPageSize());
        Map<UUID, String> emails = emailsOf(rows);
        Map<String, Optional<String>> labels = new HashMap<>();
        List<AuditLogView> items = rows.stream()
                .map(row -> new AuditLogView(toEvent(row), row.actorId() == null ? null : emails.get(row.actorId()),
                        labels.computeIfAbsent(row.objectType() + ":" + row.objectId(), key -> objectLabel(row))
                                .orElse(null)))
                .toList();
        if (recordView) {
            audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(), "AUDIT_QUERIED", OBJECT_TYPE, null,
                    AuditResult.SUCCESS, null, describe(normalized, page), clock.instant()));
        }
        return new PageImpl<>(items, page, total);
    }

    @Override
    @Transactional(readOnly = true)
    public long countSucceeded(String action, Instant from, Instant to, String detailKey, String detailValue) {
        return repository.countSucceeded(action, from, to, detailKey, detailValue);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogView> recentSecurityEvents(Instant from, int limit) {
        List<AuditRow> rows = repository.findSecurity(from, SECURITY_ACTIONS, Math.max(1, Math.min(limit, MAX_PAGE_SIZE)));
        Map<UUID, String> emails = emailsOf(rows);
        Map<String, Optional<String>> labels = new HashMap<>();
        return rows.stream()
                .map(row -> new AuditLogView(toEvent(row), row.actorId() == null ? null : emails.get(row.actorId()),
                        labels.computeIfAbsent(row.objectType() + ":" + row.objectId(), key -> objectLabel(row))
                                .orElse(null)))
                .toList();
    }

    /** Fail closed: không có actor, không phải Admin, U01 từ chối hoặc lỗi đều bị từ chối và được audit. */
    private void requireAdmin(Actor actor) {
        boolean allowed;
        try {
            allowed = actor != null && actor.role() == Role.ADMIN
                    && authorization.authorize(actor, READ_ACTION, ResourceRef.none()).allowed();
        } catch (RuntimeException e) {
            allowed = false;
        }
        if (!allowed) {
            audit.record(new AuditEvent(UUID.randomUUID(), actor == null ? null : actor.accountId(), "ACCESS_DENIED",
                    OBJECT_TYPE, null, AuditResult.DENIED, READ_ACTION, Map.of(), clock.instant()));
            throw ApiException.forbidden("AUDIT_FORBIDDEN");
        }
    }

    /** Chuỗi rỗng coi như không lọc; quá độ dài cột hoặc {@code from} sau {@code to} là bộ lọc sai. */
    static AuditFilter normalize(AuditFilter filter) {
        if (filter == null) {
            return new AuditFilter(null, null, null, null, null, null, null);
        }
        AuditFilter result = new AuditFilter(filter.actorId(), text(filter.action(), MAX_ACTION, "action"),
                text(filter.objectType(), MAX_OBJECT_TYPE, "objectType"),
                text(filter.objectId(), MAX_OBJECT_ID, "objectId"), filter.result(), filter.from(), filter.to(),
                text(filter.actor(), MAX_ACTOR_QUERY, "actor"), filter.actorIds());
        if (result.from() != null && result.to() != null && result.from().isAfter(result.to())) {
            throw invalid("Thời điểm bắt đầu phải trước thời điểm kết thúc");
        }
        return result;
    }

    /** Có phần email hoặc tên người thực hiện mà chưa có actorId: tra các tài khoản khớp qua U01 (không phân biệt hoa thường). */
    private AuditFilter withActorMatches(AuditFilter filter) {
        if (filter.actor() == null || filter.actorId() != null) {
            return filter;
        }
        List<UUID> ids = accounts.findAccounts(filter.actor(), EnumSet.allOf(Role.class), ACTOR_MATCH_LIMIT).stream()
                .map(AccountContact::accountId).toList();
        return new AuditFilter(null, filter.action(), filter.objectType(), filter.objectId(), filter.result(),
                filter.from(), filter.to(), filter.actor(), ids);
    }

    private static String text(String value, int max, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw invalid("Giá trị lọc " + field + " dài quá " + max + " ký tự");
        }
        return trimmed;
    }

    private static void validatePage(Pageable page) {
        if (page == null || page.isUnpaged() || page.getPageSize() < 1 || page.getPageSize() > MAX_PAGE_SIZE) {
            throw invalid("Mỗi trang từ 1 đến " + MAX_PAGE_SIZE + " bản ghi");
        }
    }

    private static ApiException invalid(String message) {
        return ApiException.badRequest("AUDIT_FILTER_INVALID", message);
    }

    /** Mỗi người thực hiện tra một lần; U01 lỗi hoặc không có thì để trống, không làm hỏng trang. */
    private Map<UUID, String> emailsOf(List<AuditRow> rows) {
        Map<UUID, String> emails = new HashMap<>();
        rows.stream().map(AuditRow::actorId).filter(Objects::nonNull).distinct().forEach(id -> {
            try {
                accounts.getContact(id).map(AccountContact::email).ifPresent(email -> emails.put(id, email));
            } catch (RuntimeException e) {
                // Bỏ qua: dòng audit vẫn hiện, chỉ thiếu email.
            }
        });
        return emails;
    }

    /** Tên dễ đọc của đối tượng: email tài khoản, "mã · tên" của môn hoặc lớp; loại khác hay lỗi thì rỗng. */
    private Optional<String> objectLabel(AuditRow row) {
        if (row.objectType() == null || row.objectId() == null) {
            return Optional.empty();
        }
        UUID id;
        try {
            id = UUID.fromString(row.objectId());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        try {
            return switch (row.objectType()) {
                case "ACCOUNT" -> accounts.getContact(id).map(AccountContact::email);
                case "SUBJECT" -> subjects == null ? Optional.empty()
                        : Optional.ofNullable(subjects.getIfAvailable()).flatMap(q -> q.getSubject(id))
                                .map(AuditQueryService::subjectLabel);
                case "CLASS" -> classes == null ? Optional.empty()
                        : Optional.ofNullable(classes.getIfAvailable()).flatMap(q -> q.getClassRef(id))
                                .map(AuditQueryService::classLabel);
                default -> Optional.empty();
            };
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    static String subjectLabel(SubjectRef subject) {
        return join(subject.code(), subject.name());
    }

    static String classLabel(ClassRef ref) {
        return join(ref.code(), ref.name());
    }

    private static String join(String code, String name) {
        if (code == null || code.isBlank()) {
            return name;
        }
        return name == null || name.isBlank() || name.equals(code) ? code : code + " · " + name;
    }

    private AuditEvent toEvent(AuditRow row) {
        return new AuditEvent(row.id(), row.actorId(), row.action(), row.objectType(), row.objectId(),
                AuditResult.valueOf(row.result()), row.reason(), parse(row.detailsJson()), row.occurredAt());
    }

    private Map<String, Object> parse(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP);
        } catch (Exception e) {
            return Map.of();
        }
    }

    /** Bộ lọc đã dùng, ghi vào {@code AUDIT_QUERIED}; chỉ id, không có email. */
    private static Map<String, Object> describe(AuditFilter filter, Pageable page) {
        Map<String, Object> details = new LinkedHashMap<>();
        if (filter.actorId() != null) {
            details.put("actorId", filter.actorId().toString());
        }
        putIfPresent(details, "action", filter.action());
        putIfPresent(details, "objectType", filter.objectType());
        putIfPresent(details, "objectId", filter.objectId());
        putIfPresent(details, "result", filter.result() == null ? null : filter.result().name());
        putIfPresent(details, "from", filter.from() == null ? null : filter.from().toString());
        putIfPresent(details, "to", filter.to() == null ? null : filter.to().toString());
        details.put("page", page.getPageNumber());
        details.put("size", page.getPageSize());
        return details;
    }

    private static void putIfPresent(Map<String, Object> details, String key, String value) {
        if (value != null) {
            details.put(key, value);
        }
    }
}
