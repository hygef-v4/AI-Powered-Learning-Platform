package edu.aiplatform.groups.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Kiểm cả bộ nhóm trước khi lưu (BR-U12-02, 04): tên 1–100 ký tự, không trùng; có ít nhất một thành viên; đúng một
 * trưởng nhóm là thành viên; thành viên là sinh viên đang học; mỗi người một nhóm. Trả mọi lỗi, không dừng ở lỗi đầu.
 */
public final class GroupSetRules {

    public static final int MAX_NAME = 100;

    private GroupSetRules() {
    }

    /** Một nhóm trong bản sửa; {@code key} là id nhóm cũ hoặc khóa tạm của nhóm mới để báo lỗi đúng nhóm. */
    public record Draft(String key, UUID id, String name, UUID leaderId, List<UUID> memberIds) {
    }

    public record Issue(String groupKey, String code, String message) {
    }

    public static List<Issue> validate(List<Draft> groups, Set<UUID> activeStudents) {
        List<Issue> issues = new ArrayList<>();
        Map<String, String> names = new HashMap<>();
        Map<UUID, String> owner = new HashMap<>();
        for (Draft group : groups) {
            String name = group.name() == null ? "" : group.name().trim();
            if (name.isEmpty() || name.length() > MAX_NAME) {
                issues.add(new Issue(group.key(), "GROUP_NAME_INVALID", "Tên nhóm cần 1–100 ký tự"));
            } else if (names.putIfAbsent(name.toLowerCase(), group.key()) != null) {
                issues.add(new Issue(group.key(), "GROUP_NAME_DUPLICATE", "Tên nhóm bị trùng: " + name));
            }
            Set<UUID> members = new HashSet<>(group.memberIds() == null ? List.of() : group.memberIds());
            if (members.isEmpty()) {
                issues.add(new Issue(group.key(), "GROUP_WITHOUT_MEMBER", "Nhóm chưa có thành viên"));
            }
            if (group.leaderId() == null || !members.contains(group.leaderId())) {
                issues.add(new Issue(group.key(), "GROUP_WITHOUT_LEADER", "Nhóm cần một trưởng nhóm là thành viên"));
            }
            for (UUID member : members) {
                if (!activeStudents.contains(member)) {
                    issues.add(new Issue(group.key(), "MEMBER_NOT_ACTIVE", "Có người không còn là sinh viên đang học"));
                }
                String previous = owner.putIfAbsent(member, group.key());
                if (previous != null && !previous.equals(group.key())) {
                    issues.add(new Issue(group.key(), "MEMBER_IN_TWO_GROUPS", "Một sinh viên chỉ ở một nhóm"));
                }
            }
        }
        return issues;
    }
}
