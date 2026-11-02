package edu.aiplatform.admindashboard.dto;

import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.backup.contract.BackupState;
import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.subjects.contract.SubjectStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/** HTTP projections owned by U15; no provider IDs, archive references or private records. */
public final class AdminDashboardViews {
    private AdminDashboardViews() { }
    public enum Availability { AVAILABLE, UNAVAILABLE }
    public record Section<T>(Availability availability, T data) {
        public static <T> Section<T> available(T data) { return new Section<>(Availability.AVAILABLE, data); }
        public static <T> Section<T> unavailable() { return new Section<>(Availability.UNAVAILABLE, null); }
    }
    public record AccountCounts(Map<Role, Map<AccountStatus, Long>> byRoleAndStatus, long total, long activeTotal) { }
    public record AdminDashboardView(Instant calculatedAt, Section<AccountCounts> accounts,
            Section<Map<SubjectStatus, Long>> subjects, Section<Map<ClassStatus, Long>> classes,
            Section<Long> activeEnrollments, BackupSection backup) { }
    public record BackupSection(Section<BackupRunItem> latest, Section<BackupSchedule> schedule,
            Section<List<BackupRunItem>> recentRuns, Section<Instant> latestSuccessfulAt,
            Section<BackupFailureSummary> failuresLast7Days) { }
    public record BackupRunItem(LocalDate scheduledFor, BackupState state, Instant snapshotAt, Instant startedAt,
            Instant completedAt, Long byteSize, String safeError, Instant purgedAt) { }
    public record BackupSchedule(boolean enabled, LocalTime dailyAt, String timeZone, Instant nextPlannedAt) { }
    public record BackupFailureSummary(long count, String latestSafeError) { }
    public record BackupHistoryPage(List<BackupRunItem> items, int page, int size, long totalElements, int totalPages) { }
    /** Hoạt động trên Tổng quan: tài khoản hoạt động tăng/giảm hôm nay, bài giao 7 ngày, sự việc đáng ngờ 30 ngày. */
    public record ActivityView(Instant calculatedAt, Section<Long> activeAccountChangeToday,
            Section<List<DayAssignments>> assignmentsByDay, Section<List<SecurityEventItem>> securityEvents) { }
    public record DayAssignments(LocalDate day, long individual, long group, long practice) { }
    public record SecurityEventItem(Instant occurredAt, String action, String result, String account, String reason) { }
}
