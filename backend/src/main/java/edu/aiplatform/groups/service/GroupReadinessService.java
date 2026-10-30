package edu.aiplatform.groups.service;

import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.assignments.contract.IGroupAssignmentSupport;
import edu.aiplatform.assignments.contract.ReadinessResult;
import edu.aiplatform.groups.model.ClassGroup;
import edu.aiplatform.groups.repository.GroupRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kiểm lớp sẵn sàng cho bài nhóm khi U07 duyệt và phát hành (F6, BR-U12-22). Chỉ tính thành viên hiệu lực
 * (BR-U12-07): lỗi chặn {@code CLASS_WITHOUT_GROUP}, {@code GROUP_WITHOUT_LEADER}, {@code GROUP_WITHOUT_MEMBER};
 * cảnh báo {@code STUDENT_WITHOUT_GROUP}.
 */
@Service
public class GroupReadinessService implements IGroupAssignmentSupport {

    public static final String CLASS_WITHOUT_GROUP = "CLASS_WITHOUT_GROUP";
    public static final String GROUP_WITHOUT_LEADER = "GROUP_WITHOUT_LEADER";
    public static final String GROUP_WITHOUT_MEMBER = "GROUP_WITHOUT_MEMBER";
    public static final String STUDENT_WITHOUT_GROUP = "STUDENT_WITHOUT_GROUP";

    private final GroupRepository groups;
    private final IClassAccess classes;

    public GroupReadinessService(GroupRepository groups, IClassAccess classes) {
        this.groups = groups;
        this.classes = classes;
    }

    @Override
    @Transactional(readOnly = true)
    public ReadinessResult checkReadiness(UUID classId) {
        Set<UUID> active = new HashSet<>(classes.listActiveStudents(classId));
        List<ClassGroup> current = groups.listGroups(classId);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (current.isEmpty()) {
            errors.add(CLASS_WITHOUT_GROUP);
        }
        Set<UUID> grouped = new HashSet<>();
        boolean withoutLeader = false;
        boolean withoutMember = false;
        for (ClassGroup group : current) {
            List<UUID> members = group.memberIds().stream().filter(active::contains).toList();
            grouped.addAll(members);
            withoutMember |= members.isEmpty();
            withoutLeader |= group.leaderId() == null || !members.contains(group.leaderId());
        }
        if (withoutLeader) {
            errors.add(GROUP_WITHOUT_LEADER);
        }
        if (withoutMember) {
            errors.add(GROUP_WITHOUT_MEMBER);
        }
        if (!grouped.containsAll(active)) {
            warnings.add(STUDENT_WITHOUT_GROUP);
        }
        return new ReadinessResult(errors, warnings);
    }
}
