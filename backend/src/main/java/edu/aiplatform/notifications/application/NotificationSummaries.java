package edu.aiplatform.notifications.application;

import edu.aiplatform.notifications.domain.NotificationType;

/** Fixed in-app copy; never include private event payload or reuse this as email content. */
final class NotificationSummaries {
    private static final String LEGACY_SIGN_IN_SUMMARY = "Đăng nhập để xem thông tin liên quan.";

    private NotificationSummaries() { }

    static String forType(NotificationType type) {
        return switch (type) {
            case ENROLLED -> "Xem thông tin lớp và các hoạt động học tập.";
            case ASSIGNMENT_OPENED -> "Xem nội dung và hạn nộp bài.";
            case QUIZ_PUBLISHED -> "Mở Quiz để xem và bắt đầu luyện tập.";
            case GRADE_PUBLISHED -> "Xem kết quả chấm bài của bạn.";
            case DEADLINE_REMINDER -> "Hạn nộp bài còn khoảng 24 giờ.";
            case ANNOUNCEMENT -> "Xem nội dung thông báo từ giảng viên.";
            case LEADER_CHANGE_REQUESTED -> "Xem yêu cầu để phê duyệt hoặc từ chối.";
            case LEADER_CHANGED -> "Xem thông tin trưởng nhóm mới.";
            case LEADER_REQUEST_REJECTED -> "Xem trạng thái yêu cầu trong nhóm của bạn.";
            case GROUP_MEMBERSHIP_CHANGED -> "Xem danh sách thành viên mới nhất.";
            case GROUP_SUBMITTED -> "Xem trạng thái nộp bài của nhóm.";
            case BACKUP_FAILED -> "Xem chi tiết lần sao lưu thất bại để kiểm tra và xử lý.";
        };
    }

    static String forDisplay(NotificationType type, String storedSummary) {
        // Adapt old boilerplate on read, without rewriting notification history/read state.
        return LEGACY_SIGN_IN_SUMMARY.equals(storedSummary) ? forType(type) : storedSummary;
    }
}
