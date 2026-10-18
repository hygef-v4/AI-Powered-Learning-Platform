package edu.aiplatform.subjects.contract;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * U04 khai báo, U05 cài; contract mới chờ thống nhất provider, không sửa ISubjectClasses hiện có.
 * Kiểm count và blockers trên cùng thread/transaction READ_COMMITTED đang giữ khóa subject của caller.
 * Provider phải áp dụng remainingBudget vào TỔNG query/lock waits, không mở transaction/worker riêng.
 * U05 writers phải dùng ISubjectMutationGuard; chỉ đọc count mà writers bỏ guard không chống được race.
 * Lỗi/timeout không được đổi thành0/rỗng. Không có default implementation/stub thành công.
 */
public interface ISubjectArchiveCheck {
    Result checkArchive(UUID subjectId, Duration remainingBudget);

    /** count gồm OPEN và DRAFT; blockers có thể là tập con, không chứa lớp ARCHIVED. */
    record Result(int unarchivedCount, List<SubjectClassSummary> blockingClasses) {
        public Result {
            blockingClasses = List.copyOf(blockingClasses);
            if (unarchivedCount < blockingClasses.size()
                    || blockingClasses.stream().anyMatch(item -> !List.of("OPEN", "DRAFT").contains(item.status()))) {
                throw new IllegalArgumentException("Kết quả kiểm archive không hợp lệ");
            }
        }
    }
}
