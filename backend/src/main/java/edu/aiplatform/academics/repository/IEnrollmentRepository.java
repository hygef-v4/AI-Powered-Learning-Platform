package edu.aiplatform.academics.repository;

import edu.aiplatform.academics.model.Enrollment;
import edu.aiplatform.academics.model.EnrollmentStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Chỉ dùng trong U05; các phép ghi cần cùng khóa môn với đổi trạng thái lớp. */
public interface IEnrollmentRepository {
    Optional<Enrollment> find(UUID classId, UUID accountId);
    List<Enrollment> listByClass(UUID classId, boolean activeOnly, int limit, int offset);
    List<UUID> conflictingStudents(UUID classId, UUID subjectId);
    boolean activeInOtherClass(UUID accountId, UUID subjectId, UUID excludedClassId);
    void insert(Enrollment value);
    void update(Enrollment value, EnrollmentStatus expectedStatus);
}
