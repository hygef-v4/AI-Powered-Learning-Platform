package edu.aiplatform.academics.repository;

import edu.aiplatform.academics.model.Enrollment;
import edu.aiplatform.academics.model.EnrollmentStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Chỉ dùng trong U05; các phép ghi cần cùng khóa môn với đổi trạng thái lớp. */
public interface IEnrollmentRepository {
    /** Loads an enrollment by class and account IDs. */
    Optional<Enrollment> find(UUID classId, UUID accountId);

    /** Lists a class's enrollments within the requested page. */
    List<Enrollment> listByClass(UUID classId, boolean activeOnly, int limit, int offset);

    /** Lists active students enrolled in another class of the same subject. */
    List<UUID> conflictingStudents(UUID classId, UUID subjectId);

    /** Checks for an active enrollment in another class of the same subject. */
    boolean activeInOtherClass(UUID accountId, UUID subjectId, UUID excludedClassId);

    /** Inserts a new enrollment. */
    void insert(Enrollment value);

    /** Updates an enrollment only when its stored status matches the expected status. */
    void update(Enrollment value, EnrollmentStatus expectedStatus);
}
