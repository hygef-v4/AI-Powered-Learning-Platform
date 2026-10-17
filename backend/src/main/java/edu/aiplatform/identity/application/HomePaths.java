package edu.aiplatform.identity.application;

import edu.aiplatform.identity.contract.Role;

/**
 * Đích sau đăng nhập/kích hoạt theo role (F15, BR-U01-48, Q13). Admin Dashboard (U15) chưa có nên Admin tạm vào
 * Account List; đổi thành {@code /admin} khi U15 xong.
 */
public final class HomePaths {

    public static final String CLASS_DASHBOARD = "/classes";
    public static final String MANAGER_DASHBOARD = "/manager";
    public static final String ADMIN_HOME = "/admin/accounts";

    private HomePaths() {
    }

    public static String of(Role role) {
        return switch (role) {
            case STUDENT, TEACHER -> CLASS_DASHBOARD;
            case SUBJECT_MANAGER -> MANAGER_DASHBOARD;
            case ADMIN -> ADMIN_HOME;
        };
    }
}
