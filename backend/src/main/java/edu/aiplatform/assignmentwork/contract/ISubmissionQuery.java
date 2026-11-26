package edu.aiplatform.assignmentwork.contract;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    Optional<SubmissionView> getSubmission(UUID attemptId);

    List<UUID> listStudentsWithoutSubmission(UUID assignmentId, UUID classId);
}
