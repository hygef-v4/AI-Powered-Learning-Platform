package edu.aiplatform.identity.contract;

/**
 * Tên action dùng với {@link IAuthorization#authorize} (Q14). Action không có trong bảng quyền của U01 bị từ chối;
 * unit thêm action mới bằng cách thêm hằng ở đây và một dòng trong {@code AccessPolicy} của U01.
 */
public final class Actions {

    // U01 — hồ sơ của chính mình (R1)
    public static final String PROFILE_VIEW = "PROFILE_VIEW";
    public static final String PROFILE_UPDATE = "PROFILE_UPDATE";
    public static final String PASSWORD_CHANGE = "PASSWORD_CHANGE";

    // U01 — quản trị tài khoản (chỉ ADMIN, BR-U01-67)
    public static final String ACCOUNT_LIST = "ACCOUNT_LIST";
    public static final String ACCOUNT_VIEW = "ACCOUNT_VIEW";
    public static final String ACCOUNT_CREATE = "ACCOUNT_CREATE";
    public static final String ACCOUNT_IMPORT = "ACCOUNT_IMPORT";
    public static final String ACCOUNT_UPDATE = "ACCOUNT_UPDATE";
    public static final String ACCOUNT_ROLE_CHANGE = "ACCOUNT_ROLE_CHANGE";
    public static final String ACCOUNT_STATUS_CHANGE = "ACCOUNT_STATUS_CHANGE";

    // U02 — tra cứu audit (BR-U02-07)
    public static final String AUDIT_READ = "AUDIT_READ";

    // U03 — tệp và cài đặt (BR-U03-04, 80)
    public static final String FILE_UPLOAD = "FILE_UPLOAD";
    public static final String SETTINGS_VIEW = "SETTINGS_VIEW";
    public static final String SETTINGS_EDIT = "SETTINGS_EDIT";

    // U04 — quản trị môn học (chỉ ADMIN, BR-U04-01)
    public static final String SUBJECT_LIST = "SUBJECT_LIST";
    public static final String SUBJECT_VIEW = "SUBJECT_VIEW";
    public static final String SUBJECT_CREATE = "SUBJECT_CREATE";
    public static final String SUBJECT_UPDATE = "SUBJECT_UPDATE";
    public static final String SUBJECT_MANAGER_CHANGE = "SUBJECT_MANAGER_CHANGE";
    public static final String SUBJECT_STATUS_CHANGE = "SUBJECT_STATUS_CHANGE";

    // U05 — lớp và ghi danh: vào màn theo vai trò, xem một lớp (R3/R5), quản lý lớp của môn (R2)
    public static final String CLASS_STUDENT = "CLASS_STUDENT";
    public static final String CLASS_TEACHER = "CLASS_TEACHER";
    public static final String CLASS_MANAGER = "CLASS_MANAGER";
    public static final String CLASS_VIEW = "CLASS_VIEW";
    public static final String CLASS_MANAGE = "CLASS_MANAGE";

    // U07 — soạn bài tập: bài của lớp (giảng viên chính, R3/R4), bài của môn (Chủ nhiệm môn, R2)
    public static final String ASSIGNMENT_CLASS_MANAGE = "ASSIGNMENT_CLASS_MANAGE";
    public static final String ASSIGNMENT_SUBJECT_MANAGE = "ASSIGNMENT_SUBJECT_MANAGE";

    // U08 — làm bài (sinh viên đang học lớp, R5); giảng viên lớp xem bài nộp (R3/R4)
    public static final String ASSIGNMENT_WORK = "ASSIGNMENT_WORK";
    public static final String ASSIGNMENT_ATTEMPT_REVIEW = "ASSIGNMENT_ATTEMPT_REVIEW";

    private Actions() {
    }
}
