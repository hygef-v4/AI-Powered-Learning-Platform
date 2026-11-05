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

    public AdminDashboardController(AdminDashboardService adminDashboardService, AdminDashboardActivityService adminDashboardActivityService) {
        this.adminDashboardService = adminDashboardService;
        this.adminDashboardActivityService = adminDashboardActivityService;
    }

    @GetMapping
    public ResponseEntity<AdminDashboardView> loadDashboard(@AuthenticationPrincipal Actor actor,
            @RequestParam MultiValueMap<String, String> query) {
        adminDashboardService.requireAdmin(actor);
        validateKeys(query, Set.of());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(adminDashboardService.loadDashboard(actor));
    }

    @GetMapping("/activity")
    public ResponseEntity<ActivityView> loadActivity(@AuthenticationPrincipal Actor actor,
            @RequestParam MultiValueMap<String, String> query) {
        adminDashboardService.requireAdmin(actor);
        validateKeys(query, Set.of());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(adminDashboardActivityService.loadActivity(actor));
    }

    @GetMapping("/backups")
    public ResponseEntity<BackupHistoryPage> listBackups(@AuthenticationPrincipal Actor actor,
            @RequestParam MultiValueMap<String, String> query) {
        adminDashboardService.requireAdmin(actor);
        validateKeys(query, Set.of("page", "size"));
        int page = readInteger(query, "page", 0);
        int size = readInteger(query, "size", 20);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(adminDashboardService.listBackups(actor, page, size));
    }

    private void validateKeys(MultiValueMap<String, String> query, Set<String> allowed) {
        if (!allowed.containsAll(query.keySet()) || query.values().stream().anyMatch(values -> values.size() != 1)) {
            throw createInvalidQuery();
        }
    }

    private int readInteger(MultiValueMap<String, String> query, String key, int fallback) {
        if (!query.containsKey(key)) return fallback;
        try {
            String value = query.getFirst(key);
            if (value == null || !value.matches("-?[0-9]+")) throw createInvalidQuery();
            return Integer.parseInt(value);
        } catch (NumberFormatException invalid) {
            throw createInvalidQuery();
        }
    }

    private ApiException createInvalidQuery() {
        return ApiException.badRequest("DASHBOARD_PAGE_INVALID", "Trang hoặc tham số không hợp lệ");
    }
}
