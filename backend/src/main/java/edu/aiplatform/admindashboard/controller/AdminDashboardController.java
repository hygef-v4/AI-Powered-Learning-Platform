package edu.aiplatform.admindashboard.controller;

import edu.aiplatform.admindashboard.service.AdminDashboardActivityService;
import edu.aiplatform.admindashboard.service.AdminDashboardService;
import edu.aiplatform.admindashboard.dto.AdminDashboardViews.ActivityView;
import edu.aiplatform.admindashboard.dto.AdminDashboardViews.AdminDashboardView;
import edu.aiplatform.admindashboard.dto.AdminDashboardViews.BackupHistoryPage;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.util.Set;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
public class AdminDashboardController {
    private final AdminDashboardService adminDashboardService;
    private final AdminDashboardActivityService adminDashboardActivityService;

    /** Connects the dashboard and activity services to the admin HTTP endpoints. */
    public AdminDashboardController(AdminDashboardService adminDashboardService,
            AdminDashboardActivityService adminDashboardActivityService) {
        this.adminDashboardService = adminDashboardService;
        this.adminDashboardActivityService = adminDashboardActivityService;
    }

    /** Returns account, subject, class, enrollment, and backup summaries without caching. */
    @GetMapping
    public ResponseEntity<AdminDashboardView> loadDashboard(@AuthenticationPrincipal Actor actor,
            @RequestParam MultiValueMap<String, String> query) {
        adminDashboardService.requireAdmin(actor);
        validateKeys(query, Set.of());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(adminDashboardService.loadDashboard(actor));
    }

    /** Returns activity metrics with separate windows for accounts, assignments, and security. */
    @GetMapping("/activity")
    public ResponseEntity<ActivityView> loadActivity(@AuthenticationPrincipal Actor actor,
            @RequestParam MultiValueMap<String, String> query) {
        adminDashboardService.requireAdmin(actor);
        validateKeys(query, Set.of());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(adminDashboardActivityService.loadActivity(actor));
    }

    /** Returns backup history using the requested page and size. */
    @GetMapping("/backups")
    public ResponseEntity<BackupHistoryPage> listBackups(@AuthenticationPrincipal Actor actor,
            @RequestParam MultiValueMap<String, String> query) {
        adminDashboardService.requireAdmin(actor);
        validateKeys(query, Set.of("page", "size"));
        int page = readInteger(query, "page", 0);
        int size = readInteger(query, "size", 20);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(adminDashboardService.listBackups(actor, page, size));
    }

    /** Rejects unknown or repeated query parameters. */
    private void validateKeys(MultiValueMap<String, String> query, Set<String> allowed) {
        if (!allowed.containsAll(query.keySet())
                || query.values().stream().anyMatch(values -> values.size() != 1)) {
            throw createInvalidQuery();
        }
    }

    /** Parses a single integer query value or uses the default when absent. */
    private int readInteger(MultiValueMap<String, String> query, String key, int fallback) {
        if (!query.containsKey(key))
            return fallback;
        try {
            String value = query.getFirst(key);
            if (value == null || !value.matches("-?[0-9]+"))
                throw createInvalidQuery();
            return Integer.parseInt(value);
        } catch (NumberFormatException invalid) {
            throw createInvalidQuery();
        }
    }

    /** Builds the client error for invalid dashboard query parameters. */
    private ApiException createInvalidQuery() {
        return ApiException.badRequest("DASHBOARD_PAGE_INVALID", "Trang hoặc tham số không hợp lệ");
    }
}
