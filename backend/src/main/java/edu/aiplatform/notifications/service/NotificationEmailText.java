package edu.aiplatform.notifications.service;

import edu.aiplatform.notifications.model.NotificationType;

/**
 * Tiêu đề và nội dung email thông báo song ngữ: tiếng Việt trước, tiếng Anh sau (BR-U14-23). Hệ thống chưa lưu ngôn
 * ngữ ưa thích theo tài khoản nên một email phục vụ cả hai ngôn ngữ. Câu tiếng Anh khớp nhãn chuông ở frontend.
 */
final class NotificationEmailText {
    private static final String VIETNAMESE_BODY =
            "Bạn có thông báo mới trên nền tảng học tập. Đăng nhập để xem: ";
    private static final String ENGLISH_BODY =
            "You have a new notification on the learning platform. Sign in to view it: ";
    private static final String ENGLISH_FALLBACK_TITLE = "You have a new notification";

    private NotificationEmailText() {}

    static String buildSubject(NotificationType type, String vietnameseTitle) {
        return vietnameseTitle + " / " + resolveEnglishTitle(type);
    }

    static String buildBody(String url) {
        return VIETNAMESE_BODY + url + "\n\n" + ENGLISH_BODY + url;
    }

    static String resolveEnglishTitle(NotificationType type) {
        if (type == null) {
            return ENGLISH_FALLBACK_TITLE;
        }
        return switch (type) {
            case ENROLLED -> "You have been enrolled in a class";
            case ASSIGNMENT_OPENED -> "A new assignment is open";
            case QUIZ_PUBLISHED -> "A new quiz is available";
            case GRADE_PUBLISHED -> "You have a new grade";
            case DEADLINE_REMINDER -> "An assignment is due soon";
            case ANNOUNCEMENT -> "New class announcement";
            case BACKUP_FAILED -> "System backup failed";
            case LEADER_CHANGE_REQUESTED -> "A group leader change is requested";
            case LEADER_CHANGED -> "The group leader has changed";
            case LEADER_REQUEST_REJECTED -> "The group leader change was rejected";
            case GROUP_MEMBERSHIP_CHANGED -> "Group members have changed";
            case GROUP_SUBMITTED -> "Your group has submitted";
            case GROUP_PROPOSAL_INVITED -> "You are invited to a group";
            case GROUP_PROPOSAL_SUBMITTED -> "A group proposal is awaiting approval";
            case GROUP_PROPOSAL_APPROVED -> "Group proposal approved";
            case GROUP_PROPOSAL_REJECTED -> "Group proposal rejected";
            case QUESTION_PROPOSED -> "A question is awaiting approval";
            case QUESTION_REVIEWED -> "Your proposed question has been reviewed";
        };
    }
}
