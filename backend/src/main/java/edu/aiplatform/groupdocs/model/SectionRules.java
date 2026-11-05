package edu.aiplatform.groupdocs.model;

import edu.aiplatform.groupdocs.model.GroupSection.Status;
import java.time.Duration;
import java.util.UUID;

/**
 * Quy tắc thao tác trên mục (BR-U12-32, 33, 36; giai đoạn phân công FB-U12-01); trả mã lỗi hoặc {@code null} khi được
 * phép. {@code working}: trưởng nhóm đã chốt phân công.
 */
public final class SectionRules {

    /** Lần lưu nháp cuối vẫn nhận trong 30 giây sau hạn (BR-U12-32). */
    public static final Duration GRACE = Duration.ofSeconds(30);

    private SectionRules() {
    }

    /**
     * Nhận mục. Chưa chốt phân công: nhận làm người phụ trách mục chưa có ai; mục người khác đã nhận thì không. Đã chốt:
     * chỉ người phụ trách bắt đầu soạn mục của mình.
     */
    public static String canClaim(GroupSection section, UUID actor, boolean working) {
        if (!working) {
            return section.assigneeId() == null || section.assignedTo(actor) ? null : "SECTION_CLAIMED_BY_OTHER";
        }
        if (section.assignedTo(actor)) {
            return section.status() == Status.CLAIMED && !actor.equals(section.claimedBy()) ? "SECTION_CLAIMED_BY_OTHER" : null;
        }
        return section.assigneeId() == null ? "ASSIGNMENT_LOCKED" : "SECTION_ASSIGNED_TO_OTHER";
    }

    /** Trưởng nhóm giao hoặc đổi người phụ trách chỉ khi chưa chốt phân công. */
    public static String canAssign(boolean working) {
        return working ? "ASSIGNMENT_LOCKED" : null;
    }

    /**
     * Nhả mục. Chưa chốt phân công: người phụ trách, trưởng nhóm hoặc giảng viên bỏ người phụ trách. Đã chốt: người giữ,
     * trưởng nhóm hoặc giảng viên dừng soạn (bản nháp bị bỏ, BR-U12-36), người phụ trách giữ nguyên.
     */
    public static String canRelease(GroupSection section, UUID actor, boolean leader, boolean teacher, boolean working) {
        if (!working) {
            if (section.assigneeId() == null && section.status() != Status.CLAIMED) {
                return "SECTION_NOT_CLAIMED";
            }
            return section.assignedTo(actor) || actor.equals(section.claimedBy()) || leader || teacher
                    ? null : "SECTION_RELEASE_FORBIDDEN";
        }
        if (section.status() != Status.CLAIMED) {
            return "SECTION_NOT_CLAIMED";
        }
        return actor.equals(section.claimedBy()) || leader || teacher ? null : "SECTION_RELEASE_FORBIDDEN";
    }

    /** Chỉ người giữ lưu nháp và bấm Xong, và chỉ sau khi chốt phân công. */
    public static String canEdit(GroupSection section, UUID actor, boolean working) {
        if (!working) {
            return "ASSIGNMENT_NOT_LOCKED";
        }
        return section.status() == Status.CLAIMED && actor.equals(section.claimedBy()) ? null : "SECTION_NOT_HELD";
    }
}
