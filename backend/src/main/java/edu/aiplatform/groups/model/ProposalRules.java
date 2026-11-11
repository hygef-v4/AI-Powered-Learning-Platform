package edu.aiplatform.groups.model;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Quy tắc thuần của đề xuất nhóm: tên, lý do, chỗ còn, thành viên hiệu lực (BR-U12-75…89). */
public final class ProposalRules {

    public static final int MAX_NAME = 100;
    public static final int MAX_REASON = 1000;

    private ProposalRules() {
    }

    /** Tên đã cắt khoảng trắng, hoặc {@code null} khi trống hay quá 100 ký tự. */
    public static String cleanName(String raw) {
        if (raw == null) {
            return null;
        }
        String name = raw.trim();
        return name.isEmpty() || name.length() > MAX_NAME ? null : name;
    }

    /** Khóa so trùng tên không phân biệt hoa thường. */
    public static String nameKey(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    /** Lý do tùy chọn: trống thành {@code null}. Không kiểm độ dài ở đây. */
    public static String cleanReason(String raw) {
        if (raw == null) {
            return null;
        }
        String reason = raw.trim();
        return reason.isEmpty() ? null : reason;
    }

    /** Số chỗ còn mời được: sĩ số tối đa trừ thành viên và lời mời đang chờ. */
    public static int countFreeSeats(int maxSize, int members, int pendingInvitations) {
        return Math.max(0, maxSize - members - pendingInvitations);
    }

    /** Sinh viên hiệu lực: ghi danh {@code ACTIVE} và chưa có nhóm chính thức (BR-U12-74). */
    public static boolean isEligible(UUID accountId, Set<UUID> activeStudents, Set<UUID> groupedStudents) {
        return activeStudents.contains(accountId) && !groupedStudents.contains(accountId);
    }

    /** Thành viên hiệu lực của đề xuất trong danh sách dòng thành viên. */
    public static long countEffectiveMembers(Collection<ProposalMember> members, Set<UUID> activeStudents,
                                             Set<UUID> groupedStudents) {
        return members.stream().filter(ProposalMember::isActiveMember)
                .filter(m -> isEligible(m.accountId(), activeStudents, groupedStudents)).count();
    }
}
