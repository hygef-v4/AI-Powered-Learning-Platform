package edu.aiplatform.groupdocs.model;

import edu.aiplatform.groupdocs.model.GroupSection.Status;
import java.time.Duration;
import java.util.UUID;

/** Quy tắc thao tác trên mục (BR-U12-32, 33, 36); trả mã lỗi hoặc {@code null} khi được phép. */
public final class SectionRules {

    /** Lần lưu nháp cuối vẫn nhận trong 30 giây sau hạn (BR-U12-32). */
    public static final Duration GRACE = Duration.ofSeconds(30);

    private SectionRules() {
    }

    /**
     * Thành viên tự nhận mục {@code OPEN} hoặc {@code DONE}; mục người khác đang giữ thì không; mục trưởng nhóm đã
     * chốt thì không (FB-U12-01).
     */
    public static String canClaim(GroupSection section, UUID actor) {
        if (section.locked()) {
            return "SECTION_LOCKED";
        }
        if (section.status() == Status.CLAIMED) {
            return actor.equals(section.claimedBy()) ? null : "SECTION_CLAIMED_BY_OTHER";
        }
        return null;
    }

    /** Người giữ, trưởng nhóm hoặc giảng viên của lớp nhả được mục đang giữ. */
    public static String canRelease(GroupSection section, UUID actor, boolean leader, boolean teacher) {
        if (section.status() != Status.CLAIMED) {
            return "SECTION_NOT_CLAIMED";
        }
        return actor.equals(section.claimedBy()) || leader || teacher ? null : "SECTION_RELEASE_FORBIDDEN";
    }


    /** Trưởng nhóm giao mục chưa chốt (BR-U12-33; chốt mục: FB-U12-01). */
    public static String canAssign(GroupSection section) {
        return section.locked() ? "SECTION_LOCKED" : null;
    }

    /** Trưởng nhóm chốt mục đã Xong (FB-U12-01); mục đang có người giữ hay chưa có nội dung thì không. */
    public static String canLock(GroupSection section) {
        return section.status() == Status.DONE ? null : "SECTION_NOT_DONE";
    }

    /** Chỉ người giữ lưu nháp và bấm Xong. */
    public static String canEdit(GroupSection section, UUID actor) {
        return section.status() == Status.CLAIMED && actor.equals(section.claimedBy()) ? null : "SECTION_NOT_HELD";
    }
}
