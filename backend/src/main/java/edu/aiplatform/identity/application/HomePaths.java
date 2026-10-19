package edu.aiplatform.identity.application;

import edu.aiplatform.identity.contract.Role;

/**
 * Đích sau đăng nhập/kích hoạt theo role (F15, BR-U01-48, Q13). Các màn đích chưa có trên nhánh này nên tạm trỏ về màn
 * đã có, để người dùng không rơi vào trang 404 không có khung và nút đăng xuất:
 * Admin Dashboard (U15) chưa có → Admin vào Account List; Class Dashboard (U05) và Manager Dashboard (U04) chưa merge →
 * Student, Teacher, Chủ nhiệm môn vào User Profile. Khi merge các màn đó, đổi {@link #of} về {@link #CLASS_DASHBOARD},
 * {@link #MANAGER_DASHBOARD} (và {@code /admin} khi U15 xong).
 */
public final class HomePaths {

    public static final String CLASS_DASHBOARD = "/classes";
    public static final String MANAGER_DASHBOARD = "/manager";
    public static final String ADMIN_HOME = "/admin/accounts";
    /** Đích tạm cho Student, Teacher, Chủ nhiệm môn tới khi U04/U05 có màn. */
    public static final String PROFILE = "/profile";

    private HomePaths() {
    }

    public static String of(Role role) {
        return switch (role) {
            case STUDENT, TEACHER, SUBJECT_MANAGER -> PROFILE;
            case ADMIN -> ADMIN_HOME;
        };
    }
}
