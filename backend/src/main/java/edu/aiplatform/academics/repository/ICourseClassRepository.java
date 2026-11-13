package edu.aiplatform.academics.repository;

import edu.aiplatform.academics.model.CourseClass;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Nội bộ U05. Mọi command cần transaction, khóa môn và authorization trước khi ghi. */
public interface ICourseClassRepository {
    /** Loads a class by its ID. */
    Optional<CourseClass> find(UUID classId);

    /** Lists a subject's classes within the requested page. */
    List<CourseClass> listBySubject(UUID subjectId, int limit, int offset);

    /** Inserts a new class. */
    void insert(CourseClass value);

    /** Updates a class only when its stored version matches the expected version. */
    void update(CourseClass value, long expectedVersion);
}
