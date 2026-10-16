package edu.aiplatform.jobs.contract;

/** Sự kiện thông báo trên exchange {@code platform.events}; chỉ dùng cho thông báo (U14 nghe). */
public final class EventTypes {

    /** U05. */
    public static final String ENROLLMENT_ACTIVATED = "enrollment.activated";
    /** U07. */
    public static final String ASSIGNMENT_OPENED = "assignment.opened";
    /** U09. */
    public static final String GRADE_PUBLISHED = "grade.published";
    /** U10. */
    public static final String QUIZ_PUBLISHED = "quiz.published";
    /** U12. */
    public static final String GROUP_MEMBERSHIP_CHANGED = "group.membership-changed";
    /** U12. */
    public static final String GROUP_LEADER_CHANGE_REQUESTED = "group.leader-change-requested";
    /** U12. */
    public static final String GROUP_LEADER_CHANGED = "group.leader-changed";
    /** U12. */
    public static final String GROUP_LEADER_REQUEST_REJECTED = "group.leader-request-rejected";
    /** U12. */
    public static final String GROUP_SUBMITTED = "group.submitted";
    /** U14. */
    public static final String CLASS_ANNOUNCEMENT_POSTED = "class.announcement-posted";
    /** U03. */
    public static final String BACKUP_FAILED = "backup.failed";

    private EventTypes() {
    }
}
