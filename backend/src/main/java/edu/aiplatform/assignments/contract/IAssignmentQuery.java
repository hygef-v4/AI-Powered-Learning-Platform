package edu.aiplatform.assignments.contract;

import java.time.Instant;
import java.time.ZoneId;
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

    /**
     * Số bài đã giao (đã phát hành: lên lịch, đang mở, đã đóng, ngưng giao) theo ngày giao (giờ mở, chưa đặt giờ mở thì
     * ngày tạo) trong {@code [from, to)}, tính ngày theo {@code zone}; dùng cho Tổng quan của Admin (U15).
     */
    default List<AssignmentDayCount> countGivenByDay(Instant from, Instant to, ZoneId zone) {
        return List.of();
    }
}
