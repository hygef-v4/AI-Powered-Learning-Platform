package edu.aiplatform.identity.service;

import edu.aiplatform.identity.contract.Role;

/**
 * Đích sau đăng nhập/kích hoạt theo role (F15, BR-U01-48, Q13): Student, Teacher vào Class Dashboard (U05), Chủ nhiệm
 * môn vào Manager Dashboard (U04/U05); Admin vào Admin Dashboard (U15).
 */
public final class HomePaths {

    public static final String CLASS_DASHBOARD = "/classes";
    public static final String MANAGER_DASHBOARD = "/manager";
    public static final String ADMIN_HOME = "/admin";

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
