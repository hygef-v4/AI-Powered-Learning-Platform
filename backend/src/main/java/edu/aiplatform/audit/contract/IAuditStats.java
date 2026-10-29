package edu.aiplatform.audit.contract;

import java.time.Instant;
import java.util.List;

/**
 * Số liệu audit cho Tổng quan của Admin (U15). Bên gọi đã kiểm quyền Admin; ở đây không kiểm lại và không ghi
 * {@code AUDIT_QUERIED} vì Tổng quan tự tải lại định kỳ.
 */
public interface IAuditStats {

    /**
     * Số bản ghi {@code SUCCESS} của {@code action} có {@code occurredAt} trong {@code [from, to)}; {@code detailKey}
     * khác {@code null} thì chỉ đếm bản ghi có {@code details[detailKey] = detailValue}.
     */
    long countSucceeded(String action, Instant from, Instant to, String detailKey, String detailValue);

    /**
     * Sự việc đáng ngờ từ {@code from}, mới nhất trước, tối đa {@code limit} dòng: mọi lần bị từ chối ({@code DENIED})
     * cùng đăng nhập sai, khóa tạm, vô hiệu hóa tài khoản, đặt lại mật khẩu và xem token MCP.
     */
    List<AuditLogView> recentSecurityEvents(Instant from, int limit);
}
