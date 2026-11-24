package edu.aiplatform.groups.service;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.groups.model.ClassGroup;
import edu.aiplatform.groups.repository.GroupRepository;
import edu.aiplatform.groups.model.GroupSetRules;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Trưởng nhóm tự đổi tên nhóm của mình, không cần giảng viên duyệt (yêu cầu 2026-10-29). Tên theo cùng luật với bản
 * sửa nhóm của giảng viên (1–100 ký tự, không trùng nhóm khác trong lớp); đổi tên tăng version bộ nhóm để bản sửa
 * giảng viên đang mở biết dữ liệu đã đổi.
 */
@Service
public class GroupRenameService {

    private final GroupRepository groups;
    private final GroupAccess access;
    private final Clock clock;

    public GroupRenameService(GroupRepository groups, GroupAccess access, Clock clock) {
        this.groups = groups;
        this.access = access;
        this.clock = clock;
    }

    @Transactional
    public Renamed rename(Actor actor, UUID groupId, String name) {
        ClassGroup group = groups.findGroup(groupId).orElseThrow(GroupAccess::createNotFoundError);
        ClassRef ref = access.requireStudent(actor, group.classId());
        if (!group.isMember(actor.accountId())) {
            throw GroupAccess.createNotFoundError();
        }
        if (!actor.accountId().equals(group.leaderId())) {
            throw ApiException.createForbidden("GROUP_RENAME_LEADER_ONLY");
        }
        GroupAccess.requireOpen(ref);
        String next = name == null ? "" : name.trim();
        if (next.isEmpty() || next.length() > GroupSetRules.MAX_NAME) {
            throw ApiException.createBadRequest("GROUP_NAME_INVALID", "Tên nhóm cần 1–100 ký tự");
        }
        if (next.equals(group.name())) {
            return new Renamed(group.id(), next);
        }
        boolean taken = groups.listGroups(group.classId()).stream()
                .anyMatch(other -> !other.id().equals(group.id()) && other.name().equalsIgnoreCase(next));
        if (taken) {
            throw duplicate(next);
        }
        Instant now = clock.instant();
        try {
            groups.updateGroup(group.id(), next, group.leaderId(), now);
        } catch (DuplicateKeyException e) {
            throw duplicate(next);
        }
        groups.bumpSetVersion(group.classId(), groups.setVersion(group.classId()), now);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("classId", group.classId().toString());
        details.put("before", group.name());
        details.put("after", next);
        return new Renamed(group.id(), next);
    }

    private static ApiException duplicate(String name) {
        return ApiException.createConflict("GROUP_NAME_DUPLICATE", "Tên nhóm bị trùng: " + name);
    }

    public record Renamed(UUID groupId, String name) {
    }
}
