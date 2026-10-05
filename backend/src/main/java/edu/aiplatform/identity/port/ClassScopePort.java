package edu.aiplatform.identity.port;

import java.util.List;
import java.util.UUID;

/** U01 khai báo, U04 cài (`C`). */
public interface ClassScopePort {
    boolean isTeacherOf(UUID accountId, UUID classId);
    UUID subjectOfClass(UUID classId);
    /** Lớp còn làm giảng viên chính, dùng để chặn hạ role (BR-U01-64). */
    List<UUID> listTaughtClasses(UUID accountId);
}
