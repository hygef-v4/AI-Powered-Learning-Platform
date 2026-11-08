package edu.aiplatform.admindashboard.service;


import static edu.aiplatform.admindashboard.dto.AdminDashboardViews.*;

import edu.aiplatform.admindashboard.integration.DashboardSourceReader;
import edu.aiplatform.assignments.contract.AssignmentDayCount;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.audit.contract.AuditLogView;
import edu.aiplatform.audit.contract.IAuditStats;
import edu.aiplatform.identity.contract.Actor;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Service;

/**
 * Phần hoạt động của Tổng quan (U15), tách khỏi số liệu đếm để màn tự tải lại định kỳ: số tài khoản hoạt động tăng hay
 * giảm trong hôm nay, số bài được giao 7 ngày gần nhất theo loại, các sự việc đáng ngờ 30 ngày gần nhất. Ngày tính theo
 * giờ Việt Nam. Nguồn nào lỗi hay quá hạn thì phần đó không khả dụng, các phần khác vẫn trả.
 */
@Service
public class AdminDashboardActivityService {
    static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    static final int ASSIGNMENT_DAYS = 7;
    static final int SECURITY_DAYS = 30;
    static final int SECURITY_LIMIT = 50;

    private final AdminDashboardService adminDashboardService;
    private final IAuditStats audit;
    private final IAssignmentQuery assignmentQuery;
    private final DashboardSourceReader dashboardSourceReader;
    private final Clock clock;

    public AdminDashboardActivityService(AdminDashboardService adminDashboardService,
            IAuditStats audit, IAssignmentQuery assignmentQuery,
            DashboardSourceReader dashboardSourceReader, Clock clock) {
        this.adminDashboardService = adminDashboardService;
        this.audit = audit;
        this.assignmentQuery = assignmentQuery;
        this.dashboardSourceReader = dashboardSourceReader;
        this.clock = clock;
    }

    public ActivityView loadActivity(Actor actor) {
        adminDashboardService.requireAdmin(actor);
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, ZONE);
        Instant todayStart = today.atStartOfDay(ZONE).toInstant();
        LocalDate firstDay = today.minusDays(ASSIGNMENT_DAYS - 1L);
        Instant assignmentsFrom = firstDay.atStartOfDay(ZONE).toInstant();
        Instant securityFrom = today.minusDays(SECURITY_DAYS - 1L).atStartOfDay(ZONE).toInstant();

        var changeSource = dashboardSourceReader
                .submitSource(() -> loadActiveAccountChange(todayStart, now));
        var assignmentSource = dashboardSourceReader
                .submitSource(() -> assignmentQuery.countGivenByDay(assignmentsFrom, now, ZONE));
        var securitySource = dashboardSourceReader
                .submitSource(() -> audit.recentSecurityEvents(securityFrom, SECURITY_LIMIT));

        return new ActivityView(now,
                dashboardSourceReader.readSection(changeSource, Function.identity()),
                dashboardSourceReader.readSection(assignmentSource,
                        counts -> buildAssignmentDays(firstDay, counts)),
                dashboardSourceReader.readSection(securitySource,
                        events -> events.stream().map(this::mapSecurityItem).toList()));
    }

    /** Kích hoạt và mở lại về Hoạt động trừ vô hiệu hóa tài khoản đang hoạt động, từ đầu ngày tới giờ. */
    private Long loadActiveAccountChange(Instant from, Instant to) {
        long gained = audit.countSucceeded("ACCOUNT_ACTIVATED", from, to, null, null)
                + audit.countSucceeded("ACCOUNT_ENABLED", from, to, "status", "ACTIVE");
        long lost = audit.countSucceeded("ACCOUNT_DISABLED", from, to, "before", "ACTIVE");
        return gained - lost;
    }

    /** Đủ 7 ngày liền nhau (ngày không có bài là 0), mỗi ngày một dòng ba loại. */
    private List<DayAssignments> buildAssignmentDays(LocalDate firstDay,
            List<AssignmentDayCount> counts) {
        Map<LocalDate, long[]> byDay = new LinkedHashMap<>();
        for (int i = 0; i < ASSIGNMENT_DAYS; i++) {
            byDay.put(firstDay.plusDays(i), new long[3]);
        }
        for (AssignmentDayCount count : counts) {
            long[] slot = byDay.get(count.day());
            if (slot == null || count.count() < 0)
                continue;
            switch (count.kind()) {
            case "INDIVIDUAL" -> slot[0] += count.count();
            case "GROUP" -> slot[1] += count.count();
            case "PRACTICE" -> slot[2] += count.count();
            default -> {
            }
            }
        }
        List<DayAssignments> result = new ArrayList<>();
        byDay.forEach(
                (day, slot) -> result.add(new DayAssignments(day, slot[0], slot[1], slot[2])));
        return result;
    }

    /** Tài khoản là người thực hiện, không có thì là tài khoản bị tác động; không rõ ai (sai email) thì để trống. */
    private SecurityEventItem mapSecurityItem(AuditLogView view) {
        var event = view.event();
        String account = view.actorEmail() != null ? view.actorEmail()
                : "ACCOUNT".equals(event.objectType()) ? view.objectLabel() : null;
        return new SecurityEventItem(event.occurredAt(), event.action(), event.result().name(),
                account, event.reason());
    }

}
