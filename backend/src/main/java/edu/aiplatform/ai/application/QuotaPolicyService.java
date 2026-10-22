package edu.aiplatform.ai.application;

import edu.aiplatform.ai.domain.AiUsageRepository;
import edu.aiplatform.ai.domain.AiUsageRepository.SharedRow;
import edu.aiplatform.ai.domain.QuotaPolicy;
import edu.aiplatform.ai.domain.QuotaPolicy.TeacherQuota;
import edu.aiplatform.ai.domain.QuotaPolicy.Window;
import edu.aiplatform.ai.domain.QuotaRepository;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.AccountContact;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hạn mức API chung (F9) và tổng hợp usage API chung cho Admin (F10, BR-U06-41). Mọi lần lưu được audit
 * {@code QUOTA_CHANGED} kèm trước, sau (BR-U06-52).
 */
@Service
public class QuotaPolicyService {

    static final Set<Role> TEACHING = EnumSet.of(Role.TEACHER, Role.SUBJECT_MANAGER);
    static final long MAX_TOKENS = 1_000_000_000L;
    static final int MAX_QUERY = 100;

    private final QuotaRepository quota;
    private final QuotaReservationService reservations;
    private final AiUsageRepository usage;
    private final IAccountLookup accounts;
    private final AiAccess access;
    private final IAudit audit;
    private final Clock clock;

