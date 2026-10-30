package edu.aiplatform.subjects.repository;

import edu.aiplatform.subjects.model.Subject;
import edu.aiplatform.subjects.model.SubjectCode;

import edu.aiplatform.subjects.contract.SubjectStatus;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Boundary nội bộ U04; adapter JPA nằm trong package infrastructure, không được unit khác dùng trực tiếp. */
public interface ISubjectRepository {

    Optional<Subject> findById(UUID subjectId);

    /** Khóa subject trong transaction caller, chỉ sau khóa account khi đổi phân công. */
    Optional<Subject> findByIdForUpdate(UUID subjectId);

    /** READ_COMMITTED, khóa trong caller transaction, giới hạn lock/query wait; không async. */
    Optional<Subject> findByIdForUpdate(UUID subjectId, java.time.Duration timeout);

    boolean existsByCode(SubjectCode code);

    /** Một truy vấn theo khóa môn và manager_id hiện thời, không loại môn ARCHIVED. */
    boolean isManagedBy(UUID accountId, UUID subjectId);

    /** Mã hoặc tên chứa từ khóa literal; sắp theo code, trang đánh số từ 0, size tối đa 100. */
    List<Subject> findPage(String query, SubjectStatus status, int page, int size);

    long count(String query, SubjectStatus status);

    /** Đếm theo cùng query trước khi lọc status (Q08), luôn có cả hai trạng thái kể cả số 0. */
    Map<SubjectStatus, Long> countByStatus(String query);

    /** Gồm cả môn ARCHIVED để kiểm scope hiện thời và phục vụ gỡ phân công khi disable. */
    List<Subject> findManagedBy(UUID accountId);

    /** Unique constraint ở DB vẫn phải chặn hai request tạo cùng mã. */
    void insert(Subject subject);

    /**
     * Ghi snapshot đã thay đổi với điều kiện id/version hiện tại khớp expectedVersion.
     * Snapshot mới có public version = expectedVersion + 1; false nghĩa là mất cập nhật đồng thời.
     * Adapter B05 chuyển đổi public version và storage @Version, không tăng version hai lần.
     */
    boolean update(Subject subject, long expectedVersion);
}
