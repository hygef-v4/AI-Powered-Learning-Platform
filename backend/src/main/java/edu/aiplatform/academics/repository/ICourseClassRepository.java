package edu.aiplatform.academics.repository;

import edu.aiplatform.academics.model.CourseClass;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Nội bộ U05. Mọi command cần transaction, khóa môn và authorization trước khi ghi. */
public interface ICourseClassRepository {
    /** Loads a class by its ID. */
    Optional<CourseClass> findClass(UUID classId);

    /** Lists a subject's classes within the requested page. */
    List<CourseClass> listBySubject(UUID subjectId, int limit, int offset);

    /** Inserts a new class. */
    // Ghi bản lớp mới bằng ID và trạng thái do nghiệp vụ tạo.
    void insert(CourseClass value);

    /** Updates a class only when its stored version matches the expected version. */
    void updateClass(CourseClass value, long expectedVersion);
}
