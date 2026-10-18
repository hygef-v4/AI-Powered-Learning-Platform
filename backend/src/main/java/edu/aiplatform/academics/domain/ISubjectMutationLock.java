package edu.aiplatform.academics.domain;

import java.util.UUID;

/** Khóa theo môn trước khi đọc/khóa lớp và ghi danh; giữ tới cuối transaction. */
public interface ISubjectMutationLock {
    void acquire(UUID subjectId);
}
