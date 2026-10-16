package edu.aiplatform.audit.contract;

import edu.aiplatform.identity.contract.Actor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Tra cứu audit cho Admin (UC 13); mỗi trang tối đa 100 dòng. */
public interface IAuditQuery {

    Page<AuditLogView> query(Actor actor, AuditFilter filter, Pageable page);
}
