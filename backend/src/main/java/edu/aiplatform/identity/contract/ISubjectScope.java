package edu.aiplatform.identity.contract;

import java.util.List;
import java.util.UUID;

/** U01 khai báo, U04 cài: phạm vi Chủ nhiệm môn (R2); chưa có U04 thì trả "không có phạm vi". */
public interface ISubjectScope {

    boolean isSubjectManager(UUID accountId, UUID subjectId);

    List<UUID> listManagedSubjects(UUID accountId);
}
