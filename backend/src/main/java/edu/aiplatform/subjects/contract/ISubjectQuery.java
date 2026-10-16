package edu.aiplatform.subjects.contract;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Đọc môn cho U05, U07, U10, U13, U15. */
public interface ISubjectQuery {

    Optional<SubjectRef> getSubject(UUID subjectId);

    boolean isActive(UUID subjectId);

    List<SubjectRef> listManagedSubjects(UUID accountId);

    Map<SubjectStatus, Long> countSubjectsByStatus();
}
