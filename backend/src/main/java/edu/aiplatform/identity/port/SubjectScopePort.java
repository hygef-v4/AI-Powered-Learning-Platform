package edu.aiplatform.identity.port;

import java.util.List;
import java.util.UUID;

/** U01 khai báo, U04 cài (`C`); U06, U10 dùng kiểm Chủ nhiệm môn. */
public interface SubjectScopePort {
    boolean isSubjectManager(UUID accountId, UUID subjectId);
    /** Môn còn phụ trách, dùng để chặn hạ role (BR-U01-64). */
    List<UUID> listManagedSubjects(UUID accountId);
}
