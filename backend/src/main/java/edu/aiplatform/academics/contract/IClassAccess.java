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

    Optional<ClassRef> getClassRef(UUID classId);

    boolean isActiveStudent(UUID accountId, UUID classId);

    List<UUID> listActiveStudents(UUID classId);

    boolean isClassManager(UUID accountId, UUID classId);

    List<ClassRef> listOpenClassesOf(UUID accountId);

    List<ClassRef> listOpenClassesOfSubject(UUID subjectId);

    Map<ClassStatus, Long> countClassesByStatus();

    long countActiveEnrollments();
}
