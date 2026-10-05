package edu.aiplatform.academics.port;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** U04 cung cấp cho U05, U06, U08-U16. */
public interface ClassAccessPort {
    Optional<ClassRef> getClassRef(UUID classId);
    boolean isActiveStudent(UUID accountId, UUID classId);
    List<UUID> listActiveStudents(UUID classId);
    /** Giảng viên chính, Chủ nhiệm môn của môn hoặc ADMIN (BR-U04-13). */
    boolean isClassManager(UUID accountId, UUID classId);
    Map<SubjectStatus, Long> countSubjectsByStatus();
    Map<ClassStatus, Long> countClassesByStatus();
    long countActiveEnrollments();
}
