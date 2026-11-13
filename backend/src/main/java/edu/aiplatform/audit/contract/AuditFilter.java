package edu.aiplatform.audit.contract;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * {@code actor} là một phần email hoặc tên người thực hiện (màn Audit Log lọc ngay khi gõ); {@code actorIds} là các
 * tài khoản khớp phần đó, do U02 tự tra qua U01, rỗng nghĩa là không ai khớp.
 */
public record AuditFilter(UUID actorId, String action, String objectType, String objectId,
                          AuditResult result, Instant from, Instant to, String actor, List<UUID> actorIds) {

    public AuditFilter(UUID actorId, String action, String objectType, String objectId, AuditResult result,
                       Instant from, Instant to) {
        this(actorId, action, objectType, objectId, result, from, to, null, null);
    }
}
