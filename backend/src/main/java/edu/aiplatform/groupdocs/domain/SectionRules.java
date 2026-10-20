package edu.aiplatform.groupdocs.domain;

import edu.aiplatform.groupdocs.domain.GroupSection.Status;
import java.time.Duration;
import java.util.UUID;

/** Quy tắc thao tác trên mục (BR-U12-32, 33, 36); trả mã lỗi hoặc {@code null} khi được phép. */
public final class SectionRules {

    /** Lần lưu nháp cuối vẫn nhận trong 30 giây sau hạn (BR-U12-32). */
    public static final Duration GRACE = Duration.ofSeconds(30);

    private SectionRules() {
    }

    /** Thành viên tự nhận mục {@code OPEN} hoặc {@code DONE}; mục người khác đang giữ thì không. */
    public static String canClaim(GroupSection section, UUID actor) {
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

    /** Chỉ người giữ lưu nháp và bấm Xong. */
    public static String canEdit(GroupSection section, UUID actor) {
        return section.status() == Status.CLAIMED && actor.equals(section.claimedBy()) ? null : "SECTION_NOT_HELD";
    }
}
