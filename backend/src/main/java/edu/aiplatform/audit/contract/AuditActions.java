package edu.aiplatform.audit.contract;

import java.util.Locale;

/**
 * Mã hành động audit kèm mã đối tượng, ví dụ {@code SUBJECT_CS101_CREATED}, {@code CLASS_SE1801_ARCHIVED}
 * (yêu cầu 2026-11-26). Màn Log tách lại phần mã đối tượng để hiện "Tạo môn CS101".
 */
public final class AuditActions {

    private AuditActions() {
    }

    /** {@code code} rỗng thì bỏ phần mã: {@code SUBJECT_CREATED}. Khoảng trắng trong mã đổi thành {@code -}. */
    public static String buildObjectAction(String prefix, String code, String event) {
        String normalized = code == null ? "" : code.trim().replaceAll("\\s+", "-").toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? prefix + "_" + event : prefix + "_" + normalized + "_" + event;
    }
}
