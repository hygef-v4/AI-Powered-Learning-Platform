package edu.aiplatform.audit.contract;

import edu.aiplatform.identity.contract.Actor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Tra cứu audit cho Admin (UC 13); mỗi trang tối đa 100 dòng. */
public interface IAuditQuery {

    /** Tra cứu và ghi một lần xem audit ({@code AUDIT_QUERIED}). */
    default Page<AuditLogView> query(Actor actor, AuditFilter filter, Pageable page) {
        return query(actor, filter, page, true);
    }

    /**
     * {@code recordView = false}: chỉ đổi bộ lọc hoặc chuyển trang trên màn đã mở, không ghi thêm {@code AUDIT_QUERIED};
     * lần mở màn vẫn ghi. Quyền vẫn kiểm mọi lần, bị từ chối vẫn ghi {@code ACCESS_DENIED}.
     */
    Page<AuditLogView> query(Actor actor, AuditFilter filter, Pageable page, boolean recordView);
}
