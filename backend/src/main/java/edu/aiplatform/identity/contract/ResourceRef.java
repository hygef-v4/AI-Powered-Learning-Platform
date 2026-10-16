package edu.aiplatform.identity.contract;

import java.util.UUID;

/** Đối tượng cần kiểm quyền; với môn hoặc lớp, U01 hỏi U04 hoặc U05 về phạm vi (F13). */
public record ResourceRef(ScopeKind scope, UUID id) {

    public static ResourceRef none() {
        return new ResourceRef(ScopeKind.NONE, null);
    }

    public static ResourceRef account(UUID accountId) {
        return new ResourceRef(ScopeKind.ACCOUNT, accountId);
    }

    public static ResourceRef subject(UUID subjectId) {
        return new ResourceRef(ScopeKind.SUBJECT, subjectId);
    }

    public static ResourceRef courseClass(UUID classId) {
        return new ResourceRef(ScopeKind.CLASS, classId);
    }
}
