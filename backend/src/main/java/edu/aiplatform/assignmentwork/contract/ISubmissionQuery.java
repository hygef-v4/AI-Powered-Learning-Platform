package edu.aiplatform.assignmentwork.contract;

import edu.aiplatform.assignments.contract.GradingMode;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Chấm và theo dõi nộp bài (U09); nhắc hạn người chưa nộp (U14). */
public interface ISubmissionQuery {

    List<StudentSubmissionStatus> listSubmissions(UUID assignmentId, UUID classId);

    /**
     * {@link #listSubmissions} cho nhiều bài của cùng một lớp, theo thứ tự {@code assignmentIds} (bảng điểm). Bản mặc định
     * gọi từng bài; U08 đọc gộp một lần (TC-PERF-009).
     */
    default Map<UUID, List<StudentSubmissionStatus>> listSubmissionsByAssignment(Collection<UUID> assignmentIds,
                                                                               UUID classId) {
        Map<UUID, List<StudentSubmissionStatus>> result = new LinkedHashMap<>();
        for (UUID assignmentId : assignmentIds) {
            result.put(assignmentId, listSubmissions(assignmentId, classId));
        }
        return result;
    }

    /**
     * Lượt được chấm của một sinh viên, bằng {@code gradedAttemptId} của dòng sinh viên đó trong {@link #listSubmissions}.
     * U09 kiểm "lượt nộp mới nhất" cho từng bản chấm khi chốt/công bố hàng loạt; U08 đọc riêng sinh viên đó thay vì cả
     * lớp (TC-PERF-011).
     */
    default Optional<UUID> findGradedAttemptId(UUID assignmentId, UUID classId, UUID accountId) {
        return listSubmissions(assignmentId, classId).stream()
                .filter(status -> accountId.equals(status.accountId()))
                .map(StudentSubmissionStatus::gradedAttemptId).filter(Objects::nonNull).findFirst();
    }

    /**
     * {@link #findGradedAttemptId(UUID, UUID, UUID)} khi bên gọi đã đọc bài tập và biết chế độ chấm, khỏi đọc lại bài
     * cho từng bản chấm trong lượt chốt/công bố hàng loạt (TC-PERF-011).
     */
    default Optional<UUID> findGradedAttemptId(UUID assignmentId, GradingMode mode, UUID classId, UUID accountId) {
        return findGradedAttemptId(assignmentId, classId, accountId);
    }

    /**
     * Lượt được chấm của mọi sinh viên có lượt nộp trong lớp, theo tài khoản; cùng giá trị {@code gradedAttemptId} của
     * {@link #listSubmissions}. U09 đọc một lần cho cả lượt chốt/công bố hàng loạt khi bài đã đóng (TC-PERF-011).
     */
    default Map<UUID, UUID> findGradedAttemptIds(UUID assignmentId, GradingMode mode, UUID classId) {
        Map<UUID, UUID> result = new LinkedHashMap<>();
        for (StudentSubmissionStatus status : listSubmissions(assignmentId, classId)) {
            if (status.gradedAttemptId() != null) {
                result.put(status.accountId(), status.gradedAttemptId());
            }
        }
        return result;
    }

    Optional<SubmissionView> getSubmission(UUID attemptId);

    List<UUID> listStudentsWithoutSubmission(UUID assignmentId, UUID classId);
}
