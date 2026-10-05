package edu.aiplatform.assessments.port;

import java.util.List;
import java.util.UUID;

/** U08 khai báo, U09 cài (`C`); U10 dùng copy, U11/U15 đọc cấu hình. */
public interface TypeConfigPort {
    /** Đủ cấu hình để duyệt, gồm rubric từng câu/từng phần. */
    List<ReviewIssue> check(UUID assignmentId);
    /** Sao cấu hình, khung và nhân bản rubric (BR-U09-28). */
    void copy(UUID fromAssignmentId, UUID toAssignmentId);
    void repointRubric(UUID oldRubricId, UUID newRubricId);
    String readConfig(UUID assignmentId);
}
