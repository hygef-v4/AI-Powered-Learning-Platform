package edu.aiplatform.subjects.contract;

import java.time.Duration;
import java.util.UUID;

/**
 * U04 cài, U05 gọi trước create/open/reopen lớp, sau các khóa account nếu có.
 * Cần transaction READ_COMMITTED của caller: khóa subject giữ tới commit/rollback, không REQUIRES_NEW.
 * Chỉ trả khi subject ACTIVE; missing/archived/timeout đều chặn mutation. Không cấp quyền người dùng.
 * timeout là ngân sách chờ khóa, tối đa1s. Contract mới chờ U05 xác nhận và nối writer thật.
 */
public interface ISubjectMutationGuard {
    SubjectRef lockActive(UUID subjectId, Duration timeout);
}