    public QuotaPolicyService(QuotaRepository quota, QuotaReservationService reservations, AiUsageRepository usage,
                              IAccountLookup accounts, AiAccess access, IAudit audit, Clock clock) {
        this.quota = quota;
        this.reservations = reservations;
        this.usage = usage;
        this.accounts = accounts;
        this.access = access;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public QuotaPolicy policy(Actor actor) {
        access.require(actor, Actions.AI_ADMIN);
        return quota.policy();
    }

    @Transactional
    public QuotaPolicy savePolicy(Actor actor, Long defaultDay, Long globalDay, Long version) {
        access.require(actor, Actions.AI_ADMIN);
        if (defaultDay == null || globalDay == null || defaultDay < 0 || globalDay < 0 || defaultDay > MAX_TOKENS
                || globalDay > MAX_TOKENS) {
            throw ApiException.badRequest("AI_QUOTA_INVALID", "Hạn mức phải là số token từ 0 trở lên");
        }
        QuotaPolicy before = quota.policy();
        if (!quota.savePolicy(defaultDay, globalDay, version == null ? -1 : version, actor.accountId(), clock.instant())) {
            throw ApiException.conflict("AI_QUOTA_VERSION_CONFLICT", "Hạn mức vừa được thay đổi, hãy tải lại");
        }
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("defaultBefore", before.defaultDayTokens());
        details.put("globalBefore", before.globalDayTokens());
        details.put("defaultAfter", defaultDay);
        details.put("globalAfter", globalDay);
        record(actor, "QUOTA_POLICY", details);
        return quota.policy();
    }

    @Transactional(readOnly = true)
    public List<TeacherQuotaView> teacherQuotas(Actor actor) {
        access.require(actor, Actions.AI_ADMIN);
        return quota.listTeacherQuotas().stream().map(q -> {
            Optional<AccountContact> contact = contact(q.accountId());
            return new TeacherQuotaView(q.accountId(), contact.map(AccountContact::displayName).orElse(null),
                    contact.map(AccountContact::email).orElse(null), q.dayTokens(), q.updatedAt());
        }).toList();
    }

    /** Mức riêng thay mức chung; 0 là chặn; chỉ đặt cho Teacher, Subject Manager. */
    @Transactional
    public void setTeacherQuota(Actor actor, UUID accountId, Long dayTokens) {
        access.require(actor, Actions.AI_ADMIN);
        if (dayTokens == null || dayTokens < 0 || dayTokens > MAX_TOKENS) {
            throw ApiException.badRequest("AI_QUOTA_INVALID", "Hạn mức phải là số token từ 0 trở lên");
        }
        AccountContact contact = contact(accountId).filter(c -> TEACHING.contains(c.role()))
                .orElseThrow(() -> ApiException.badRequest("AI_QUOTA_NOT_TEACHER", "Chỉ đặt mức riêng cho giảng viên"));
        Long before = quota.teacherQuota(accountId).map(TeacherQuota::dayTokens).orElse(null);
        quota.upsertTeacherQuota(contact.accountId(), dayTokens, actor.accountId(), clock.instant());
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("accountId", accountId.toString());
        details.put("before", before);
        details.put("after", dayTokens);
        record(actor, "TEACHER_QUOTA", details);
    }

    @Transactional
    public void deleteTeacherQuota(Actor actor, UUID accountId) {
        access.require(actor, Actions.AI_ADMIN);
        Long before = quota.teacherQuota(accountId).map(TeacherQuota::dayTokens).orElseThrow(AiAccess::notFound);
        quota.deleteTeacherQuota(accountId);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("accountId", accountId.toString());
        details.put("before", before);
        details.put("after", null);
        record(actor, "TEACHER_QUOTA", details);
    }

    @Transactional(readOnly = true)
    public List<AccountContact> teacherCandidates(Actor actor, String query) {
        access.require(actor, Actions.AI_ADMIN);
        String q = query == null ? "" : query.trim();
        if (q.length() > MAX_QUERY) {
            throw ApiException.badRequest("AI_QUERY_TOO_LONG", "Từ khóa tìm tối đa 100 ký tự");
        }
        return accounts.findAccounts(q, TEACHING, 20);
    }

    /** Tổng hợp API chung (BR-U06-41): hôm nay trên trần, tổng kỳ, số giảng viên đã dùng, theo ngày, người, model. */
    @Transactional(readOnly = true)
    public UsageSummary summary(Actor actor, int days) {
        access.require(actor, Actions.AI_ADMIN);
        int span = days == 30 ? 30 : 7;
        LocalDate today = reservations.today();
        Instant from = today.minusDays(span - 1L).atStartOfDay(QuotaReservationService.VN).toInstant();
        Instant to = today.plusDays(1).atStartOfDay(QuotaReservationService.VN).toInstant();
        List<SharedRow> rows = usage.sharedSummary(from, to);
        QuotaPolicy policy = quota.policy();
        long todayUsed = quota.findWindow(QuotaRepository.GLOBAL, today).map(Window::consumed).orElse(0L);
        Map<LocalDate, Long> byDay = new TreeMap<>();
        for (int i = span - 1; i >= 0; i--) {
            byDay.put(today.minusDays(i), 0L);
        }
        Map<UUID, Long> byTeacher = new HashMap<>();
        Map<String, long[]> byModel = new LinkedHashMap<>();
        for (SharedRow row : rows) {
            byDay.merge(row.day(), row.tokens(), Long::sum);
            byTeacher.merge(row.accountId(), row.tokens(), Long::sum);
            String key = (row.provider() == null ? "-" : row.provider().name()) + "|" + row.model();
            long[] totals = byModel.computeIfAbsent(key, k -> new long[2]);
            totals[0] += row.requests();
            totals[1] += row.tokens();
        }
        List<TeacherUsage> teachers = new ArrayList<>();
        for (Map.Entry<UUID, Long> entry : byTeacher.entrySet()) {
            Optional<AccountContact> contact = contact(entry.getKey());
            Long limit = reservations.limitOf(entry.getKey());
            long usedToday = quota.findWindow(entry.getKey().toString(), today).map(Window::consumed).orElse(0L);
            teachers.add(new TeacherUsage(entry.getKey(), contact.map(AccountContact::displayName).orElse(null),
                    contact.map(AccountContact::email).orElse(null), entry.getValue(), usedToday, limit));
        }
        teachers.sort(Comparator.comparingLong(TeacherUsage::periodTokens).reversed());
        List<ModelUsage> models = byModel.entrySet().stream().map(e -> {
            String[] parts = e.getKey().split("\\|", 2);
            return new ModelUsage(parts[0], parts[1], e.getValue()[0], e.getValue()[1]);
        }).collect(Collectors.toList());
        return new UsageSummary(span, todayUsed, policy.globalDayTokens(),
                byDay.values().stream().mapToLong(Long::longValue).sum(), teachers.size(),
                byDay.entrySet().stream().map(e -> new DayUsage(e.getKey(), e.getValue())).toList(), teachers, models);
    }

    private Optional<AccountContact> contact(UUID accountId) {
        try {
            return accounts.getContact(accountId);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private void record(Actor actor, String objectId, Map<String, Object> details) {
        audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(), "QUOTA_CHANGED", "AI_QUOTA", objectId,
                AuditResult.SUCCESS, null, details, clock.instant()));
    }

    public record TeacherQuotaView(UUID accountId, String name, String email, long dayTokens, Instant updatedAt) {
    }

    public record UsageSummary(int days, long todayTokens, Long globalDayTokens, long periodTokens, int teachersUsed,
                               List<DayUsage> byDay, List<TeacherUsage> byTeacher, List<ModelUsage> byModel) {
    }

    public record DayUsage(LocalDate day, long tokens) {
    }

    public record TeacherUsage(UUID accountId, String name, String email, long periodTokens, long todayTokens,
                               Long dayLimit) {
    }

    public record ModelUsage(String provider, String model, long requests, long tokens) {
    }
}
