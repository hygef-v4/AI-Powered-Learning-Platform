package edu.aiplatform.academics.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Nội bộ U05. Mọi command cần transaction, khóa môn và authorization trước khi ghi. */
public interface ICourseClassRepository {
    Optional<CourseClass> find(UUID classId);
    List<CourseClass> listBySubject(UUID subjectId, int limit, int offset);
    void insert(CourseClass value);
    void update(CourseClass value, long expectedVersion);
}
