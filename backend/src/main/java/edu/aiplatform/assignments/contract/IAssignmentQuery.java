package edu.aiplatform.assignments.contract;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Đọc bài, lịch, nội dung đã khóa; biết còn nhận bài không; nhắc hạn. Dùng bởi U08, U09, U12, U14. */
public interface IAssignmentQuery {

    Optional<AssignmentView> get(UUID assignmentId);

    Optional<AssignmentContent> getContent(UUID assignmentId);

    List<AssignmentView> listForClass(UUID classId);

    SubmissionWindow submissionWindow(UUID assignmentId, Instant now);

    List<AssignmentView> listClosingBetween(Instant from, Instant to);
}
