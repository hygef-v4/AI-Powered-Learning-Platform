package edu.aiplatform.admindashboard.web;

import edu.aiplatform.admindashboard.application.AdminDashboardActivityService;
import edu.aiplatform.admindashboard.application.AdminDashboardService;
import edu.aiplatform.admindashboard.application.AdminDashboardViews.ActivityView;
import edu.aiplatform.admindashboard.application.AdminDashboardViews.AdminDashboardView;
import edu.aiplatform.admindashboard.application.AdminDashboardViews.BackupHistoryPage;
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
    private final AdminDashboardService service;
    private final AdminDashboardActivityService activity;

    public AdminDashboardController(AdminDashboardService service, AdminDashboardActivityService activity) {
        this.service = service;
        this.activity = activity;
    }

    @GetMapping
    public ResponseEntity<AdminDashboardView> dashboard(@AuthenticationPrincipal Actor actor,
            @RequestParam MultiValueMap<String, String> query) {
        service.requireAdmin(actor);
        validateKeys(query, Set.of());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.dashboard(actor));
    }

    @GetMapping("/activity")
    public ResponseEntity<ActivityView> activity(@AuthenticationPrincipal Actor actor,
            @RequestParam MultiValueMap<String, String> query) {
        service.requireAdmin(actor);
        validateKeys(query, Set.of());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(activity.activity(actor));
    }

    @GetMapping("/backups")
    public ResponseEntity<BackupHistoryPage> backups(@AuthenticationPrincipal Actor actor,
            @RequestParam MultiValueMap<String, String> query) {
        service.requireAdmin(actor);
        validateKeys(query, Set.of("page", "size"));
        int page = integer(query, "page", 0);
        int size = integer(query, "size", 20);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.backups(actor, page, size));
    }

    private void validateKeys(MultiValueMap<String, String> query, Set<String> allowed) {
        if (!allowed.containsAll(query.keySet()) || query.values().stream().anyMatch(values -> values.size() != 1)) {
            throw invalidQuery();
        }
    }

    private int integer(MultiValueMap<String, String> query, String key, int fallback) {
        if (!query.containsKey(key)) return fallback;
        try {
            String value = query.getFirst(key);
            if (value == null || !value.matches("-?[0-9]+")) throw invalidQuery();
            return Integer.parseInt(value);
        } catch (NumberFormatException invalid) {
            throw invalidQuery();
        }
    }

    private ApiException invalidQuery() {
        return ApiException.badRequest("DASHBOARD_PAGE_INVALID", "Trang hoặc tham số không hợp lệ");
    }
}
