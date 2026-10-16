package edu.aiplatform.identity.contract;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** U01 khai báo, U05 cài: phạm vi giảng viên lớp (R3/R4) và sinh viên đang học (R5). */
public interface IClassScope {

    boolean isTeacherOf(UUID accountId, UUID classId);

    boolean isEnrolledStudent(UUID accountId, UUID classId);

    Optional<UUID> subjectOfClass(UUID classId);

    List<UUID> listTaughtClasses(UUID accountId);
}
