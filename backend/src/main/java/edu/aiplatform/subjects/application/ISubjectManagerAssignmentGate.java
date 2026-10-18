package edu.aiplatform.subjects.application;

import java.util.List;
import java.util.UUID;

/**
 * Port NỘI BỘ U04 chờ adapter tới contract khóa account của U01, chưa phải contract bàn giao.
 * Phải khóa account theo danh sách UUID đã sort, cùng transaction PostgreSQL với caller,
 * giữ khóa đến commit/rollback. Khóa cả account cũ (có thể disabled), không chỉ account mới.
 * Không dùng synchronized, cache, transaction riêng hoặc contact lookup để giả khóa.
 * U01 disable/đổi role cũng phải tuân thủ account trước subject. Chưa có provider thì fail closed.
 */
public interface ISubjectManagerAssignmentGate {
    void lockAccounts(List<UUID> accountIds);
}
