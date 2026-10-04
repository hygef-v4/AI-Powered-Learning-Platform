package edu.aiplatform.audit.port;

import edu.aiplatform.shared.contract.ActorRef;
import edu.aiplatform.shared.contract.Page;
import edu.aiplatform.shared.contract.PageRequest;

/** Tra cứu audit cho ADMIN (UC 39, BR-U02-07, 08). */
public interface AuditQueryPort {
    Page<AuditEvent> query(ActorRef actor, AuditFilter filter, PageRequest page);
}
