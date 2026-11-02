package edu.aiplatform.audit.controller;

import edu.aiplatform.audit.contract.AuditFilter;
import edu.aiplatform.audit.contract.AuditLogView;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAuditQuery;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** UC 13 View Audit Log: tra cứu chỉ đọc cho Admin; không có API sửa hay xóa (BR-U02-01). */
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
public class AuditController {

    static final int DEFAULT_PAGE_SIZE = 50;

    private final IAuditQuery auditQuery;

    public AuditController(IAuditQuery auditQuery) {
        this.auditQuery = auditQuery;
    }

    public record AuditLogItem(UUID id, Instant occurredAt, UUID actorId, String actorEmail, String action,
                               String objectType, String objectId, String objectLabel, AuditResult result,
                               String reason, Map<String, Object> details) {

        static AuditLogItem of(AuditLogView view) {
            var e = view.event();
            return new AuditLogItem(e.id(), e.occurredAt(), e.actorId(), view.actorEmail(), e.action(),
                    e.objectType(), e.objectId(), view.objectLabel(), e.result(), e.reason(), e.details());
        }
    }

    public record AuditLogPage(List<AuditLogItem> items, int page, int size, long totalElements, int totalPages) {
    }

    /**
     * {@code actor} dựng phía server từ phiên đăng nhập, không lấy từ request; quyền do {@link IAuditQuery} kiểm.
     * Màn Audit Log gửi {@code recordView=false} khi lọc, xóa lọc hay chuyển trang để chỉ lần mở màn được ghi.
     */
    @GetMapping
    public AuditLogPage list(@AuthenticationPrincipal Actor actor,
                             @RequestParam(required = false) UUID actorId,
                             @RequestParam(name = "actor", required = false) String actorText,
                             @RequestParam(required = false) String action,
                             @RequestParam(required = false) String objectType,
                             @RequestParam(required = false) String objectId,
                             @RequestParam(required = false) AuditResult result,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
                             @RequestParam(defaultValue = "true") boolean recordView) {
        if (page < 0 || size < 1) {
            throw ApiException.badRequest("AUDIT_FILTER_INVALID", "Số trang hoặc kích thước trang không hợp lệ");
        }
        Page<AuditLogView> found = auditQuery.query(actor,
                new AuditFilter(actorId, action, objectType, objectId, result, from, to, actorText, null), PageRequest.of(page, size),
                recordView);
        return new AuditLogPage(found.getContent().stream().map(AuditLogItem::of).toList(), found.getNumber(),
                found.getSize(), found.getTotalElements(), found.getTotalPages());
    }

    /** Tham số sai kiểu (UUID, kết quả, thời điểm, số trang): 400 an toàn, không lộ chi tiết nội bộ (BR-U02-51). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Giá trị lọc " + e.getName() + " không hợp lệ");
        detail.setProperty("code", "AUDIT_FILTER_INVALID");
        return detail;
    }
}
