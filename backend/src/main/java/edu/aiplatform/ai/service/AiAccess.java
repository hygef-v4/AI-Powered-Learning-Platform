package edu.aiplatform.ai.service;

import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Kiểm quyền chung của U06 qua U01; ngoài quyền trả {@code 404} và ghi audit (BR-U06-51); U01 lỗi thì từ chối
 * (fail closed, BR-U06-53).
 */
@Component
public class AiAccess {

    public static final String NOT_FOUND = "AI_NOT_FOUND";

    private final IAuthorization authorization;
    private final IAudit audit;
    private final Clock clock;

    public AiAccess(IAuthorization authorization, IAudit audit, Clock clock) {
        this.authorization = authorization;
        this.audit = audit;
        this.clock = clock;
    }

    public void require(Actor actor, String action) {
        if (!allowed(actor, action)) {
            audit.record(new AuditEvent(UUID.randomUUID(), actor == null ? null : actor.accountId(), "ACCESS_DENIED",
                    "AI", null, AuditResult.DENIED, action, Map.of(), clock.instant()));
            throw notFound();
        }
    }

    /** Kiểm mà không audit, dùng khi chỉ cần biết có được dùng một nguồn hay không. */
    public boolean allowed(Actor actor, String action) {
        try {
            return actor != null && authorization.authorize(actor, action, ResourceRef.none()).allowed();
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static ApiException notFound() {
        return ApiException.notFound(NOT_FOUND);
    }
}
