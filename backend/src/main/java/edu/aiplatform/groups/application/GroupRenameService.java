package edu.aiplatform.groups.application;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.groups.domain.ClassGroup;
import edu.aiplatform.groups.domain.GroupRepository;
import edu.aiplatform.groups.domain.GroupSetRules;
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
    private final IAudit audit;
    private final Clock clock;

    public GroupRenameService(GroupRepository groups, GroupAccess access, IAudit audit, Clock clock) {
        this.groups = groups;
        this.access = access;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public Renamed rename(Actor actor, UUID groupId, String name) {
        ClassGroup group = groups.findGroup(groupId).orElseThrow(GroupAccess::notFound);
        ClassRef ref = access.requireStudent(actor, group.classId());
        if (!group.isMember(actor.accountId())) {
            throw GroupAccess.notFound();
        }
        if (!actor.accountId().equals(group.leaderId())) {
            throw ApiException.forbidden("GROUP_RENAME_LEADER_ONLY");
        }
        GroupAccess.requireOpen(ref);
        String next = name == null ? "" : name.trim();
        if (next.isEmpty() || next.length() > GroupSetRules.MAX_NAME) {
            throw ApiException.badRequest("GROUP_NAME_INVALID", "Tên nhóm cần 1–100 ký tự");
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
        audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(), "GROUP_RENAMED", "CLASS_GROUP",
                group.id().toString(), AuditResult.SUCCESS, null, details, now));
        return new Renamed(group.id(), next);
    }

    private static ApiException duplicate(String name) {
        return ApiException.conflict("GROUP_NAME_DUPLICATE", "Tên nhóm bị trùng: " + name);
    }

    public record Renamed(UUID groupId, String name) {
    }
}
