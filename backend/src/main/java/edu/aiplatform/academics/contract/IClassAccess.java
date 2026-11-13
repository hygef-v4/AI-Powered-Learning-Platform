package edu.aiplatform.academics.contract;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Thông tin lớp, sinh viên đang học, Chủ nhiệm môn của lớp, lớp đang mở của một người hoặc một môn, số đếm.
 * Dùng bởi U07–U15. {@code isClassManager} chỉ đúng với Chủ nhiệm môn của môn chứa lớp.
 */
public interface IClassAccess {

    /** Loads the lightweight class reference for a class ID. */
    Optional<ClassRef> getClassRef(UUID classId);

    /** Checks whether the account is actively enrolled as a student in the class. */
    boolean isActiveStudent(UUID accountId, UUID classId);

    /** Lists active student account IDs enrolled in the class. */
    List<UUID> listActiveStudents(UUID classId);

    /** Checks whether the account manages the class subject. */
    boolean isClassManager(UUID accountId, UUID classId);

    /** Lists open classes associated with the account. */
    List<ClassRef> listOpenClassesOf(UUID accountId);

    /** Lists open classes belonging to the subject. */
    List<ClassRef> listOpenClassesOfSubject(UUID subjectId);

    /** Counts classes grouped by lifecycle status. */
    Map<ClassStatus, Long> countClassesByStatus();

    /** Counts active enrollments across the platform. */
    long countActiveEnrollments();
}
