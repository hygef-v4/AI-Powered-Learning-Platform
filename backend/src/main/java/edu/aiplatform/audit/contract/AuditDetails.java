package edu.aiplatform.audit.contract;

import edu.aiplatform.identity.contract.Actor;
import java.util.LinkedHashMap;
import java.util.Map;

/** Khóa {@code details} dùng chung để màn Log diễn giải lần bị từ chối. */
public final class AuditDetails {

    private AuditDetails() {
    }

    /**
     * {@code action}: thao tác bị chặn (bỏ qua khi rỗng); {@code actorRole}: vai trò người gọi lúc bị chặn, vì vai trò có
     * thể đổi sau đó.
     */
    public static Map<String, Object> describeDenial(Actor actor, String action) {
        Map<String, Object> details = new LinkedHashMap<>();
        if (action != null && !action.isBlank()) {
            details.put("action", action);
        }
        if (actor != null && actor.role() != null) {
            details.put("actorRole", actor.role().name());
        }
        return details;
    }
}
