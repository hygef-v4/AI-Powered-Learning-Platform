package edu.aiplatform.identity.contract;

import java.util.List;
import java.util.UUID;

/** U01 khai báo, U04 cài: phạm vi Chủ nhiệm môn (R2); chưa có U04 thì trả "không có phạm vi". */
public interface ISubjectScope {

    boolean isSubjectManager(UUID accountId, UUID subjectId);

    List<UUID> listManagedSubjects(UUID accountId);

    /**
     * Gỡ tài khoản khỏi vai trò Chủ nhiệm của mọi môn, kể cả môn đã lưu trữ (BR-U04-12). U01 gọi khi vô hiệu hóa
     * tài khoản, trong cùng transaction; lỗi thì ném để vô hiệu hóa rollback. Trả id các môn đã gỡ.
     */
    List<UUID> releaseManagerAssignments(UUID accountId, Actor actor);
}
