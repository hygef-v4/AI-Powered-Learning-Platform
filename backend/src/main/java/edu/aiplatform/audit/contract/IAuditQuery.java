package edu.aiplatform.audit.contract;

import edu.aiplatform.identity.contract.Actor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Tra cứu audit cho Admin (UC 13); mỗi trang tối đa 100 dòng. */
public interface IAuditQuery {

    /**
     * Tra cứu không ghi audit (yêu cầu 2026-11-25: nhật ký chỉ giữ sự kiện quan trọng); quyền vẫn kiểm mọi lần,
     * bị từ chối vẫn ghi {@code ACCESS_DENIED}. Các dòng {@code AUDIT_QUERIED} cũ không hiện nữa.
     */
    Page<AuditLogView> query(Actor actor, AuditFilter filter, Pageable page);
}
