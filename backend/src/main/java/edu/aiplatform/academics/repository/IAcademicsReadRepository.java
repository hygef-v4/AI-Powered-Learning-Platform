package edu.aiplatform.academics.repository;

import edu.aiplatform.academics.model.CourseClass;

import edu.aiplatform.academics.contract.ClassStatus;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** U05-only scoped read queries; limits/offsets validated by persistence. */
public interface IAcademicsReadRepository {
    /** Lists classes in the manager's subjects matching the supplied filters. */
    List<CourseClass> listForManager(Collection<UUID> subjectIds, String query, ClassStatus status,
            int limit, int offset);

    /** Lists classes assigned to a teacher matching the supplied filters. */
    List<CourseClass> listForTeacher(UUID accountId, String query, ClassStatus status, int limit,
            int offset);

    /** Lists classes in which a student is enrolled. */
    List<CourseClass> listForStudent(UUID accountId, String query, ClassStatus status, int limit,
            int offset);

    /** Lists all classes assigned to a teacher. */
    List<CourseClass> listTaught(UUID accountId);

    /** Lists all classes belonging to a subject. */
    List<CourseClass> listOfSubject(UUID subjectId);

    /** Counts classes grouped by lifecycle status. */
    Map<ClassStatus, Long> countClassesByStatus();

    /** Counts classes separately for the requested subjects. */
    Map<UUID, Integer> countClassesOfSubjects(Collection<UUID> subjectIds);

    /** Counts non-archived classes belonging to a subject. */
    int countUnarchivedClasses(UUID subjectId);

    /** Counts active enrollments across all classes. */
    long countActiveEnrollments();

    /** Returns active and removed enrollment counts keyed by class ID. */
    Map<UUID, EnrollmentCounts> enrollmentCounts(Collection<UUID> classIds);

    record EnrollmentCounts(long active, long removed) {
    }
}
