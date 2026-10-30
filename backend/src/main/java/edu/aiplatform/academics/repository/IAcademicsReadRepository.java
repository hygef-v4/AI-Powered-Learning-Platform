package edu.aiplatform.academics.repository;

import edu.aiplatform.academics.model.CourseClass;

import edu.aiplatform.academics.contract.ClassStatus;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** U05-only scoped read queries; limits/offsets validated by persistence. */
public interface IAcademicsReadRepository {
    List<CourseClass> listForManager(Collection<UUID> subjectIds, String query, ClassStatus status, int limit, int offset);
    List<CourseClass> listForTeacher(UUID accountId, String query, ClassStatus status, int limit, int offset);
    List<CourseClass> listForStudent(UUID accountId, String query, ClassStatus status, int limit, int offset);
    List<CourseClass> listTaught(UUID accountId);
    List<CourseClass> listOfSubject(UUID subjectId);
    Map<ClassStatus, Long> countClassesByStatus();
    Map<UUID, Integer> countClassesOfSubjects(Collection<UUID> subjectIds);
    int countUnarchivedClasses(UUID subjectId);
    long countActiveEnrollments();
    Map<UUID, EnrollmentCounts> enrollmentCounts(Collection<UUID> classIds);

    record EnrollmentCounts(long active, long removed) { }
}
