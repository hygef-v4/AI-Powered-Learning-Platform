package edu.aiplatform.admindashboard.application;

import static edu.aiplatform.admindashboard.application.AdminDashboardViews.*;

import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.admindashboard.infrastructure.DashboardSourceReader;
import edu.aiplatform.admindashboard.infrastructure.DashboardSourceReader.PendingSource;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.backup.contract.BackupDashboardRunView;
import edu.aiplatform.backup.contract.IBackupDashboardStatus;
import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.settings.contract.ISettings;
import edu.aiplatform.settings.contract.SettingKey;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.ISubjectQuery;
import edu.aiplatform.subjects.contract.SubjectStatus;
import edu.aiplatform.academics.contract.ClassStatus;
import java.time.Clock;
import java.time.Duration;
import edu.aiplatform.backup.contract.BackupDashboardSummary;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AdminDashboardService {
    private static final ZoneId BACKUP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final SettingKey<Boolean> BACKUP_ENABLED = new SettingKey<>("backup.enabled", Boolean.class);
    private static final SettingKey<LocalTime> BACKUP_DAILY_AT = new SettingKey<>("backup.dailyAt", LocalTime.class);
    private final IAccountLookup accounts;
    private final ISubjectQuery subjects;
    private final IClassAccess classes;
    private final IBackupDashboardStatus backups;
    private final ISettings settings;
    private final IAuthorization authorization;
    private final IAudit audit;
    private final DashboardSourceReader reader;
    private final Clock clock;

    public AdminDashboardService(IAccountLookup accounts, ISubjectQuery subjects, IClassAccess classes,
            IBackupDashboardStatus backups, ISettings settings, IAuthorization authorization, IAudit audit,
            DashboardSourceReader reader, Clock clock) {
        this.accounts = accounts;
        this.subjects = subjects;
        this.classes = classes;
        this.backups = backups;
        this.settings = settings;
        this.authorization = authorization;
        this.audit = audit;
        this.reader = reader;
        this.clock = clock;
    }

    public AdminDashboardView dashboard(Actor actor) {
        requireAdmin(actor);
        Instant calculatedAt = clock.instant();
        var accountSource = reader.submit(accounts::countByRoleAndStatus);
        var subjectSource = reader.submit(subjects::countSubjectsByStatus);
        var classSource = reader.submit(classes::countClassesByStatus);
        var enrollmentSource = reader.submit(classes::countActiveEnrollments);
        var latestSource = reader.submit(backups::latestRun);
        var recentSource = reader.submit(() -> backups.history(PageRequest.of(0, 3)));
        var scheduleSource = reader.submit(() -> schedule(calculatedAt));
        Instant fromInclusive = calculatedAt.minus(Duration.ofDays(7));
        var summarySource = reader.submit(() -> backups.summarize(fromInclusive, calculatedAt));

        var accountCounts = section(accountSource, this::accountCounts);
        var subjectCounts = section(subjectSource, counts -> completeCounts(SubjectStatus.class, counts));
        var classCounts = section(classSource, counts -> completeCounts(ClassStatus.class, counts));
        var enrollmentCount = section(enrollmentSource, this::nonNegative);
        var latest = section(latestSource, run -> run.map(this::backupItem).orElse(null));
        var recent = section(recentSource, page -> page.getContent().stream().limit(3).map(this::backupItem).toList());
        var summary = section(summarySource, value -> validSummary(value, fromInclusive, calculatedAt));
        Section<Instant> lastSuccess = summary.availability() == Availability.AVAILABLE
                ? Section.available(summary.data().latestSuccessfulAt()) : Section.unavailable();
        Section<BackupFailureSummary> failures = summary.availability() == Availability.AVAILABLE
                ? Section.available(new BackupFailureSummary(summary.data().failedRuns(), summary.data().latestSafeError()))
                : Section.unavailable();
        var backup = new BackupSection(latest, section(scheduleSource, Function.identity()), recent, lastSuccess, failures);
        return new AdminDashboardView(calculatedAt, accountCounts, subjectCounts, classCounts, enrollmentCount, backup);
    }

    public BackupHistoryPage backups(Actor actor, int page, int size) {
        requireAdmin(actor);
        if (page < 0 || size < 1 || size > 100) {
            throw ApiException.badRequest("DASHBOARD_PAGE_INVALID", "Trang hoặc kích thước trang không hợp lệ");
        }
        Section<BackupHistoryPage> result = section(reader.submit(() -> backups.history(PageRequest.of(page, size))),
                found -> history(found, page, size));
        if (result.availability() == Availability.UNAVAILABLE) {
            throw ApiException.unavailable("DASHBOARD_BACKUP_UNAVAILABLE", "Không đọc được lịch sử backup");
        }
        return result.data();
    }

    private BackupHistoryPage history(Page<BackupDashboardRunView> found, int page, int size) {
        return new BackupHistoryPage(found.getContent().stream().map(this::backupItem).toList(), page, size,
                found.getTotalElements(), found.getTotalPages());
    }

    public void requireAdmin(Actor actor) {
        ApiException failure = null;
        if (actor == null || actor.accountId() == null) {
            failure = new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Cần đăng nhập");
        } else if (actor.role() != Role.ADMIN) {
            failure = ApiException.forbidden("DASHBOARD_FORBIDDEN");
        } else {
            try {
                AuthorizationDecision decision = authorization.authorize(actor, Actions.ADMIN_DASHBOARD_VIEW, ResourceRef.none());
                if (decision == null || (!decision.allowed() && "DEPENDENCY_UNAVAILABLE".equals(decision.reason()))) {
                    failure = authorizationUnavailable();
                } else if (!decision.allowed()) {
                    failure = ApiException.forbidden("DASHBOARD_FORBIDDEN");
                }
            } catch (RuntimeException unavailable) {
                failure = authorizationUnavailable();
            }
        }
        if (failure == null) return;
        try {
            audit.record(new AuditEvent(UUID.randomUUID(), actor == null ? null : actor.accountId(), "ACCESS_DENIED",
                    "ADMIN_DASHBOARD", null, AuditResult.DENIED, failure.code(), Map.of(), clock.instant()));
        } catch (RuntimeException auditUnavailable) {
            throw ApiException.unavailable("DASHBOARD_AUDIT_UNAVAILABLE", "Không ghi được nhật ký truy cập");
        }
        throw failure;
    }

    private ApiException authorizationUnavailable() {
        return ApiException.unavailable("DASHBOARD_AUTHORIZATION_UNAVAILABLE", "Không kiểm tra được quyền truy cập");
    }

    private <T, R> Section<R> section(PendingSource<T> pending, Function<T, R> map) {
        var result = reader.await(pending);
        if (!result.available() || result.value() == null) return Section.unavailable();
        try {
            return Section.available(map.apply(result.value()));
        } catch (RuntimeException invalidData) {
            return Section.unavailable();
        }
    }

    private AccountCounts accountCounts(Map<Role, Map<AccountStatus, Long>> raw) {
        var breakdown = new EnumMap<Role, Map<AccountStatus, Long>>(Role.class);
        raw.forEach((role, statuses) -> {
            Objects.requireNonNull(role);
            completeCounts(AccountStatus.class, statuses);
        });
        long total = 0, active = 0;
        for (Role role : Role.values()) {
            Map<AccountStatus, Long> counts = completeCounts(AccountStatus.class, raw.getOrDefault(role, Map.of()));
            breakdown.put(role, counts);
            for (long count : counts.values()) total = Math.addExact(total, count);
            active = Math.addExact(active, counts.get(AccountStatus.ACTIVE));
        }
        return new AccountCounts(Map.copyOf(breakdown), total, active);
    }

    private <E extends Enum<E>> Map<E, Long> completeCounts(Class<E> type, Map<E, Long> raw) {
        Objects.requireNonNull(raw);
        raw.forEach((status, count) -> { Objects.requireNonNull(status); nonNegative(count); });
        var counts = new EnumMap<E, Long>(type);
        long total = 0;
        for (E status : type.getEnumConstants()) {
            long count = nonNegative(raw.getOrDefault(status, 0L));
            counts.put(status, count);
            total = Math.addExact(total, count);
        }
        return Map.copyOf(counts);
    }

    private long nonNegative(Long count) {
        if (count == null || count < 0) throw new IllegalArgumentException("Invalid count");
        return count;
    }

    private BackupRunItem backupItem(BackupDashboardRunView run) {
        Objects.requireNonNull(run.state());
        Objects.requireNonNull(run.scheduledFor());
        if (run.byteSize() != null) nonNegative(run.byteSize());
        return new BackupRunItem(run.scheduledFor(), run.state(), run.snapshotAt(), run.startedAt(), run.completedAt(),
                run.byteSize(), run.safeError(), run.purgedAt());
    }

    private BackupDashboardSummary validSummary(BackupDashboardSummary value, Instant from, Instant to) {
        if (!from.equals(value.fromInclusive()) || !to.equals(value.toExclusive()) || value.failedRuns() < 0
                || (value.latestSuccessfulAt() != null && !value.latestSuccessfulAt().isBefore(to))
                || (value.failedRuns() == 0 && value.latestSafeError() != null)
                || (value.failedRuns() > 0 && (value.latestSafeError() == null || value.latestSafeError().isBlank()))) {
            throw new IllegalArgumentException("Invalid backup summary");
        }
        return value;
    }

    private BackupSchedule schedule(Instant now) {
        boolean enabled = settings.get(BACKUP_ENABLED).orElse(false);
        LocalTime dailyAt = settings.get(BACKUP_DAILY_AT).orElse(null);
        Instant next = null;
        if (enabled && dailyAt != null) {
            ZonedDateTime local = now.atZone(BACKUP_ZONE);
            ZonedDateTime slot = local.toLocalDate().atTime(dailyAt).atZone(BACKUP_ZONE);
            if (slot.toInstant().isBefore(now)) slot = slot.plusDays(1);
            next = slot.toInstant();
        }
        return new BackupSchedule(enabled, dailyAt, BACKUP_ZONE.getId(), next);
    }
}
