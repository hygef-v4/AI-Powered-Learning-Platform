package edu.aiplatform.groups.model;

import edu.aiplatform.academics.contract.ClassStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Đợt sinh viên tự chia nhóm của một lớp (BR-U12-70, 71). Đợt mở khi bật, lớp {@code OPEN} và chưa tới hạn chót;
 * trạng thái tính mỗi lần đọc/ghi, không có job đóng đợt.
 */
public record GroupFormationSettings(UUID classId, boolean enabled, int minSize, int maxSize, Instant deadline,
                                     UUID updatedBy, Instant updatedAt, long version) {

    public static final int MIN_SIZE = 2;
    public static final int MAX_SIZE = 20;
    public static final int DEFAULT_MIN_SIZE = 2;
    public static final int DEFAULT_MAX_SIZE = 5;

    /** Lý do đợt đóng; {@code null} khi đợt mở. */
    public enum ClosedReason {
        DISABLED, CLASS_NOT_OPEN, DEADLINE_PASSED
    }

    /** Lớp chưa có cài đặt: coi như tắt. */
    public static GroupFormationSettings defaults(UUID classId) {
        return new GroupFormationSettings(classId, false, DEFAULT_MIN_SIZE, DEFAULT_MAX_SIZE, null, null, null, 0);
    }

    public static boolean isValidSize(int size) {
        return size >= MIN_SIZE && size <= MAX_SIZE;
    }

    public ClosedReason closedReason(ClassStatus classStatus, Instant now) {
        if (!enabled) {
            return ClosedReason.DISABLED;
        }
        if (classStatus != ClassStatus.OPEN) {
            return ClosedReason.CLASS_NOT_OPEN;
        }
        if (deadline != null && !now.isBefore(deadline)) {
            return ClosedReason.DEADLINE_PASSED;
        }
        return null;
    }

    public boolean isOpen(ClassStatus classStatus, Instant now) {
        return closedReason(classStatus, now) == null;
    }
}
